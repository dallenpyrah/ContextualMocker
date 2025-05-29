package com.contextualmocker.matchers;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.Collection;

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

    /**
     * Matches any String.
     *
     * @return A placeholder value (empty string).
     */
    public static String anyString() {
        MatcherContext.addMatcher(new TypeMatcher<>(String.class));
        return "";
    }

    /**
     * Matches any Collection.
     *
     * @param <T> The collection type.
     * @return A placeholder value (null).
     */
    public static <T extends Collection<?>> T anyCollection() {
        MatcherContext.addMatcher(new TypeMatcher<>(Collection.class));
        return null;
    }

    /**
     * Matches null values.
     *
     * @param <T> The type of the argument.
     * @return A placeholder value (null).
     */
    public static <T> T isNull() {
        MatcherContext.addMatcher(new NullMatcher<>());
        return null;
    }

    /**
     * Matches non-null values.
     *
     * @param <T> The type of the argument.
     * @return A placeholder value (null).
     */
    public static <T> T notNull() {
        MatcherContext.addMatcher(new NotNullMatcher<>());
        return null;
    }

    /**
     * Matches strings that contain the specified substring.
     *
     * @param substring The substring to search for.
     * @return A placeholder value (empty string).
     */
    public static String contains(String substring) {
        MatcherContext.addMatcher(new ContainsMatcher(substring));
        return "";
    }

    /**
     * Matches strings that start with the specified prefix.
     *
     * @param prefix The prefix to match.
     * @return A placeholder value (empty string).
     */
    public static String startsWith(String prefix) {
        MatcherContext.addMatcher(new StartsWithMatcher(prefix));
        return "";
    }

    /**
     * Matches strings that end with the specified suffix.
     *
     * @param suffix The suffix to match.
     * @return A placeholder value (empty string).
     */
    public static String endsWith(String suffix) {
        MatcherContext.addMatcher(new EndsWithMatcher(suffix));
        return "";
    }

    /**
     * Matches strings that match the specified regular expression pattern.
     *
     * @param pattern The regex pattern to match.
     * @return A placeholder value (empty string).
     */
    public static String matches(String pattern) {
        MatcherContext.addMatcher(new RegexMatcher(pattern));
        return "";
    }

    /**
     * Matches strings that match the specified compiled Pattern.
     *
     * @param pattern The compiled Pattern to match.
     * @return A placeholder value (empty string).
     */
    public static String matches(Pattern pattern) {
        MatcherContext.addMatcher(new RegexMatcher(pattern));
        return "";
    }

    /**
     * Matches arguments that satisfy the given predicate.
     *
     * @param <T> The type of the argument.
     * @param predicate The predicate to test the argument.
     * @return A placeholder value (null).
     */
    public static <T> T argThat(Predicate<T> predicate) {
        MatcherContext.addMatcher(new PredicateMatcher<>(predicate));
        return null;
    }

    /**
     * Matches numbers within the specified range (inclusive).
     *
     * @param min The minimum value (inclusive).
     * @param max The maximum value (inclusive).
     * @return A placeholder value (0).
     */
    public static int intThat(int min, int max) {
        MatcherContext.addMatcher(new RangeMatcher<>(min, max));
        return 0;
    }

    /**
     * Matches numbers within the specified range (inclusive).
     *
     * @param min The minimum value (inclusive).
     * @param max The maximum value (inclusive).
     * @return A placeholder value (0L).
     */
    public static long longThat(long min, long max) {
        MatcherContext.addMatcher(new RangeMatcher<>(min, max));
        return 0L;
    }

    /**
     * Matches numbers within the specified range (inclusive).
     *
     * @param min The minimum value (inclusive).
     * @param max The maximum value (inclusive).
     * @return A placeholder value (0.0).
     */
    public static double doubleThat(double min, double max) {
        MatcherContext.addMatcher(new RangeMatcher<>(min, max));
        return 0.0;
    }
}