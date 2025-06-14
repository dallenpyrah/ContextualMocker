package io.github.dallenpyrah.contextualmocker.handlers;

import java.lang.reflect.Method;

/**
 * Handler for the hashCode() method.
 * Part of the strategy pattern for Object method handling.
 */
public class HashCodeHandler implements ObjectMethodHandler {
    @Override
    public Object handle(Object proxy, Method method, Object[] args) {
        return System.identityHashCode(proxy);
    }
}
