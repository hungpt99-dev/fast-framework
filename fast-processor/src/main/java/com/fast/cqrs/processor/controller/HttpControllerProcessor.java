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
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.*;

/**
 * Annotation processor for {@link HttpController} interfaces.
 * <p>
 * Generates concrete controller implementation classes at compile-time.
 * This processor is designed for GraalVM native-image compatibility:
 * <ul>
 *   <li>Zero reflection at runtime</li>
 *   <li>Direct handler injection and invocation</li>
 *   <li>Compile-time validation of handler specifications</li>
 *   <li>Generated security checks from @PreAuthorize</li>
 * </ul>
 * <p>
 * For each {@code @HttpController} interface, generates a class named
 * {@code <Interface>_FastImpl} that:
 * <ul>
 *   <li>Implements all interface methods</li>
 *   <li>Copies Spring MVC annotations</li>
 *   <li>Injects handlers directly via constructor</li>
 *   <li>Calls handlers directly (no bus dispatch)</li>
 *   <li>Generates security checks inline</li>
 * </ul>
 * <p>
 * <b>IMPORTANT:</b> Every {@code @Query} and {@code @Command} method MUST specify
 * an explicit {@code handler} attribute. Auto-discovery via QueryBus/CommandBus
 * is not supported for GraalVM compatibility.
 *
 * @see HttpController
 * @see Query
 * @see Command
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("com.fast.cqrs.cqrs.annotation.HttpController")
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class HttpControllerProcessor extends AbstractProcessor {

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

    /**
     * Handler info collected from method annotations.
     */
    private record HandlerInfo(
        String fieldName,
        TypeName typeName,
        boolean isQueryHandler
    ) {}

    private void generateControllerImplementation(TypeElement controllerInterface) throws IOException {
        String packageName = elementUtils.getPackageOf(controllerInterface).getQualifiedName().toString();
        String interfaceName = controllerInterface.getSimpleName().toString();
        String implClassName = interfaceName + "_FastImpl";

        // Collect all handlers from method annotations
        Map<String, HandlerInfo> handlers = new LinkedHashMap<>();
        Map<ExecutableElement, HandlerInfo> methodHandlers = new HashMap<>();
        
        // Collect DTO types for GraalVM reflection hints
        Set<TypeName> dtoTypes = new LinkedHashSet<>();
        
        for (Element enclosed : controllerInterface.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) enclosed;
                HandlerInfo handlerInfo = extractHandlerInfo(method);
                if (handlerInfo != null) {
                    handlers.put(handlerInfo.fieldName(), handlerInfo);
                    methodHandlers.put(method, handlerInfo);
                }
                
                // Collect DTO types from parameters and return type
                collectDtoTypes(method, dtoTypes);
            }
        }

        // Build the implementation class (NOT final - allows Spring AOP if needed)
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(implClassName)
                .addModifiers(Modifier.PUBLIC)  // Removed FINAL for Spring AOP compatibility
                .addSuperinterface(TypeName.get(controllerInterface.asType()))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("jakarta.annotation", "Generated"))
                        .addMember("value", "$S", HttpControllerProcessor.class.getCanonicalName())
                        .addMember("date", "$S", java.time.Instant.now().toString())
                        .build())
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "RestController"))
                        .addMember("value", "$S", Character.toLowerCase(interfaceName.charAt(0)) + interfaceName.substring(1))
                        .build())
                .addJavadoc("Generated implementation for {@link $T}.\n", controllerInterface)
                .addJavadoc("<p>This class is generated at compile-time for GraalVM native-image compatibility.\n")
                .addJavadoc("<p>All handler invocations are direct method calls with zero reflection.\n")
                .addJavadoc("<p>Security checks are generated inline from @PreAuthorize annotations.\n");

        // Copy class-level Spring MVC annotations
        for (AnnotationMirror annotation : controllerInterface.getAnnotationMirrors()) {
            if (shouldCopyAnnotation(annotation)) {
                classBuilder.addAnnotation(AnnotationSpec.get(annotation));
            }
        }

        // Add fields for handlers (direct injection, no bus)
        for (HandlerInfo handler : handlers.values()) {
            classBuilder.addField(FieldSpec.builder(handler.typeName(), handler.fieldName(), 
                    Modifier.PRIVATE, Modifier.FINAL).build());
        }

        // Build constructor with all handlers
        MethodSpec.Builder ctorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        
        for (HandlerInfo handler : handlers.values()) {
            ctorBuilder.addParameter(handler.typeName(), handler.fieldName());
            ctorBuilder.addStatement("this.$N = $N", handler.fieldName(), handler.fieldName());
        }
        
        classBuilder.addMethod(ctorBuilder.build());

        // Generate method implementations
        for (Element enclosed : controllerInterface.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) enclosed;
                HandlerInfo handlerInfo = methodHandlers.get(method);
                classBuilder.addMethod(generateMethodImplementation(method, controllerInterface, handlerInfo));
            }
        }

        // Write the generated file
        JavaFile javaFile = JavaFile.builder(packageName, classBuilder.build())
                .addFileComment("Auto-generated by Fast CQRS Processor. DO NOT EDIT.")
                .addFileComment("\nThis class is GraalVM native-image compatible (zero reflection).")
                .indent("    ")
                .build();

        javaFile.writeTo(filer);
        
        // Generate RuntimeHints for GraalVM Native Image
        if (!dtoTypes.isEmpty()) {
            generateRuntimeHints(packageName, interfaceName, dtoTypes);
        }
    }
    
    /**
     * Collects DTO types from method parameters and return type for GraalVM reflection hints.
     */
    private void collectDtoTypes(ExecutableElement method, Set<TypeName> dtoTypes) {
        // Collect return type if it's a complex type
        TypeMirror returnType = method.getReturnType();
        if (isComplexDtoType(returnType)) {
            dtoTypes.add(TypeName.get(returnType));
        }
        
        // Collect parameter types
        for (VariableElement param : method.getParameters()) {
            TypeMirror paramType = param.asType();
            if (isComplexDtoType(paramType)) {
                dtoTypes.add(TypeName.get(paramType));
            }
        }
    }
    
    /**
     * Checks if a type is a complex DTO that needs reflection hints.
     */
    private boolean isComplexDtoType(TypeMirror type) {
        if (type.getKind() != javax.lang.model.type.TypeKind.DECLARED) {
            return false;
        }
        String typeName = type.toString();
        // Exclude primitives, common JDK types, and void
        if (typeName.startsWith("java.lang.") || 
            typeName.startsWith("java.util.") ||
            typeName.equals("void")) {
            return false;
        }
        return true;
    }
    
    /**
     * Generates a RuntimeHintsRegistrar class for GraalVM Native Image support.
     */
    private void generateRuntimeHints(String packageName, String controllerName, Set<TypeName> dtoTypes) throws IOException {
        String hintsClassName = controllerName + "_RuntimeHints";
        
        ClassName runtimeHintsRegistrar = ClassName.get("org.springframework.aot.hint", "RuntimeHintsRegistrar");
        ClassName runtimeHints = ClassName.get("org.springframework.aot.hint", "RuntimeHints");
        ClassName memberCategory = ClassName.get("org.springframework.aot.hint", "MemberCategory");
        
        // Build registerHints method body
        CodeBlock.Builder registerBody = CodeBlock.builder();
        for (TypeName dtoType : dtoTypes) {
            registerBody.addStatement("hints.reflection().registerType($T.class, $T.INVOKE_DECLARED_CONSTRUCTORS, $T.INVOKE_DECLARED_METHODS, $T.DECLARED_FIELDS)",
                    dtoType, memberCategory, memberCategory, memberCategory);
        }
        
        TypeSpec hintsClass = TypeSpec.classBuilder(hintsClassName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(runtimeHintsRegistrar)
                .addAnnotation(AnnotationSpec.builder(ClassName.get("jakarta.annotation", "Generated"))
                        .addMember("value", "$S", HttpControllerProcessor.class.getCanonicalName())
                        .build())
                .addJavadoc("GraalVM Native Image reflection hints for DTOs used by {@link $L}.\n", controllerName)
                .addJavadoc("<p>Register this class in META-INF/spring/aot.factories or use @ImportRuntimeHints.\n")
                .addMethod(MethodSpec.methodBuilder("registerHints")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(runtimeHints, "hints")
                        .addParameter(ClassName.get(ClassLoader.class), "classLoader")
                        .addCode(registerBody.build())
                        .build())
                .build();
        
        JavaFile.builder(packageName, hintsClass)
                .addFileComment("Auto-generated GraalVM Native Image hints by Fast CQRS Processor.")
                .indent("    ")
                .build()
                .writeTo(filer);
        
        logger.note("Generated RuntimeHints for " + controllerName + " with " + dtoTypes.size() + " DTO types");
    }

    /**
     * Extracts handler info from @Query or @Command annotation.
     * Returns null if no explicit handler is specified (will cause a compile error).
     */
    private HandlerInfo extractHandlerInfo(ExecutableElement method) {
        Query queryAnn = method.getAnnotation(Query.class);
        if (queryAnn != null) {
            TypeMirror handlerType = getHandlerTypeMirror(queryAnn);
            if (handlerType != null && !isDefaultHandler(handlerType, "Query$DefaultHandler")) {
                String fieldName = generateFieldName(handlerType);
                return new HandlerInfo(fieldName, TypeName.get(handlerType), true);
            }
        }
        
        Command commandAnn = method.getAnnotation(Command.class);
        if (commandAnn != null) {
            TypeMirror handlerType = getCommandHandlerTypeMirror(commandAnn);
            if (handlerType != null && !isDefaultHandler(handlerType, "Command$DefaultHandler")) {
                String fieldName = generateFieldName(handlerType);
                return new HandlerInfo(fieldName, TypeName.get(handlerType), false);
            }
        }
        
        return null;
    }

    private TypeMirror getHandlerTypeMirror(Query annotation) {
        try {
            annotation.handler();
            return null;
        } catch (MirroredTypeException e) {
            return e.getTypeMirror();
        }
    }

    private TypeMirror getCommandHandlerTypeMirror(Command annotation) {
        try {
            annotation.handler();
            return null;
        } catch (MirroredTypeException e) {
            return e.getTypeMirror();
        }
    }

    private boolean isDefaultHandler(TypeMirror type, String defaultName) {
        return type.toString().contains(defaultName);
    }

    private String generateFieldName(TypeMirror handlerType) {
        String simpleName = handlerType.toString();
        int lastDot = simpleName.lastIndexOf('.');
        if (lastDot >= 0) {
            simpleName = simpleName.substring(lastDot + 1);
        }
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    private MethodSpec generateMethodImplementation(ExecutableElement method, TypeElement controllerInterface, 
                                                     HandlerInfo handlerInfo) {
        Query queryAnn = method.getAnnotation(Query.class);
        Command commandAnn = method.getAnnotation(Command.class);

        // Build method manually to ensure parameter annotations are copied
        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(TypeName.get(method.getReturnType()));

        // Copy method-level Spring MVC annotations
        for (AnnotationMirror annotation : method.getAnnotationMirrors()) {
            if (shouldCopyAnnotation(annotation)) {
                builder.addAnnotation(AnnotationSpec.get(annotation));
            }
        }

        // Copy parameters WITH their annotations (critical for @RequestBody, @PathVariable, @Valid, etc.)
        for (VariableElement param : method.getParameters()) {
            ParameterSpec.Builder paramBuilder = ParameterSpec.builder(
                    TypeName.get(param.asType()),
                    param.getSimpleName().toString());
            
            // Copy ALL parameter annotations
            for (AnnotationMirror ann : param.getAnnotationMirrors()) {
                paramBuilder.addAnnotation(AnnotationSpec.get(ann));
            }
            
            builder.addParameter(paramBuilder.build());
        }

        // Generate security check if @PreAuthorize is present
        generateSecurityCheck(builder, method);

        if (queryAnn != null) {
            generateQueryDispatch(builder, method, handlerInfo);
        } else if (commandAnn != null) {
            generateCommandDispatch(builder, method, handlerInfo);
        } else {
            logger.warning("Method " + method.getSimpleName() + " has no @Query or @Command annotation", method);
            builder.addStatement("throw new $T($S)",
                    UnsupportedOperationException.class,
                    "Method must be annotated with @Query or @Command");
        }

        return builder.build();
    }

    /**
     * Generates inline security check from @PreAuthorize annotation.
     */
    private void generateSecurityCheck(MethodSpec.Builder builder, ExecutableElement method) {
        for (AnnotationMirror annotation : method.getAnnotationMirrors()) {
            String annotationName = annotation.getAnnotationType().toString();
            if (annotationName.equals("org.springframework.security.access.prepost.PreAuthorize")) {
                // Extract the expression value
                for (var entry : annotation.getElementValues().entrySet()) {
                    if (entry.getKey().getSimpleName().toString().equals("value")) {
                        String expression = entry.getValue().getValue().toString();
                        // Generate inline security check
                        builder.addStatement("if (!$T.evaluateExpression($S)) throw new $T($S)",
                                ClassName.get("com.fast.cqrs.web", "SecurityInvocationInterceptor"),
                                expression,
                                ClassName.get("com.fast.cqrs.security", "FastSecurityContext", "SecurityException"),
                                "Access is denied");
                        return;
                    }
                }
            }
        }
    }

    private void generateQueryDispatch(MethodSpec.Builder builder, ExecutableElement method, 
                                        HandlerInfo handlerInfo) {
        VariableElement queryParam = findPayloadParameter(method);

        if (handlerInfo == null) {
            // No explicit handler - compile error for GraalVM compatibility
            logger.error("@Query method '" + method.getSimpleName() + "' must specify handler attribute. " +
                        "Auto-discovery is disabled for GraalVM native-image compatibility. " +
                        "Example: @Query(handler = MyHandler.class)", method);
            builder.addStatement("throw new $T($S)",
                    UnsupportedOperationException.class,
                    "Handler not specified - see compilation error");
            return;
        }

        if (queryParam != null) {
            // Direct handler invocation - zero overhead, zero reflection
            builder.addStatement("return $N.handle($N)", handlerInfo.fieldName(), queryParam.getSimpleName());
        } else {
            logger.error("@Query method '" + method.getSimpleName() + 
                        "' must have a query parameter (annotated with @RequestBody or @ModelAttribute)", method);
            builder.addStatement("throw new $T($S)",
                    UnsupportedOperationException.class,
                    "Query parameter not found");
        }
    }

    private void generateCommandDispatch(MethodSpec.Builder builder, ExecutableElement method, 
                                          HandlerInfo handlerInfo) {
        VariableElement commandParam = findPayloadParameter(method);

        if (handlerInfo == null) {
            // No explicit handler - compile error for GraalVM compatibility
            logger.error("@Command method '" + method.getSimpleName() + "' must specify handler attribute. " +
                        "Auto-discovery is disabled for GraalVM native-image compatibility. " +
                        "Example: @Command(handler = MyHandler.class)", method);
            builder.addStatement("throw new $T($S)",
                    UnsupportedOperationException.class,
                    "Handler not specified - see compilation error");
            return;
        }

        if (commandParam != null) {
            // Direct handler invocation - zero overhead, zero reflection
            builder.addStatement("$N.handle($N)", handlerInfo.fieldName(), commandParam.getSimpleName());
        } else {
            logger.error("@Command method '" + method.getSimpleName() + 
                        "' must have a command parameter (annotated with @RequestBody)", method);
            builder.addStatement("throw new $T($S)",
                    UnsupportedOperationException.class,
                    "Command parameter not found");
        }

        // Handle return type
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
        
        // Exclude framework annotations that shouldn't be copied
        // NOTE: We MUST copy @Query and @Command for AOP aspects to work!
        if (annotationType.equals("com.fast.cqrs.cqrs.annotation.HttpController") ||
            annotationType.equals("java.lang.Override") ||
            annotationType.equals("jakarta.annotation.Generated") ||
            annotationType.equals("org.springframework.security.access.prepost.PreAuthorize")) {
            return false;
        }
        
        return true;
    }
}
