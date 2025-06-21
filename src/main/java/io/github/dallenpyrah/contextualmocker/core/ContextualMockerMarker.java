package io.github.dallenpyrah.contextualmocker.core;

/**
 * Marker interface implemented by all mocks and spies created by ContextualMocker.
 * This interface is used internally to identify objects that were created by this framework,
 * distinguishing them from mocks created by other frameworks that might also use ByteBuddy.
 * 
 * <p>This interface has no methods and serves only as a type marker.</p>
 * 
 * @since 1.1.0
 */
public interface ContextualMockerMarker {
    // Marker interface - no methods required
}