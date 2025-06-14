package io.github.dallenpyrah.contextualmocker.api;

import io.github.dallenpyrah.contextualmocker.handlers.ContextualAnswer;

/**
 * Defines the behavior (return value, exception, answer, state changes) for a stubbed method call.
 * Extracted from ContextualMocker to follow Interface Segregation Principle.
 * @param <T> Mock type.
 * @param <R> Return type of the stubbed method.
 */
public interface OngoingContextualStubbing<T, R> {
    /**
     * Specifies a state condition for this stubbing rule.
     * The rule will only apply if the mock is in the given state for the current context.
     * @param state The required state.
     * @return This ongoing stubbing definition.
     */
    OngoingContextualStubbing<T, R> whenStateIs(Object state);
    
    /**
     * Specifies a state transition to occur after this stubbed method is invoked.
     * The mock's state for the current context will be updated to the new state.
     * @param newState The target state.
     * @return This ongoing stubbing definition.
     */
    OngoingContextualStubbing<T, R> willSetStateTo(Object newState);
    
    /**
     * Sets the time-to-live (TTL) for this specific stubbing rule.
     * If the rule is not matched within this duration (in milliseconds) after creation,
     * it will be automatically removed. A TTL of 0 or less means the rule never expires.
     * @param ttlMillis The time-to-live in milliseconds.
     * @return This ongoing stubbing definition, allowing chaining before thenReturn/thenThrow/thenAnswer.
     */
    OngoingContextualStubbing<T, R> ttlMillis(long ttlMillis);
    
    /**
     * Specifies the value to return when the stubbed method is called.
     * @param value The value to return.
     * @return A context-specific stubbing initiator for defining further rules.
     */
    ContextSpecificStubbingInitiator<T> thenReturn(R value);
    
    /**
     * Specifies the throwable to be thrown when the stubbed method is called.
     * @param throwable The throwable instance to throw.
     * @return A context-specific stubbing initiator for defining further rules.
     */
    ContextSpecificStubbingInitiator<T> thenThrow(Throwable throwable);
    
    /**
     * Specifies a custom answer (dynamic behavior) when the stubbed method is called.
     * @param answer The custom answer implementation.
     * @return A context-specific stubbing initiator for defining further rules.
     */
    ContextSpecificStubbingInitiator<T> thenAnswer(ContextualAnswer<R> answer);
}
