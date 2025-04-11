package com.contextualmocker.matchers;

import java.util.ArrayList;
import java.util.List;

public final class MatcherContext {
    private static final ThreadLocal<List<ArgumentMatcher<?>>> matchers =
            ThreadLocal.withInitial(ArrayList::new);

    static void addMatcher(ArgumentMatcher<?> matcher) {
        matchers.get().add(matcher);
    }

    public static List<ArgumentMatcher<?>> consumeMatchers() {
        List<ArgumentMatcher<?>> current = new ArrayList<>(matchers.get());
        System.out.println("[MatcherContext] Thread: " + Thread.currentThread().getName() + " consuming matchers: " + current);
        matchers.get().clear();
        return current;
    }

    static void clear() {
        matchers.get().clear();
    }
}