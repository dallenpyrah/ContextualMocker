package com.contextualmocker;

import java.util.ArrayList;
import java.util.List;

final class MatcherContext {
    private static final ThreadLocal<List<ArgumentMatchers.ArgumentMatcher<?>>> matchers =
            ThreadLocal.withInitial(ArrayList::new);

    static void addMatcher(ArgumentMatchers.ArgumentMatcher<?> matcher) {
        matchers.get().add(matcher);
    }

    static List<ArgumentMatchers.ArgumentMatcher<?>> consumeMatchers() {
        List<ArgumentMatchers.ArgumentMatcher<?>> current = new ArrayList<>(matchers.get());
        matchers.get().clear();
        return current;
    }

    static void clear() {
        matchers.get().clear();
    }
}