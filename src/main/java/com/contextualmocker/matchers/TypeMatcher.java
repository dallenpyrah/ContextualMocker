package com.contextualmocker.matchers;

/**
 * Matcher that matches arguments of a specific type.
 *
 * @param <T> The type to match.
 */
public class TypeMatcher<T> implements ArgumentMatcher {
    private final Class<T> expectedType;

    public TypeMatcher(Class<T> expectedType) {
        this.expectedType = expectedType;
    }

    @Override
    public boolean matches(Object argument) {
        return argument != null && expectedType.isAssignableFrom(argument.getClass());
    }

    @Override
    public String toString() {
        return "isA(" + expectedType.getSimpleName() + ")";
    }
}