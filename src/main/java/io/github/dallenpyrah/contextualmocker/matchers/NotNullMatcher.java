package io.github.dallenpyrah.contextualmocker.matchers;

/**
 * Matcher that matches non-null values.
 *
 * @param <T> The type of the argument.
 */
public class NotNullMatcher<T> implements ArgumentMatcher {
    
    @Override
    public boolean matches(Object argument) {
        return argument != null;
    }

    @Override
    public String toString() {
        return "notNull()";
    }
}