package io.github.dallenpyrah.contextualmocker.matchers;

/**
 * Matcher that matches numeric values within a specified range (inclusive).
 *
 * @param <T> The numeric type.
 */
public class RangeMatcher<T extends Number & Comparable<T>> implements ArgumentMatcher {
    private final T min;
    private final T max;

    public RangeMatcher(T min, T max) {
        this.min = min;
        this.max = max;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean matches(Object argument) {
        if (!(argument instanceof Number)) {
            return false;
        }
        
        try {
            T value = (T) argument;
            return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "inRange(" + min + ", " + max + ")";
    }
}