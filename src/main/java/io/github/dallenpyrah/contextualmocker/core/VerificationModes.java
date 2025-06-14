package io.github.dallenpyrah.contextualmocker.core;

/**
 * Factory class for creating verification modes.
 * Extracted from ContextualMocker to follow Single Responsibility Principle.
 */
public final class VerificationModes {
    
    private VerificationModes() {}
    
    /**
     * Creates a verification mode that expects an exact number of invocations.
     *
     * @param wantedNumberOfInvocations The exact number of invocations expected.
     * @return A verification mode.
     */
    public static ContextualVerificationMode times(int wantedNumberOfInvocations) {
        return new TimesVerificationMode(wantedNumberOfInvocations);
    }

    /**
     * Creates a verification mode that expects zero invocations.
     *
     * @return A verification mode.
     */
    public static ContextualVerificationMode never() {
        return new TimesVerificationMode(0);
    }

    /**
     * Creates a verification mode that expects at least one invocation.
     *
     * @return A verification mode.
     */
    public static ContextualVerificationMode atLeastOnce() {
        return new AtLeastVerificationMode(1);
    }

    /**
     * Creates a verification mode that expects at least a minimum number of invocations.
     *
     * @param minInvocations The minimum number of invocations expected.
     * @return A verification mode.
     */
    public static ContextualVerificationMode atLeast(int minInvocations) {
        return new AtLeastVerificationMode(minInvocations);
    }

    /**
     * Creates a verification mode that expects at most a maximum number of invocations.
     *
     * @param maxInvocations The maximum number of invocations allowed.
     * @return A verification mode.
     */
    public static ContextualVerificationMode atMost(int maxInvocations) {
        return new AtMostVerificationMode(maxInvocations);
    }
}
