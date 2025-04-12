package com.contextualmocker.matchers;

import java.util.Objects;

/**
 * Provides static methods for creating argument matchers used in stubbing and verification.
 * These methods register matchers in a thread-local context, which are then consumed
 * by the framework during method invocation or verification.
 */
public final class ArgumentMatchers {
    private ArgumentMatchers() {}

    /**
     * Matches any object (including null).
     *
     * @param <T> The type of the argument.
     * @return A placeholder value (null); the actual matching logic is handled internally.
     */
    public static <T> T any() {
        MatcherContext.addMatcher(new AnyMatcher());
        return null;
    }

    /**
     * Matches any int or Integer.
     *
     * @return A placeholder value (0).
     */
    public static int anyInt() {
        MatcherContext.addMatcher(new AnyMatcher());
        return 0;
    }

    /**
     * Matches any long or Long.
     *
     * @return A placeholder value (0L).
     */
    public static long anyLong() {
        MatcherContext.addMatcher(new AnyMatcher());
        return 0L;
    }

    /**
     * Matches any double or Double.
     *
     * @return A placeholder value (0.0d).
     */
    public static double anyDouble() {
        MatcherContext.addMatcher(new AnyMatcher());
        return 0.0d;
    }

    /**
     * Matches any float or Float.
     *
     * @return A placeholder value (0.0f).
     */
    public static float anyFloat() {
        MatcherContext.addMatcher(new AnyMatcher());
        return 0.0f;
    }

    /**
     * Matches any boolean or Boolean.
     *
     * @return A placeholder value (false).
     */
    public static boolean anyBoolean() {
        MatcherContext.addMatcher(new AnyMatcher());
        return false;
    }

    /**
     * Matches any byte or Byte.
     *
     * @return A placeholder value (0).
     */
    public static byte anyByte() {
        MatcherContext.addMatcher(new AnyMatcher());
        return (byte) 0;
    }

    /**
     * Matches any short or Short.
     *
     * @return A placeholder value (0).
     */
    public static short anyShort() {
        MatcherContext.addMatcher(new AnyMatcher());
        return (short) 0;
    }

    /**
     * Matches any char or Character.
     *
     * @return A placeholder value ('\u0000').
     */
    public static char anyChar() {
        MatcherContext.addMatcher(new AnyMatcher());
        return '\u0000';
    }

    /**
     * Matches an argument that is equal to the given value.
     * Equality is determined using {@link Objects#deepEquals(Object, Object)}.
     *
     * @param <T> The type of the argument.
     * @param value The value the argument must be equal to.
     * @return The provided value; the actual matching logic is handled internally.
     */
    public static <T> T eq(T value) {
        MatcherContext.addMatcher(new EqMatcher<>(value));
        return value;
    }
}