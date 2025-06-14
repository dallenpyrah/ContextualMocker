package io.github.dallenpyrah.contextualmocker.core;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Verification mode specifying a maximum number of invocations.
 * Extracted from ContextualMocker to follow Single Responsibility Principle.
 */
public class AtMostVerificationMode implements ContextualVerificationMode {
    private final int max;

    public AtMostVerificationMode(int max) {
        this.max = max;
    }

    public void verifyCount(int actual, Method method, Object[] args) {
        if (actual > max) {
            throw new AssertionError(
                    "Wanted at most " + max + " invocations but got " + actual + 
                    " for method '" + method.getName() + "' with arguments " + Arrays.toString(args));
        }
    }
    
    @Override
    public void verifyCountWithContext(int actual, Method method, Object[] args, 
                                     Object mock, ContextID contextId, List<InvocationRecord> allInvocations) {
        if (actual > max) {
            throw new VerificationFailureException(
                mock, contextId, method, args, max, actual, 
                "at most " + max + " time" + (max == 1 ? "" : "s"), allInvocations);
        }
    }
}
