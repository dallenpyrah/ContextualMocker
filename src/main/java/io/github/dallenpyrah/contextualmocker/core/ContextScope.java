package io.github.dallenpyrah.contextualmocker.core;

import io.github.dallenpyrah.contextualmocker.api.OngoingContextualStubbing;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A scoped context manager that automatically handles context setup and cleanup.
 * Implements AutoCloseable for use in try-with-resources blocks.
 * 
 * <p>Usage example:
 * <pre>{@code
 * try (var scope = ContextScope.withContext(contextId)) {
 *     scope.when(mock, () -> mock.method(arg)).thenReturn(value);
 *     scope.verify(mock, times(1), () -> mock.method(arg));
 * }
 * }</pre>
 */
public class ContextScope implements AutoCloseable {
    private final ContextID contextId;
    private final ContextID previousContext;
    private boolean closed = false;

    private ContextScope(ContextID contextId) {
        this.contextId = Objects.requireNonNull(contextId, "ContextID cannot be null");
        this.previousContext = ContextHolder.getCurrentContextIfSet();
        ContextHolder.setContext(contextId);
    }

    /**
     * Creates a new context scope with the given context ID.
     * The context is immediately set and will be restored when the scope is closed.
     *
     * @param contextId The context identifier to use within this scope.
     * @return A new ContextScope instance.
     * @throws NullPointerException if contextId is null.
     */
    public static ContextScope withContext(ContextID contextId) {
        return new ContextScope(contextId);
    }

    /**
     * Performs stubbing within this context scope.
     *
     * @param <T> The type of the mock object.
     * @param <R> The return type of the method being stubbed.
     * @param mock The mock object to stub.
     * @param methodCall A lambda that calls the method to be stubbed.
     * @return An ongoing stubbing definition.
     * @throws IllegalStateException if this scope has been closed.
     */
    public <T, R> OngoingContextualStubbing<T, R> when(T mock, Supplier<R> methodCall) {
        checkNotClosed();
        return ContextualMocker.when(mock, contextId, methodCall);
    }

    /**
     * Performs verification within this context scope.
     *
     * @param <T> The type of the mock object.
     * @param mock The mock object to verify.
     * @param mode The verification mode (times, never, etc.).
     * @param methodCall A lambda that calls the method to be verified.
     * @throws IllegalStateException if this scope has been closed.
     */
    public <T> void verify(T mock, ContextualVerificationMode mode, Supplier<?> methodCall) {
        checkNotClosed();
        ContextualMocker.verify(mock, contextId, mode, methodCall);
    }

    /**
     * Verifies that no more interactions occurred on the given mock within this context
     * beyond those already verified.
     *
     * @param <T> The type of the mock object.
     * @param mock The mock object to check.
     * @throws IllegalStateException if this scope has been closed.
     */
    public <T> void verifyNoMoreInteractions(T mock) {
        checkNotClosed();
        ContextualMocker.verifyNoMoreInteractions(mock, contextId);
    }

    /**
     * Verifies that no interactions occurred at all on the given mock within this context.
     *
     * @param <T> The type of the mock object.
     * @param mock The mock object to check.
     * @throws IllegalStateException if this scope has been closed.
     */
    public <T> void verifyNoInteractions(T mock) {
        checkNotClosed();
        ContextualMocker.verifyNoInteractions(mock, contextId);
    }

    /**
     * Gets the context ID associated with this scope.
     *
     * @return The context ID.
     * @throws IllegalStateException if this scope has been closed.
     */
    public ContextID getContextId() {
        checkNotClosed();
        return contextId;
    }

    /**
     * Closes this context scope and restores the previous context.
     * This method is idempotent - calling it multiple times has no additional effect.
     */
    @Override
    public void close() {
        if (!closed) {
            ContextHolder.setContext(previousContext);
            closed = true;
        }
    }

    /**
     * Checks if this scope has been closed.
     *
     * @return true if this scope has been closed, false otherwise.
     */
    public boolean isClosed() {
        return closed;
    }

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("ContextScope has been closed");
        }
    }
}