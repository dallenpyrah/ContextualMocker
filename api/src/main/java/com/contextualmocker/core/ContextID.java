package com.contextualmocker.core;

/**
 * Public contract: identifies a logical invocation context.
 * <p>
 * Implementations <strong>must</strong> implement value-based {@code equals}/{@code hashCode}
 * because the engine relies on these instances as Map keys.</p>
 */
public interface ContextID {
}