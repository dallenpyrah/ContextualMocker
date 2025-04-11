package com.contextualmocker.matchers;

import java.util.Objects;

public final class ArgumentMatchers {
    private ArgumentMatchers() {}

    public static <T> T any() {
        MatcherContext.addMatcher(new AnyMatcher<>());
        return null;
    }

    public static int anyInt() {
        MatcherContext.addMatcher(new AnyMatcher<>());
        return 0;
    }

    public static long anyLong() {
        MatcherContext.addMatcher(new AnyMatcher<>());
        return 0L;
    }

    public static double anyDouble() {
        MatcherContext.addMatcher(new AnyMatcher<>());
        return 0.0d;
    }

    public static float anyFloat() {
        MatcherContext.addMatcher(new AnyMatcher<>());
        return 0.0f;
    }

    public static boolean anyBoolean() {
        MatcherContext.addMatcher(new AnyMatcher<>());
        return false;
    }

    public static byte anyByte() {
        MatcherContext.addMatcher(new AnyMatcher<>());
        return (byte) 0;
    }

    public static short anyShort() {
        MatcherContext.addMatcher(new AnyMatcher<>());
        return (short) 0;
    }

    public static char anyChar() {
        MatcherContext.addMatcher(new AnyMatcher<>());
        return '\u0000';
    }

    public static <T> T eq(T value) {
    MatcherContext.addMatcher(new EqMatcher<>(value));
    return value;
}
}