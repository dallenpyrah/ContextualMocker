package com.contextualmocker.api;

import java.util.ServiceLoader;

/**
 * User-facing static entry points.  Equivalent to the legacy {@code ContextualMocker} class
 * but isolates callers from the actual engine implementation.
 */
public final class Contextual {

    private static final MockingEngine ENGINE = ServiceLoader.load(MockingEngine.class)
                                                          .findFirst()
                                                          .orElseThrow(() -> new IllegalStateException("No MockingEngine implementation found on class-path"));

    private Contextual() {}

    public static <T> T mock(Class<T> type) {
        return ENGINE.mock(type);
    }

    public static <T> T spy(T real) {
        return ENGINE.spy(real);
    }

    public static MockingEngine.ContextScope scopedContext(com.contextualmocker.core.ContextID id) {
        return ENGINE.scopedContext(id);
    }

    public static <T> MockingEngine.ContextualStubbingInitiator<T> given(T mock) {
        return ENGINE.given(mock);
    }
}