package com.contextualmocker.api;

import com.contextualmocker.core.ContextID;

/**
 * Service Provider Interface implemented by the engine layer.
 * Client code should not depend on a particular implementation – use
 * {@link java.util.ServiceLoader} to obtain the default instance (see {@link Contextual}).
 */
public interface MockingEngine {

    <T> T mock(Class<T> type);

    <T> T spy(T realObject);

    ContextScope scopedContext(ContextID id);

    // lightweight helpers mirroring the old static API (subset for now)
    <T> ContextualStubbingInitiator<T> given(T mock);

    interface ContextualStubbingInitiator<T> {
        ContextSpecificStubbingInitiator<T> forContext(ContextID id);
    }

    interface ContextSpecificStubbingInitiator<T> {
        <R> OngoingContextualStubbing<T, R> when(java.util.function.Supplier<R> call);
    }

    interface OngoingContextualStubbing<T, R> {
        ContextSpecificStubbingInitiator<T> thenReturn(R value);
    }

    interface ContextScope extends AutoCloseable {
        ContextID getContextId();
        @Override void close();
    }
}