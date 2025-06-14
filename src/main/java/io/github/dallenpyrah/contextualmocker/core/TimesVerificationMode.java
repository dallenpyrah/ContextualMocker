package io.github.dallenpyrah.contextualmocker.core;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Verification mode specifying an exact number of invocations.
 * Extracted from ContextualMocker to follow Single Responsibility Principle.
 */
public class TimesVerificationMode implements ContextualVerificationMode {
    private final int wanted;
    
    public TimesVerificationMode(int wanted) {
        this.wanted = wanted;
    }
    
    public void verifyCount(int actual, Method method, Object[] args) {
        if (actual != wanted) {
            throw new AssertionError("Wanted " + wanted + " invocations but got " + actual + 
                " for method '" + method.getName() + "' with arguments " + Arrays.toString(args));
        }
    }
    
    @Override
    public void verifyCountWithContext(int actual, Method method, Object[] args, 
                                     Object mock, ContextID contextId, List<InvocationRecord> allInvocations) {
        if (actual != wanted) {
            throw new VerificationFailureException(
                mock, contextId, method, args, wanted, actual, 
                "exactly " + wanted + " time" + (wanted == 1 ? "" : "s"), allInvocations);
        }
    }
}
