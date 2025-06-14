package io.github.dallenpyrah.contextualmocker.core;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Verification mode specifying a minimum number of invocations.
 * Extracted from ContextualMocker to follow Single Responsibility Principle.
 */
public class AtLeastVerificationMode implements ContextualVerificationMode {
    private final int min;
    
    public AtLeastVerificationMode(int min) {
        this.min = min;
    }
    
    public void verifyCount(int actual, Method method, Object[] args) {
        if (actual < min) {
            throw new AssertionError("Wanted at least " + min + " invocations but got only " + 
                actual + " for method '" + method.getName() + "' with arguments " + Arrays.toString(args));
        }
    }
    
    @Override
    public void verifyCountWithContext(int actual, Method method, Object[] args, 
                                     Object mock, ContextID contextId, List<InvocationRecord> allInvocations) {
        if (actual < min) {
            throw new VerificationFailureException(
                mock, contextId, method, args, min, actual, 
                "at least " + min + " time" + (min == 1 ? "" : "s"), allInvocations);
        }
    }
}
