package com.fast.cqrs.processor.sql;

import com.fast.cqrs.processor.util.ProcessorLogger;
import com.fast.cqrs.sql.annotation.Execute;
import com.fast.cqrs.sql.annotation.Param;
import com.fast.cqrs.sql.annotation.Select;
import com.fast.cqrs.sql.annotation.SqlRepository;
import com.fast.cqrs.sql.repository.Column;
import com.fast.cqrs.sql.repository.Id;
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
 *   <li>Compile-time generated RowMappers (no reflection)</li>
 *   <li>Direct SQL execution</li>
 *   <li>Automatic transaction management</li>
 *   <li>Optional caching</li>
 *   <li>Optional metrics</li>
 * </ul>
 * <p>
 * <b>GraalVM Compatible:</b> All entity mappings are generated at compile-time,
 * eliminating the need for runtime reflection.
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
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.stereotype", "Repository"))
                        .addMember("value", "$S", Character.toLowerCase(interfaceName.charAt(0)) + interfaceName.substring(1))
                        .build())
                .addJavadoc("Generated implementation for {@link $T}.\n", repositoryInterface)
                .addJavadoc("<p>GraalVM compatible: All RowMappers are generated at compile-time.\n");

        // Fields
        ClassName jdbcTemplateClass = ClassName.get("org.springframework.jdbc.core.namedparam", "NamedParameterJdbcTemplate");
        classBuilder.addField(FieldSpec.builder(jdbcTemplateClass, "jdbcTemplate", Modifier.PRIVATE, Modifier.FINAL).build());

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
                .addStatement("this.jdbcTemplate = jdbcTemplate");

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
                .addFileComment("Auto-generated by Fast CQRS Processor. DO NOT EDIT.\n")
                .addFileComment("GraalVM native-image compatible (zero reflection RowMappers).")
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
        ClassName transactionalClass = ClassName.get("org.springframework.transaction.annotation", "Transactional");

        // findById - read-only transaction
        builder.addMethod(MethodSpec.methodBuilder("findById")
                .addAnnotation(Override.class)
                .addAnnotation(AnnotationSpec.builder(transactionalClass)
                        .addMember("readOnly", "true")
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(optionalClass, entityTypeName))
                .addParameter(idTypeName, "id")
                .addStatement("return crudExecutor.findById(id)")
                .build());

        // findAll - read-only transaction
        builder.addMethod(MethodSpec.methodBuilder("findAll")
                .addAnnotation(Override.class)
                .addAnnotation(AnnotationSpec.builder(transactionalClass)
                        .addMember("readOnly", "true")
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(listClass, entityTypeName))
                .addStatement("return crudExecutor.findAll()")
                .build());

        // save - write transaction
        builder.addMethod(MethodSpec.methodBuilder("save")
                .addAnnotation(Override.class)
                .addAnnotation(transactionalClass)
                .addModifiers(Modifier.PUBLIC)
                .returns(entityTypeName)
                .addParameter(entityTypeName, "entity")
                .addStatement("return ($T) crudExecutor.save(entity)", entityTypeName)
                .build());

        // saveAll - write transaction
        builder.addMethod(MethodSpec.methodBuilder("saveAll")
                .addAnnotation(Override.class)
                .addAnnotation(transactionalClass)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(ParameterizedTypeName.get(listClass, entityTypeName), "entities")
                .addStatement("crudExecutor.saveAll(entities)")
                .build());

        // updateAll - write transaction
        builder.addMethod(MethodSpec.methodBuilder("updateAll")
                .addAnnotation(Override.class)
                .addAnnotation(transactionalClass)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(ParameterizedTypeName.get(listClass, entityTypeName), "entities")
                .addStatement("crudExecutor.updateAll(entities)")
                .build());

        // deleteById - write transaction
        builder.addMethod(MethodSpec.methodBuilder("deleteById")
                .addAnnotation(Override.class)
                .addAnnotation(transactionalClass)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(idTypeName, "id")
                .addStatement("crudExecutor.deleteById(id)")
                .build());

        // deleteAllById - write transaction
        builder.addMethod(MethodSpec.methodBuilder("deleteAllById")
                .addAnnotation(Override.class)
                .addAnnotation(transactionalClass)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(ParameterizedTypeName.get(listClass, idTypeName), "ids")
                .addStatement("crudExecutor.deleteAllById(ids)")
                .build());

        // existsById - read-only transaction
        builder.addMethod(MethodSpec.methodBuilder("existsById")
                .addAnnotation(Override.class)
                .addAnnotation(AnnotationSpec.builder(transactionalClass)
                        .addMember("readOnly", "true")
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addParameter(idTypeName, "id")
                .addStatement("return crudExecutor.existsById(id)")
                .build());

        // count - read-only transaction
        builder.addMethod(MethodSpec.methodBuilder("count")
                .addAnnotation(Override.class)
                .addAnnotation(AnnotationSpec.builder(transactionalClass)
                        .addMember("readOnly", "true")
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .returns(long.class)
                .addStatement("return crudExecutor.count()")
                .build());

        // deleteAll - write transaction
        builder.addMethod(MethodSpec.methodBuilder("deleteAll")
                .addAnnotation(Override.class)
                .addAnnotation(transactionalClass)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addStatement("crudExecutor.deleteAll()")
                .build());
    }

    private MethodSpec generateSelectMethod(ExecutableElement method, Select select, String repositoryName) {
        String sql = select.value();
        String cache = select.cache();
        boolean hasCache = !cache.isEmpty();
        
        TypeMirror returnType = method.getReturnType();
        ClassName transactionalClass = ClassName.get("org.springframework.transaction.annotation", "Transactional");

        MethodSpec.Builder builder = MethodSpec.overriding(method)
                .addAnnotation(AnnotationSpec.builder(transactionalClass)
                        .addMember("readOnly", "true")
                        .build());

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

        // Generate inline RowMapper for the return type
        String resultVar = "result";
        TypeMirror elementType = extractElementType(returnType);
        
        if (isOptionalType(returnType)) {
            // Generate inline RowMapper lambda
            builder.addCode("\n// Compile-time generated RowMapper (zero reflection)\n");
            builder.addStatement("$T<$T> rowMapper = $L",
                    ClassName.get("org.springframework.jdbc.core", "RowMapper"),
                    TypeName.get(elementType),
                    generateRowMapperLambda(elementType));
            builder.addStatement("$T<?> results = jdbcTemplate.query($S, params, rowMapper)",
                    List.class, sql);
            builder.addStatement("$T $N = results.isEmpty() ? $T.empty() : $T.of(results.get(0))",
                    TypeName.get(returnType), resultVar, Optional.class, Optional.class);
        } else if (isCollectionType(returnType)) {
            builder.addCode("\n// Compile-time generated RowMapper (zero reflection)\n");
            builder.addStatement("$T<$T> rowMapper = $L",
                    ClassName.get("org.springframework.jdbc.core", "RowMapper"),
                    TypeName.get(elementType),
                    generateRowMapperLambda(elementType));
            builder.addStatement("$T $N = jdbcTemplate.query($S, params, rowMapper)",
                    TypeName.get(returnType), resultVar, sql);
        } else {
            // Single result
            builder.addCode("\n// Compile-time generated RowMapper (zero reflection)\n");
            builder.addStatement("$T<$T> rowMapper = $L",
                    ClassName.get("org.springframework.jdbc.core", "RowMapper"),
                    TypeName.get(returnType),
                    generateRowMapperLambda(returnType));
            builder.addStatement("$T<?> results = jdbcTemplate.query($S, params, rowMapper)",
                    List.class, sql);
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
            builder.addStatement("$T $N = ($T) results.get(0)", TypeName.get(returnType), resultVar, TypeName.get(returnType));
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

    /**
     * Generates a compile-time RowMapper lambda for the given entity type.
     * This eliminates the need for runtime reflection.
     */
    private CodeBlock generateRowMapperLambda(TypeMirror entityType) {
        // Handle primitive wrappers and common types
        String typeName = entityType.toString();
        
        if (typeName.equals("java.lang.String")) {
            return CodeBlock.of("(rs, rowNum) -> rs.getString(1)");
        }
        if (typeName.equals("java.lang.Integer") || typeName.equals("int")) {
            return CodeBlock.of("(rs, rowNum) -> rs.getInt(1)");
        }
        if (typeName.equals("java.lang.Long") || typeName.equals("long")) {
            return CodeBlock.of("(rs, rowNum) -> rs.getLong(1)");
        }
        if (typeName.equals("java.lang.Boolean") || typeName.equals("boolean")) {
            return CodeBlock.of("(rs, rowNum) -> rs.getBoolean(1)");
        }
        if (typeName.equals("java.lang.Double") || typeName.equals("double")) {
            return CodeBlock.of("(rs, rowNum) -> rs.getDouble(1)");
        }
        if (typeName.equals("java.math.BigDecimal")) {
            return CodeBlock.of("(rs, rowNum) -> rs.getBigDecimal(1)");
        }
        if (typeName.equals("java.time.LocalDateTime")) {
            return CodeBlock.of("(rs, rowNum) -> rs.getTimestamp(1) != null ? rs.getTimestamp(1).toLocalDateTime() : null");
        }
        if (typeName.equals("java.time.LocalDate")) {
            return CodeBlock.of("(rs, rowNum) -> rs.getDate(1) != null ? rs.getDate(1).toLocalDate() : null");
        }
        if (typeName.equals("byte[]")) {
            return CodeBlock.of("(rs, rowNum) -> rs.getBytes(1)");
        }

        // For complex types, try to analyze the entity's fields
        if (entityType.getKind() == TypeKind.DECLARED) {
            TypeElement entityElement = (TypeElement) ((DeclaredType) entityType).asElement();
            return generateEntityRowMapper(entityElement, TypeName.get(entityType));
        }

        // Fallback to BeanPropertyRowMapper (requires reflection)
        logger.warning("Using BeanPropertyRowMapper for " + typeName + " - consider adding @Column annotations for GraalVM compatibility", null);
        return CodeBlock.of("$T.newInstance($T.class)", 
                ClassName.get("org.springframework.jdbc.core", "BeanPropertyRowMapper"),
                TypeName.get(entityType));
    }

    /**
     * Generates a compile-time RowMapper for an entity with known fields.
     * Uses plain string building to avoid JavaPoet CodeBlock nesting issues.
     */
    private CodeBlock generateEntityRowMapper(TypeElement entityElement, TypeName entityType) {
        List<FieldMapping> mappings = extractFieldMappings(entityElement);
        
        if (mappings.isEmpty()) {
            // No fields found, fall back to BeanPropertyRowMapper
            return CodeBlock.of("$T.newInstance($T.class)",
                    ClassName.get("org.springframework.jdbc.core", "BeanPropertyRowMapper"),
                    entityType);
        }

        // Build the lambda as a plain string to avoid CodeBlock nesting issues
        StringBuilder sb = new StringBuilder();
        sb.append("(rs, rowNum) -> {\n");
        sb.append("    ").append(entityType.toString()).append(" entity = new ").append(entityType.toString()).append("();\n");
        
        for (FieldMapping mapping : mappings) {
            sb.append("    entity.").append(mapping.setterName).append("(");
            sb.append(generateResultSetGetterString(mapping));
            sb.append(");\n");
        }
        
        sb.append("    return entity;\n");
        sb.append("}");
        
        return CodeBlock.of(sb.toString());
    }

    /**
     * Extracts field mappings from an entity class.
     */
    private List<FieldMapping> extractFieldMappings(TypeElement entityElement) {
        List<FieldMapping> mappings = new ArrayList<>();

        for (Element enclosed : entityElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;
                
                // Skip static and transient fields
                if (field.getModifiers().contains(Modifier.STATIC) ||
                    field.getModifiers().contains(Modifier.TRANSIENT)) {
                    continue;
                }

                String fieldName = field.getSimpleName().toString();
                String columnName = fieldName;
                TypeMirror fieldType = field.asType();

                // Check for @Column annotation
                Column columnAnn = field.getAnnotation(Column.class);
                if (columnAnn != null && !columnAnn.value().isEmpty()) {
                    columnName = columnAnn.value();
                } else {
                    // Convert camelCase to snake_case
                    columnName = camelToSnake(fieldName);
                }

                // Check for @Id annotation (id fields use their field name as column)
                Id idAnn = field.getAnnotation(Id.class);
                if (idAnn != null) {
                    // @Id doesn't have a value, use the field name or @Column if present
                    // columnName is already set from @Column or camelCase conversion
                }

                String setterName = "set" + capitalize(fieldName);
                mappings.add(new FieldMapping(fieldName, columnName, setterName, fieldType));
            }
        }

        return mappings;
    }

    /**
     * Generates the appropriate ResultSet getter call as a plain string.
     * This avoids CodeBlock nesting issues in the entity RowMapper generation.
     */
    private String generateResultSetGetterString(FieldMapping mapping) {
        String typeName = mapping.fieldType.toString();
        String col = mapping.columnName;

        // Handle common types - return plain Java code strings
        if (typeName.equals("java.lang.String")) {
            return "rs.getString(\"" + col + "\")";
        }
        if (typeName.equals("java.lang.Integer") || typeName.equals("int")) {
            return "rs.getInt(\"" + col + "\")";
        }
        if (typeName.equals("java.lang.Long") || typeName.equals("long")) {
            return "rs.getLong(\"" + col + "\")";
        }
        if (typeName.equals("java.lang.Boolean") || typeName.equals("boolean")) {
            return "rs.getBoolean(\"" + col + "\")";
        }
        if (typeName.equals("java.lang.Double") || typeName.equals("double")) {
            return "rs.getDouble(\"" + col + "\")";
        }
        if (typeName.equals("java.lang.Float") || typeName.equals("float")) {
            return "rs.getFloat(\"" + col + "\")";
        }
        if (typeName.equals("java.math.BigDecimal")) {
            return "rs.getBigDecimal(\"" + col + "\")";
        }
        if (typeName.equals("java.time.LocalDateTime")) {
            return "rs.getTimestamp(\"" + col + "\") != null ? rs.getTimestamp(\"" + col + "\").toLocalDateTime() : null";
        }
        if (typeName.equals("java.time.LocalDate")) {
            return "rs.getDate(\"" + col + "\") != null ? rs.getDate(\"" + col + "\").toLocalDate() : null";
        }
        if (typeName.equals("java.time.Instant")) {
            return "rs.getTimestamp(\"" + col + "\") != null ? rs.getTimestamp(\"" + col + "\").toInstant() : null";
        }
        if (typeName.equals("java.sql.Timestamp")) {
            return "rs.getTimestamp(\"" + col + "\")";
        }
        if (typeName.equals("java.sql.Date")) {
            return "rs.getDate(\"" + col + "\")";
        }
        if (typeName.equals("byte[]")) {
            return "rs.getBytes(\"" + col + "\")";
        }
        if (typeName.equals("java.sql.Blob")) {
            return "rs.getBlob(\"" + col + "\")";
        }
        if (typeName.equals("java.sql.Clob")) {
            return "rs.getClob(\"" + col + "\")";
        }
        if (typeName.equals("java.util.UUID")) {
            return "rs.getString(\"" + col + "\") != null ? java.util.UUID.fromString(rs.getString(\"" + col + "\")) : null";
        }

        // Default to getObject for unknown types
        return "(" + typeName + ") rs.getObject(\"" + col + "\")";
    }

    private MethodSpec generateExecuteMethod(ExecutableElement method, Execute execute) {
        String sql = execute.value();
        TypeMirror returnType = method.getReturnType();
        ClassName transactionalClass = ClassName.get("org.springframework.transaction.annotation", "Transactional");

        MethodSpec.Builder builder = MethodSpec.overriding(method)
                .addAnnotation(transactionalClass);  // Write transaction

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

    private TypeMirror extractElementType(TypeMirror type) {
        if (isOptionalType(type) || isCollectionType(type)) {
            return extractTypeArgument(type, 0);
        }
        return type;
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

    private String camelToSnake(String camel) {
        StringBuilder snake = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) snake.append('_');
                snake.append(Character.toLowerCase(c));
            } else {
                snake.append(c);
            }
        }
        return snake.toString();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    private record FieldMapping(
        String fieldName,
        String columnName,
        String setterName,
        TypeMirror fieldType
    ) {}
}
