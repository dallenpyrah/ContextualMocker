package com.contextualmocker;

import java.util.ArrayList;
import java.util.List;

final class MatcherContext {
    private static final ThreadLocal<List<ArgumentMatcher<?>>> matchers =
            ThreadLocal.withInitial(ArrayList::new);

    static void addMatcher(ArgumentMatcher<?> matcher) {
        matchers.get().add(matcher);
    }

    static List<ArgumentMatcher<?>> consumeMatchers() {
        List<ArgumentMatcher<?>> current = new ArrayList<>(matchers.get());
        matchers.get().clear();
        return current;
    }

    static void clear() {
        matchers.get().clear();
    }
}