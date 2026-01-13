package com.fast.cqrs.cqrs;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Utility class for resolving generic type arguments.
 */
public final class GenericTypeResolver {

    private GenericTypeResolver() {
        // Utility class
    }

    /**
     * Resolves the type argument at the specified index for the given target class.
     *
     * @param clazz       the class to inspect
     * @param targetClass the generic interface or superclass to look for
     * @param index       the index of the type parameter (0-based)
     * @return the resolved type, or Object.class if not resolvable
     */
    public static Class<?> resolveTypeArgument(Class<?> clazz, Class<?> targetClass, int index) {
        Type[] genericInterfaces = clazz.getGenericInterfaces();
        
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType parameterizedType) {
                Type rawType = parameterizedType.getRawType();
                if (rawType.equals(targetClass)) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (index < typeArguments.length) {
                        Type typeArg = typeArguments[index];
                        if (typeArg instanceof Class<?> classType) {
                            return classType;
                        }
                    }
                }
            }
        }
        
        // Check superclass
        Type genericSuperclass = clazz.getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            if (rawType.equals(targetClass)) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                if (index < typeArguments.length) {
                    Type typeArg = typeArguments[index];
                    if (typeArg instanceof Class<?> classType) {
                        return classType;
                    }
                }
            }
        }
        
        // Recursively check superclass
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            return resolveTypeArgument(superclass, targetClass, index);
        }
        
        return Object.class;
    }
}
