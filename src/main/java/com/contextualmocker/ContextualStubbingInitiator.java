package com.contextualmocker;

/**
 * Initiates the stubbing process for a specific mock object.
 *
 * @param <T> The type of the mock object.
 */
public interface ContextualStubbingInitiator<T> {

    /**
     * Specifies the context for which the subsequent stubbing will apply.
     *
     * @param contextId The context identifier. Must not be null.
     * @return An initiator ready to define the method call to be stubbed.
     */
    ContextSpecificStubbingInitiator<T> forContext(ContextID contextId);
}