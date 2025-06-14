package io.github.dallenpyrah.contextualmocker.api;

import java.util.function.Supplier;

/**
 * Initiates stubbing for a specific method call within a context.
 * Extracted from ContextualMocker to follow Interface Segregation Principle.
 * @param <T> Mock type.
 */
public interface ContextSpecificStubbingInitiator<T> {
    /**
     * Specifies the method call to be stubbed.
     * The method should be called on the mock object passed to this method.
     * Example: {@code .when(() -> mock.someMethod(arg1, arg2))}
     * @param <R> The return type of the method being stubbed.
     * @param methodCallSupplier A lambda or method reference that calls the method to be stubbed.
     * @return An ongoing stubbing definition.
     */
    <R> OngoingContextualStubbing<T, R> when(Supplier<R> methodCallSupplier);
}
