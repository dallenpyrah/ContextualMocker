package io.github.dallenpyrah.contextualmocker.core;

/**
 * Provides default values for primitive and reference types.
 * Extracted from ContextualMocker to follow Single Responsibility Principle.
 */
public final class DefaultValueProvider {
    
    private DefaultValueProvider() {}
    
    /**
     * Returns the default value for the given type.
     * For primitives, returns their default values (0, false, etc.).
     * For reference types, returns null.
     * 
     * @param type The class type to get default value for
     * @return The default value for the type
     */
    public static Object getDefaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0d;
        if (type == char.class) return '\u0000';
        
        return null;
    }
}
