package com.contextualmocker;

import java.util.Objects;

public final class ArgumentMatchers {
    private ArgumentMatchers() {}

    public static <T> T any() {
        MatcherContext.addMatcher(new AnyMatcher<>());
        return null;
    }

    public static <T> T eq(T value) {
        MatcherContext.addMatcher(new EqMatcher<>(value));
        return value;
    }

    public interface ArgumentMatcher<T> {
        boolean matches(T argument);
    }

    static class AnyMatcher<T> implements ArgumentMatcher<T> {
        @Override
        public boolean matches(T argument) {
            return true;
        }
    }

    static class EqMatcher<T> implements ArgumentMatcher<T> {
        private final T expected;
        EqMatcher(T expected) {
            this.expected = expected;
        }
        @Override
        public boolean matches(T argument) {
            return Objects.equals(expected, argument);
        }
    }
}