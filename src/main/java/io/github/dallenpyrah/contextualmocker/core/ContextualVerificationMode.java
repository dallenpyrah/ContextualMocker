package io.github.dallenpyrah.contextualmocker.core;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Interface for different verification modes (times, atLeast, etc.).
 * Extracted from ContextualMocker to follow Interface Segregation Principle.
 */
public interface ContextualVerificationMode {
    /**
     * Verifies the count with enhanced error reporting.
     */
    default void verifyCount(int actual, Method method, Object[] args) {
        verifyCountWithContext(actual, method, args, null, null, null);
    }
    
    /**
     * Verifies the count with full context for enhanced error messages.
     */
    void verifyCountWithContext(int actual, Method method, Object[] args, 
                              Object mock, ContextID contextId, List<InvocationRecord> allInvocations);
}
