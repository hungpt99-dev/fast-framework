package com.fast.cqrs.processor.controller;

import com.fast.cqrs.cqrs.annotation.Command;
import com.fast.cqrs.cqrs.annotation.HttpController;
import com.fast.cqrs.cqrs.annotation.Query;
import com.fast.cqrs.processor.util.ProcessorLogger;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Annotation processor for {@link HttpController} interfaces.
 * <p>
 * Generates concrete controller implementation classes at compile-time,
 * eliminating runtime dynamic proxy overhead for HTTP request handling.
 * <p>
 * For each {@code @HttpController} interface, generates a class named
 * {@code <Interface>_FastImpl} that:
 * <ul>
 *   <li>Implements all interface methods</li>
 *   <li>Copies Spring MVC annotations (@GetMapping, @PostMapping, etc.)</li>
 *   <li>Routes @Query methods through QueryBus</li>
 *   <li>Routes @Command methods through CommandBus</li>
 * </ul>
 *
 * @see HttpController
 * @see Query
 * @see Command
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("com.fast.cqrs.cqrs.annotation.HttpController")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class HttpControllerProcessor extends AbstractProcessor {

    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;
    private ProcessorLogger logger;

    // Spring MVC annotations to copy from interface to implementation
    private static final Set<String> MVC_ANNOTATIONS = Set.of(
            "org.springframework.web.bind.annotation.RequestMapping",
            "org.springframework.web.bind.annotation.GetMapping",
            "org.springframework.web.bind.annotation.PostMapping",
            "org.springframework.web.bind.annotation.PutMapping",
            "org.springframework.web.bind.annotation.DeleteMapping",
            "org.springframework.web.bind.annotation.PatchMapping",
            "org.springframework.web.bind.annotation.PathVariable",
            "org.springframework.web.bind.annotation.RequestParam",
            "org.springframework.web.bind.annotation.RequestBody",
            "org.springframework.web.bind.annotation.RequestHeader",
            "org.springframework.web.bind.annotation.ModelAttribute"
    );

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
        for (Element element : roundEnv.getElementsAnnotatedWith(HttpController.class)) {
            if (element.getKind() != ElementKind.INTERFACE) {
                logger.error("@HttpController can only be applied to interfaces", element);
                continue;
            }

            TypeElement controllerInterface = (TypeElement) element;

            try {
                generateControllerImplementation(controllerInterface);
                logger.note("Generated implementation for " + controllerInterface.getSimpleName());
            } catch (IOException e) {
                logger.error("Failed to generate implementation: " + e.getMessage(), element);
            }
        }
        return true;
    }

    private void generateControllerImplementation(TypeElement controllerInterface) throws IOException {
        String packageName = elementUtils.getPackageOf(controllerInterface).getQualifiedName().toString();
        String interfaceName = controllerInterface.getSimpleName().toString();
        String implClassName = interfaceName + "_FastImpl";

        // Build the implementation class
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(implClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(TypeName.get(controllerInterface.asType()))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("javax.annotation.processing", "Generated"))
                        .addMember("value", "$S", HttpControllerProcessor.class.getCanonicalName())
                        .addMember("date", "$S", java.time.Instant.now().toString())
                        .build())
                .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "RestController"))
                .addJavadoc("Generated implementation for {@link $T}.\n", controllerInterface)
                .addJavadoc("<p>This class is generated at compile-time by the Fast CQRS Processor.\n");

        // Copy class-level Spring MVC annotations
        for (AnnotationMirror annotation : controllerInterface.getAnnotationMirrors()) {
            if (shouldCopyAnnotation(annotation)) {
                classBuilder.addAnnotation(AnnotationSpec.get(annotation));
            }
        }

        // Add QueryBus and CommandBus fields
        ClassName queryBusClass = ClassName.get("com.fast.cqrs.cqrs", "QueryBus");
        ClassName commandBusClass = ClassName.get("com.fast.cqrs.cqrs", "CommandBus");
        
        classBuilder.addField(FieldSpec.builder(queryBusClass, "queryBus", Modifier.PRIVATE, Modifier.FINAL).build());
        classBuilder.addField(FieldSpec.builder(commandBusClass, "commandBus", Modifier.PRIVATE, Modifier.FINAL).build());

        // Add constructor
        classBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(queryBusClass, "queryBus")
                .addParameter(commandBusClass, "commandBus")
                .addStatement("this.queryBus = queryBus")
                .addStatement("this.commandBus = commandBus")
                .build());

        // Generate method implementations
        for (Element enclosed : controllerInterface.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) enclosed;
                classBuilder.addMethod(generateMethodImplementation(method, controllerInterface));
            }
        }

        // Write the generated file
        JavaFile javaFile = JavaFile.builder(packageName, classBuilder.build())
                .addFileComment("Auto-generated by Fast CQRS Processor. DO NOT EDIT.")
                .indent("    ")
                .build();

        javaFile.writeTo(filer);
    }

    private MethodSpec generateMethodImplementation(ExecutableElement method, TypeElement controllerInterface) {
        Query queryAnn = method.getAnnotation(Query.class);
        Command commandAnn = method.getAnnotation(Command.class);

        MethodSpec.Builder builder = MethodSpec.overriding(method);

        // Copy method-level Spring MVC annotations
        for (AnnotationMirror annotation : method.getAnnotationMirrors()) {
            if (shouldCopyAnnotation(annotation)) {
                builder.addAnnotation(AnnotationSpec.get(annotation));
            }
        }

        if (queryAnn != null) {
            generateQueryDispatch(builder, method, queryAnn);
        } else if (commandAnn != null) {
            generateCommandDispatch(builder, method, commandAnn);
        } else {
            logger.warning("Method " + method.getSimpleName() + " has no @Query or @Command annotation", method);
            builder.addStatement("throw new $T($S)",
                    UnsupportedOperationException.class,
                    "Method must be annotated with @Query or @Command");
        }

        return builder.build();
    }

    private void generateQueryDispatch(MethodSpec.Builder builder, ExecutableElement method, Query queryAnn) {
        TypeMirror returnType = method.getReturnType();

        // Find the query parameter (usually the first non-primitive parameter or @ModelAttribute)
        VariableElement queryParam = findPayloadParameter(method);

        if (queryParam != null) {
            // Direct dispatch with query object
            builder.addStatement("return queryBus.dispatch($N)", queryParam.getSimpleName());
        } else if (method.getParameters().isEmpty()) {
            // No parameters - create simple query wrapper
            builder.addStatement("return queryBus.dispatch(new $T())",
                    ClassName.get("com.fast.cqrs.cqrs.CqrsDispatcher", "SimpleQuery"));
        } else {
            // Build query from parameters
            builder.addComment("TODO: Auto-construct query from parameters");
            builder.addStatement("throw new $T($S)",
                    UnsupportedOperationException.class,
                    "Query parameter inference - coming in Phase 2");
        }
    }

    private void generateCommandDispatch(MethodSpec.Builder builder, ExecutableElement method, Command commandAnn) {
        // Find the command parameter
        VariableElement commandParam = findPayloadParameter(method);

        if (commandParam != null) {
            builder.addStatement("commandBus.dispatch($N)", commandParam.getSimpleName());
        } else {
            builder.addComment("TODO: Auto-construct command from parameters");
            builder.addStatement("throw new $T($S)",
                    UnsupportedOperationException.class,
                    "Command parameter inference - coming in Phase 2");
        }

        // Void return
        TypeMirror returnType = method.getReturnType();
        if (returnType.getKind() != javax.lang.model.type.TypeKind.VOID) {
            builder.addStatement("return null");
        }
    }

    private VariableElement findPayloadParameter(ExecutableElement method) {
        // First, look for @RequestBody or @ModelAttribute annotated parameter
        for (VariableElement param : method.getParameters()) {
            for (AnnotationMirror ann : param.getAnnotationMirrors()) {
                String annName = ann.getAnnotationType().toString();
                if (annName.endsWith("RequestBody") || annName.endsWith("ModelAttribute")) {
                    return param;
                }
            }
        }
        
        // Fallback: find first non-primitive, non-String parameter
        for (VariableElement param : method.getParameters()) {
            TypeMirror type = param.asType();
            if (type.getKind() == javax.lang.model.type.TypeKind.DECLARED) {
                String typeName = type.toString();
                if (!typeName.equals("java.lang.String") && !typeName.startsWith("java.lang.")) {
                    return param;
                }
            }
        }
        
        return null;
    }

    private boolean shouldCopyAnnotation(AnnotationMirror annotation) {
        String annotationType = annotation.getAnnotationType().toString();
        
        // Exclude framework annotations that we handle or replace
        if (annotationType.equals("com.fast.cqrs.cqrs.annotation.HttpController") ||
            annotationType.equals("com.fast.cqrs.cqrs.annotation.Query") ||
            annotationType.equals("com.fast.cqrs.cqrs.annotation.Command") ||
            annotationType.equals("java.lang.Override") ||
            annotationType.equals("javax.annotation.processing.Generated")) {
            return false;
        }
        
        return true;
    }
}
