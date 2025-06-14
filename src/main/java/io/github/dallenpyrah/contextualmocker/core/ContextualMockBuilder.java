package io.github.dallenpyrah.contextualmocker.core;

import io.github.dallenpyrah.contextualmocker.api.OngoingContextualStubbing;
import io.github.dallenpyrah.contextualmocker.handlers.ContextualAnswer;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A builder pattern for performing context-aware mocking operations.
 * Provides a fluent API that reduces boilerplate when working with contexts.
 */
public class ContextualMockBuilder {
    private final ContextID contextId;

    public ContextualMockBuilder(ContextID contextId) {
        this.contextId = Objects.requireNonNull(contextId, "ContextID cannot be null");
    }

    /**
     * Creates a new builder for the given context.
     *
     * @param contextId The context identifier.
     * @return A new ContextualMockBuilder instance.
     */
    public static ContextualMockBuilder withContext(ContextID contextId) {
        return new ContextualMockBuilder(contextId);
    }

    /**
     * Starts stubbing for a mock in this context.
     *
     * @param <T> The type of the mock object.
     * @param <R> The return type of the method being stubbed.
     * @param mock The mock object to stub.
     * @param methodCall A lambda that calls the method to be stubbed.
     * @return An ongoing stubbing definition.
     */
    public <T, R> OngoingStubbingInBuilder<T, R> stub(T mock, Supplier<R> methodCall) {
        return new OngoingStubbingInBuilder<>(this, ContextualMocker.when(mock, contextId, methodCall));
    }

    /**
     * Wrapper for ongoing stubbing that returns to the builder after completion.
     */
    public static class OngoingStubbingInBuilder<T, R> {
        private final ContextualMockBuilder builder;
        private final OngoingContextualStubbing<T, R> stubbing;

        OngoingStubbingInBuilder(ContextualMockBuilder builder, OngoingContextualStubbing<T, R> stubbing) {
            this.builder = builder;
            this.stubbing = stubbing;
        }

        public OngoingStubbingInBuilder<T, R> whenStateIs(Object state) {
            stubbing.whenStateIs(state);
            return this;
        }

        public OngoingStubbingInBuilder<T, R> willSetStateTo(Object newState) {
            stubbing.willSetStateTo(newState);
            return this;
        }

        public OngoingStubbingInBuilder<T, R> ttlMillis(long ttlMillis) {
            stubbing.ttlMillis(ttlMillis);
            return this;
        }

        public ContextualMockBuilder thenReturn(R value) {
            stubbing.thenReturn(value);
            return builder;
        }

        public ContextualMockBuilder thenThrow(Throwable throwable) {
            stubbing.thenThrow(throwable);
            return builder;
        }

        public ContextualMockBuilder thenAnswer(ContextualAnswer<R> answer) {
            stubbing.thenAnswer(answer);
            return builder;
        }
    }

    /**
     * Performs verification for a mock in this context.
     *
     * @param <T> The type of the mock object.
     * @param mock The mock object to verify.
     * @param mode The verification mode (times, never, etc.).
     * @param methodCall A lambda that calls the method to be verified.
     * @return This builder for method chaining.
     */
    public <T> ContextualMockBuilder verify(T mock, ContextualVerificationMode mode, Supplier<?> methodCall) {
        ContextualMocker.verify(mock, contextId, mode, methodCall);
        return this;
    }

    /**
     * Verifies that no more interactions occurred on the given mock within this context
     * beyond those already verified.
     *
     * @param <T> The type of the mock object.
     * @param mock The mock object to check.
     * @return This builder for method chaining.
     */
    public <T> ContextualMockBuilder verifyNoMoreInteractions(T mock) {
        ContextualMocker.verifyNoMoreInteractions(mock, contextId);
        return this;
    }

    /**
     * Verifies that no interactions occurred at all on the given mock within this context.
     *
     * @param <T> The type of the mock object.
     * @param mock The mock object to check.
     * @return This builder for method chaining.
     */
    public <T> ContextualMockBuilder verifyNoInteractions(T mock) {
        ContextualMocker.verifyNoInteractions(mock, contextId);
        return this;
    }

    /**
     * Gets the context ID associated with this builder.
     *
     * @return The context ID.
     */
    public ContextID getContextId() {
        return contextId;
    }
}