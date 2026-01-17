package com.fast.cqrs.processor.sql;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.*;

/**
 * Metadata extracted from a repository method at compile-time.
 * <p>
 * This class caches all the information needed to generate the
 * method implementation, avoiding repeated reflection lookups.
 */
public record MethodMetadata(
        /**
         * The method element being processed.
         */
        ExecutableElement method,

        /**
         * The SQL statement to execute.
         */
        String sql,

        /**
         * Whether this is a SELECT (query) or EXECUTE (update) method.
         */
        OperationType operationType,

        /**
         * Parameter name to parameter element mapping.
         */
        Map<String, VariableElement> namedParameters,

        /**
         * The return type of the method.
         */
        TypeMirror returnType,

        /**
         * For collections/optionals, the element type.
         */
        TypeMirror elementType,

        /**
         * The return type category for code generation.
         */
        ReturnTypeKind returnTypeKind
) {
    /**
     * Type of SQL operation.
     */
    public enum OperationType {
        SELECT,
        EXECUTE,
        CRUD
    }

    /**
     * Category of return type for code generation.
     */
    public enum ReturnTypeKind {
        VOID,
        SINGLE_OBJECT,
        OPTIONAL,
        LIST,
        SET,
        COLLECTION,
        PRIMITIVE_INT,
        PRIMITIVE_LONG,
        BOXED_INT,
        BOXED_LONG
    }

    /**
     * Builder for creating MethodMetadata instances.
     */
    public static class Builder {
        private ExecutableElement method;
        private String sql;
        private OperationType operationType;
        private Map<String, VariableElement> namedParameters = new LinkedHashMap<>();
        private TypeMirror returnType;
        private TypeMirror elementType;
        private ReturnTypeKind returnTypeKind;

        public Builder method(ExecutableElement method) {
            this.method = method;
            this.returnType = method.getReturnType();
            return this;
        }

        public Builder sql(String sql) {
            this.sql = sql;
            return this;
        }

        public Builder operationType(OperationType operationType) {
            this.operationType = operationType;
            return this;
        }

        public Builder addParameter(String name, VariableElement element) {
            this.namedParameters.put(name, element);
            return this;
        }

        public Builder elementType(TypeMirror elementType) {
            this.elementType = elementType;
            return this;
        }

        public Builder returnTypeKind(ReturnTypeKind returnTypeKind) {
            this.returnTypeKind = returnTypeKind;
            return this;
        }

        public MethodMetadata build() {
            return new MethodMetadata(
                    method,
                    sql,
                    operationType,
                    Collections.unmodifiableMap(namedParameters),
                    returnType,
                    elementType,
                    returnTypeKind
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the simple method name.
     */
    public String methodName() {
        return method.getSimpleName().toString();
    }

    /**
     * Gets the list of parameters in order.
     */
    public List<? extends VariableElement> parameters() {
        return method.getParameters();
    }
}
