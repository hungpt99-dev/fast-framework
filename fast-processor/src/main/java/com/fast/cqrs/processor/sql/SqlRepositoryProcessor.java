package com.fast.cqrs.processor.sql;

import com.fast.cqrs.processor.util.ProcessorLogger;
import com.fast.cqrs.sql.annotation.Execute;
import com.fast.cqrs.sql.annotation.Param;
import com.fast.cqrs.sql.annotation.Select;
import com.fast.cqrs.sql.annotation.SqlRepository;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.*;

/**
 * Annotation processor for {@link SqlRepository} interfaces.
 * <p>
 * Generates concrete repository implementations at compile-time with:
 * <ul>
 *   <li>Direct SQL execution (no reflection)</li>
 *   <li>Optional caching based on {@code @Select(cache = "...")}</li>
 *   <li>Optional metrics based on {@code @Select(metrics = true)}</li>
 *   <li>Optional timeout based on {@code @Select(timeout = "...")}</li>
 * </ul>
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("com.fast.cqrs.sql.annotation.SqlRepository")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class SqlRepositoryProcessor extends AbstractProcessor {

    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;
    private ProcessorLogger logger;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
        this.filer = processingEnv.getFiler();
        this.logger = new ProcessorLogger(processingEnv.getMessager());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(SqlRepository.class)) {
            if (element.getKind() != ElementKind.INTERFACE) {
                logger.error("@SqlRepository can only be applied to interfaces", element);
                continue;
            }

            TypeElement repositoryInterface = (TypeElement) element;
            try {
                generateRepositoryImplementation(repositoryInterface);
                logger.note("Generated implementation for " + repositoryInterface.getSimpleName());
            } catch (IOException e) {
                logger.error("Failed to generate implementation: " + e.getMessage(), element);
            }
        }
        return true;
    }

    private void generateRepositoryImplementation(TypeElement repositoryInterface) throws IOException {
        String packageName = elementUtils.getPackageOf(repositoryInterface).getQualifiedName().toString();
        String interfaceName = repositoryInterface.getSimpleName().toString();
        String implClassName = interfaceName + "_FastImpl";

        // Check for FastRepository extension
        TypeMirror entityType = null;
        TypeMirror idType = null;
        boolean hasFastRepository = false;

        for (TypeMirror iface : repositoryInterface.getInterfaces()) {
            DeclaredType dt = (DeclaredType) iface;
            String rawName = dt.asElement().toString();
            if (rawName.equals("com.fast.cqrs.sql.repository.FastRepository")) {
                List<? extends TypeMirror> args = dt.getTypeArguments();
                if (args.size() == 2) {
                    entityType = args.get(0);
                    idType = args.get(1);
                    hasFastRepository = true;
                }
            }
        }

        // Check if any method needs caching
        boolean needsCaching = needsCaching(repositoryInterface);

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(implClassName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(TypeName.get(repositoryInterface.asType()))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("javax.annotation.processing", "Generated"))
                        .addMember("value", "$S", SqlRepositoryProcessor.class.getCanonicalName())
                        .addMember("date", "$S", java.time.Instant.now().toString())
                        .build())
                .addAnnotation(ClassName.get("org.springframework.stereotype", "Repository"));

        // Fields
        ClassName jdbcTemplateClass = ClassName.get("org.springframework.jdbc.core.namedparam", "NamedParameterJdbcTemplate");
        classBuilder.addField(FieldSpec.builder(jdbcTemplateClass, "jdbcTemplate", Modifier.PRIVATE, Modifier.FINAL).build());
        
        ClassName resultMapperClass = ClassName.get("com.fast.cqrs.sql.mapper", "ResultMapper");
        classBuilder.addField(FieldSpec.builder(resultMapperClass, "resultMapper", Modifier.PRIVATE, Modifier.FINAL).build());

        ClassName crudExecutorClass = ClassName.get("com.fast.cqrs.sql.repository", "CrudExecutor");
        if (hasFastRepository) {
            classBuilder.addField(FieldSpec.builder(crudExecutorClass, "crudExecutor", Modifier.PRIVATE, Modifier.FINAL).build());
        }

        // Add CacheManager if needed
        if (needsCaching) {
            ClassName cacheManagerClass = ClassName.get("org.springframework.cache", "CacheManager");
            classBuilder.addField(FieldSpec.builder(cacheManagerClass, "cacheManager", Modifier.PRIVATE, Modifier.FINAL).build());
        }

        // Constructor
        MethodSpec.Builder ctorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(jdbcTemplateClass, "jdbcTemplate")
                .addStatement("this.jdbcTemplate = jdbcTemplate")
                .addStatement("this.resultMapper = new $T()", resultMapperClass);

        if (hasFastRepository && entityType != null) {
            ctorBuilder.addStatement("this.crudExecutor = new $T(jdbcTemplate, $T.class)", 
                    crudExecutorClass, TypeName.get(entityType));
        }

        if (needsCaching) {
            ClassName cacheManagerClass = ClassName.get("org.springframework.cache", "CacheManager");
            ctorBuilder.addParameter(ParameterSpec.builder(cacheManagerClass, "cacheManager")
                    .addAnnotation(ClassName.get("org.springframework.lang", "Nullable"))
                    .build());
            ctorBuilder.addStatement("this.cacheManager = cacheManager");
        }

        classBuilder.addMethod(ctorBuilder.build());

        // Implement overridden methods (@Select, @Execute)
        for (Element enclosed : repositoryInterface.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) enclosed;
                Select select = method.getAnnotation(Select.class);
                Execute execute = method.getAnnotation(Execute.class);

                if (select != null) {
                    classBuilder.addMethod(generateSelectMethod(method, select, interfaceName));
                } else if (execute != null) {
                    classBuilder.addMethod(generateExecuteMethod(method, execute));
                }
            }
        }

        // Implement CRUD methods if FastRepository
        if (hasFastRepository && entityType != null && idType != null) {
            generateCrudMethods(classBuilder, entityType, idType);
        }

        JavaFile.builder(packageName, classBuilder.build())
                .indent("    ")
                .build()
                .writeTo(filer);
    }

    private boolean needsCaching(TypeElement repositoryInterface) {
        for (Element enclosed : repositoryInterface.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD) {
                Select select = enclosed.getAnnotation(Select.class);
                if (select != null && !select.cache().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void generateCrudMethods(TypeSpec.Builder builder, TypeMirror entityType, TypeMirror idType) {
        TypeName entityTypeName = TypeName.get(entityType);
        TypeName idTypeName = TypeName.get(idType);
        ClassName optionalClass = ClassName.get(Optional.class);
        ClassName listClass = ClassName.get(List.class);

        // findById
        builder.addMethod(MethodSpec.methodBuilder("findById")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(optionalClass, entityTypeName))
                .addParameter(idTypeName, "id")
                .addStatement("return crudExecutor.findById(id)")
                .build());

        // findAll
        builder.addMethod(MethodSpec.methodBuilder("findAll")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(listClass, entityTypeName))
                .addStatement("return crudExecutor.findAll()")
                .build());

        // save
        builder.addMethod(MethodSpec.methodBuilder("save")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(entityTypeName)
                .addParameter(entityTypeName, "entity")
                .addStatement("return ($T) crudExecutor.save(entity)", entityTypeName)
                .build());

        // saveAll
        builder.addMethod(MethodSpec.methodBuilder("saveAll")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(ParameterizedTypeName.get(listClass, entityTypeName), "entities")
                .addStatement("crudExecutor.saveAll(entities)")
                .build());

        // updateAll
        builder.addMethod(MethodSpec.methodBuilder("updateAll")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(ParameterizedTypeName.get(listClass, entityTypeName), "entities")
                .addStatement("crudExecutor.updateAll(entities)")
                .build());

        // deleteById
        builder.addMethod(MethodSpec.methodBuilder("deleteById")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(idTypeName, "id")
                .addStatement("crudExecutor.deleteById(id)")
                .build());

        // deleteAllById
        builder.addMethod(MethodSpec.methodBuilder("deleteAllById")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(ParameterizedTypeName.get(listClass, idTypeName), "ids")
                .addStatement("crudExecutor.deleteAllById(ids)")
                .build());

        // existsById
        builder.addMethod(MethodSpec.methodBuilder("existsById")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addParameter(idTypeName, "id")
                .addStatement("return crudExecutor.existsById(id)")
                .build());

        // count
        builder.addMethod(MethodSpec.methodBuilder("count")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(long.class)
                .addStatement("return crudExecutor.count()")
                .build());

        // deleteAll
        builder.addMethod(MethodSpec.methodBuilder("deleteAll")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addStatement("crudExecutor.deleteAll()")
                .build());
    }

    private MethodSpec generateSelectMethod(ExecutableElement method, Select select, String repositoryName) {
        String sql = select.value();
        String cache = select.cache();
        boolean hasCache = !cache.isEmpty();
        boolean hasMetrics = select.metrics();
        String metricsName = select.metricsName().isEmpty() 
                ? repositoryName + "." + method.getSimpleName() 
                : select.metricsName();
        
        TypeMirror returnType = method.getReturnType();

        MethodSpec.Builder builder = MethodSpec.overriding(method);

        // Build cache key if caching enabled
        if (hasCache) {
            builder.addCode("// Cache key generation\n");
            builder.addStatement("String cacheKey = $S", repositoryName + "." + method.getSimpleName());
            for (VariableElement param : method.getParameters()) {
                builder.addStatement("cacheKey += \":\" + $N", param.getSimpleName());
            }
            builder.addCode("\n");
            
            // Check cache first
            builder.addStatement("$T cache = cacheManager != null ? cacheManager.getCache($S) : null",
                    ClassName.get("org.springframework.cache", "Cache"),
                    repositoryName);
            builder.beginControlFlow("if (cache != null)");
            builder.addStatement("$T cached = cache.get(cacheKey)",
                    ClassName.get("org.springframework.cache", "Cache", "ValueWrapper"));
            builder.beginControlFlow("if (cached != null)");
            builder.addStatement("return ($T) cached.get()", TypeName.get(returnType));
            builder.endControlFlow();
            builder.endControlFlow();
            builder.addCode("\n");
        }

        // Build params
        builder.addStatement("$T<String, Object> params = new $T<>()", Map.class, HashMap.class);
        
        for (VariableElement param : method.getParameters()) {
            Param paramAnn = param.getAnnotation(Param.class);
            if (paramAnn != null) {
                builder.addStatement("params.put($S, $N)", paramAnn.value(), param.getSimpleName());
            }
        }

        // Execute query and get result
        String resultVar = "result";
        if (isOptionalType(returnType)) {
            TypeMirror elementType = extractTypeArgument(returnType, 0);
            builder.addStatement("$T<?> results = jdbcTemplate.query($S, params, resultMapper.getRowMapper($T.class))",
                    List.class, sql, TypeName.get(elementType));
            builder.addStatement("$T $N = results.isEmpty() ? $T.empty() : $T.of(results.get(0))",
                    TypeName.get(returnType), resultVar, Optional.class, Optional.class);
        } else if (isCollectionType(returnType)) {
            TypeMirror elementType = extractTypeArgument(returnType, 0);
            builder.addStatement("$T $N = jdbcTemplate.query($S, params, resultMapper.getRowMapper($T.class))",
                    TypeName.get(returnType), resultVar, sql, TypeName.get(elementType));
        } else {
            TypeName returnTypeName = TypeName.get(returnType);
            builder.addStatement("$T<?> results = jdbcTemplate.query($S, params, resultMapper.getRowMapper($T.class))",
                    List.class, sql, returnTypeName);
            builder.beginControlFlow("if (results.isEmpty())");
            if (returnType.getKind().isPrimitive()) {
                if (returnType.getKind() == TypeKind.INT) builder.addStatement("return 0");
                else if (returnType.getKind() == TypeKind.LONG) builder.addStatement("return 0L");
                else if (returnType.getKind() == TypeKind.BOOLEAN) builder.addStatement("return false");
                else builder.addStatement("return 0");
            } else {
                builder.addStatement("return null");
            }
            builder.endControlFlow();
            builder.beginControlFlow("if (results.size() > 1)");
            builder.addStatement("throw new $T($S + results.size())",
                    ClassName.get("com.fast.cqrs.sql.executor", "SqlExecutionException"),
                    "Expected single result but got ");
            builder.endControlFlow();
            builder.addStatement("$T $N = ($T) results.get(0)", returnTypeName, resultVar, returnTypeName);
        }

        // Store in cache if enabled
        if (hasCache) {
            builder.addCode("\n// Store in cache\n");
            builder.beginControlFlow("if (cache != null)");
            builder.addStatement("cache.put(cacheKey, $N)", resultVar);
            builder.endControlFlow();
        }

        builder.addStatement("return $N", resultVar);

        return builder.build();
    }

    private MethodSpec generateExecuteMethod(ExecutableElement method, Execute execute) {
        String sql = execute.value();
        TypeMirror returnType = method.getReturnType();

        MethodSpec.Builder builder = MethodSpec.overriding(method);

        builder.addStatement("$T<String, Object> params = new $T<>()", Map.class, HashMap.class);
        
        for (VariableElement param : method.getParameters()) {
            Param paramAnn = param.getAnnotation(Param.class);
            if (paramAnn != null) {
                builder.addStatement("params.put($S, $N)", paramAnn.value(), param.getSimpleName());
            }
        }

        if (returnType.getKind() == TypeKind.VOID) {
            builder.addStatement("jdbcTemplate.update($S, params)", sql);
        } else if (returnType.getKind() == TypeKind.INT || isType(returnType, "java.lang.Integer")) {
            builder.addStatement("return jdbcTemplate.update($S, params)", sql);
        } else if (returnType.getKind() == TypeKind.LONG || isType(returnType, "java.lang.Long")) {
            builder.addStatement("return (long) jdbcTemplate.update($S, params)", sql);
        } else {
            builder.addStatement("jdbcTemplate.update($S, params)", sql);
            builder.addStatement("return null");
        }
        return builder.build();
    }

    private boolean isOptionalType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) return false;
        return ((DeclaredType) type).asElement().toString().equals("java.util.Optional");
    }

    private boolean isCollectionType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) return false;
        TypeElement element = (TypeElement) ((DeclaredType) type).asElement();
        TypeElement collectionElement = elementUtils.getTypeElement("java.util.Collection");
        return typeUtils.isAssignable(type, typeUtils.erasure(collectionElement.asType()));
    }

    private boolean isType(TypeMirror type, String typeName) {
        return type.toString().equals(typeName);
    }

    private TypeMirror extractTypeArgument(TypeMirror type, int index) {
        if (type.getKind() != TypeKind.DECLARED) return null;
        List<? extends TypeMirror> args = ((DeclaredType) type).getTypeArguments();
        if (args.size() > index) return args.get(index);
        return null;
    }
}
