package com.contextualmocker;

import java.util.Objects;

class EqMatcher<T> implements ArgumentMatcher<T> {
    private final T expected;

    EqMatcher(T expected) {
        this.expected = expected;
    }

    @Override
    public boolean matches(Object argument) {
        return Objects.equals(expected, argument);
    }
}