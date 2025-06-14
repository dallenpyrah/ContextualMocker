package io.github.dallenpyrah.contextualmocker.api;

import io.github.dallenpyrah.contextualmocker.core.ContextID;

/**
 * Initiates verification for a specific mock, requiring context next.
 * Extracted from ContextualMocker to follow Interface Segregation Principle.
 * @param <T> Mock type.
 */
public interface ContextualVerificationInitiator<T> {
    /**
     * Specifies the context for the verification.
     * @param contextId The context identifier.
     * @return A context-specific verification initiator.
     */
    ContextSpecificVerificationInitiator<T> forContext(ContextID contextId);
}
