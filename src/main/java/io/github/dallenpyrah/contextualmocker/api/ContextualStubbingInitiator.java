package io.github.dallenpyrah.contextualmocker.api;

import io.github.dallenpyrah.contextualmocker.core.ContextID;

/**
 * Initiates stubbing for a specific mock, requiring context next.
 * Extracted from ContextualMocker to follow Interface Segregation Principle.
 * @param <T> Mock type.
 */
public interface ContextualStubbingInitiator<T> {
    /**
     * Specifies the context for the stubbing rule.
     * @param contextId The context identifier.
     * @return A context-specific stubbing initiator.
     */
    ContextSpecificStubbingInitiator<T> forContext(ContextID contextId);
}
