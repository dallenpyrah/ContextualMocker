package io.github.dallenpyrah.contextualmocker.handlers;
import io.github.dallenpyrah.contextualmocker.core.ContextID;

import java.lang.reflect.Method;

/**
 * Represents an action to be performed when a stubbed method is invoked
 * within a specific context. Allows for dynamic responses based on the
 * invocation details and context.
 *
 * @param <T> The type of the return value.
 */
@FunctionalInterface
public interface ContextualAnswer<T> {

    /**
     * Computes a return value or performs an action based on the invocation.
     *
     * @param contextId The context identifier active during the invocation.
     * @param mock The mock object instance.
     * @param method The method that was invoked.
     * @param arguments The arguments passed to the method.
     * @return The value to be returned by the stubbed method.
     * @throws Throwable An exception to be thrown by the stubbed method.
     */
    T answer(ContextID contextId, Object mock, Method method, Object[] arguments) throws Throwable;
}