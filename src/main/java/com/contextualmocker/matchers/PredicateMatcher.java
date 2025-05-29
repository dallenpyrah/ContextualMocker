package com.contextualmocker.matchers;

import java.util.function.Predicate;

/**
 * Matcher that matches arguments using a custom predicate.
 *
 * @param <T> The type of the argument.
 */
public class PredicateMatcher<T> implements ArgumentMatcher {
    private final Predicate<T> predicate;

    public PredicateMatcher(Predicate<T> predicate) {
        this.predicate = predicate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean matches(Object argument) {
        try {
            return predicate.test((T) argument);
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "argThat(<custom predicate>)";
    }
}