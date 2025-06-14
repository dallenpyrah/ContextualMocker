package io.github.dallenpyrah.contextualmocker.matchers;

/**
 * Matcher that matches null values.
 *
 * @param <T> The type of the argument.
 */
public class NullMatcher<T> implements ArgumentMatcher {
    
    @Override
    public boolean matches(Object argument) {
        return argument == null;
    }

    @Override
    public String toString() {
        return "isNull()";
    }
}