package io.github.dallenpyrah.contextualmocker.api;

import io.github.dallenpyrah.contextualmocker.core.ContextualVerificationMode;
import java.util.function.Supplier;

/**
 * Initiates verification for a specific method call within a context, using a verification mode.
 * Extracted from ContextualMocker to follow Interface Segregation Principle.
 * @param <T> Mock type.
 */
public interface ContextSpecificVerificationInitiator<T> {
    /**
     * Specifies the verification mode (e.g., times(1), never()) and prepares for the method call.
     * @param mode The verification mode.
     * @return The mock object proxy; call the method to be verified on this proxy.
     */
    T verify(ContextualVerificationMode mode);

    /**
     * Directly verifies a method call with the specified mode and method call.
     * This eliminates the need for a separate verify() call on the returned proxy.
     * 
     * @param mode The verification mode.
     * @param methodCall A lambda that calls the method to be verified.
     */
    void that(ContextualVerificationMode mode, Supplier<?> methodCall);
}
