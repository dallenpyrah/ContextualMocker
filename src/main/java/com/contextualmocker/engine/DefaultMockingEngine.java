package com.contextualmocker.engine;

import com.contextualmocker.api.MockingEngine;
import com.contextualmocker.core.ContextID;
import com.contextualmocker.core.ContextualMocker;

/**
 * Simple bridge that adapts the existing static ContextualMocker implementation
 * to the new {@link MockingEngine} SPI expected by consumers of the {@code api} module.
 */
public final class DefaultMockingEngine implements MockingEngine {

    @Override
    public <T> T mock(Class<T> type) {
        return ContextualMocker.mock(type);
    }

    @Override
    public <T> T spy(T realObject) {
        return ContextualMocker.spy(realObject);
    }

    @Override
    public MockingEngine.ContextScope scopedContext(ContextID id) {
        com.contextualmocker.core.ContextScope delegate = ContextualMocker.scopedContext(id);
        return new ContextScopeAdapter(delegate);
    }

    @Override
    public <T> MockingEngine.ContextualStubbingInitiator<T> given(T mock) {
        com.contextualmocker.core.ContextualMocker.ContextualStubbingInitiator<T> delegate = ContextualMocker.given(mock);
        return new StubbingInitiatorAdapter<>(delegate);
    }

    /* -------------------------------------------------- */
    private static class ContextScopeAdapter implements MockingEngine.ContextScope {
        private final com.contextualmocker.core.ContextScope delegate;
        ContextScopeAdapter(com.contextualmocker.core.ContextScope d) { this.delegate = d; }
        @Override public ContextID getContextId() { return delegate.getContextId(); }
        @Override public void close() { delegate.close(); }
    }

    private static class StubbingInitiatorAdapter<T> implements MockingEngine.ContextualStubbingInitiator<T> {
        private final com.contextualmocker.core.ContextualMocker.ContextualStubbingInitiator<T> delegate;
        StubbingInitiatorAdapter(com.contextualmocker.core.ContextualMocker.ContextualStubbingInitiator<T> d) { this.delegate = d; }
        @Override
        public MockingEngine.ContextSpecificStubbingInitiator<T> forContext(ContextID id) {
            com.contextualmocker.core.ContextualMocker.ContextSpecificStubbingInitiator<T> inner = delegate.forContext(id);
            return new ContextSpecificStubbingInitiatorAdapter<>(inner);
        }
    }

    private static class ContextSpecificStubbingInitiatorAdapter<T> implements MockingEngine.ContextSpecificStubbingInitiator<T> {
        private final com.contextualmocker.core.ContextualMocker.ContextSpecificStubbingInitiator<T> delegate;
        ContextSpecificStubbingInitiatorAdapter(com.contextualmocker.core.ContextualMocker.ContextSpecificStubbingInitiator<T> d) { this.delegate = d; }
        @Override
        public <R> MockingEngine.OngoingContextualStubbing<T, R> when(java.util.function.Supplier<R> call) {
            com.contextualmocker.core.ContextualMocker.OngoingContextualStubbing<T, R> inner = delegate.when(call);
            return new OngoingStubbingAdapter<>(inner);
        }
    }

    private static class OngoingStubbingAdapter<T, R> implements MockingEngine.OngoingContextualStubbing<T, R> {
        private final com.contextualmocker.core.ContextualMocker.OngoingContextualStubbing<T, R> delegate;
        OngoingStubbingAdapter(com.contextualmocker.core.ContextualMocker.OngoingContextualStubbing<T, R> d) { this.delegate = d; }
        @Override
        public MockingEngine.ContextSpecificStubbingInitiator<T> thenReturn(R value) {
            com.contextualmocker.core.ContextualMocker.ContextSpecificStubbingInitiator<T> next = delegate.thenReturn(value);
            return new ContextSpecificStubbingInitiatorAdapter<>(next);
        }
    }
}