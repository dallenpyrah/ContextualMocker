package io.github.dallenpyrah.contextualmocker.handlers;

import java.lang.reflect.Method;

/**
 * Handler for the equals() method.
 * Part of the strategy pattern for Object method handling.
 */
public class EqualsHandler implements ObjectMethodHandler {
    @Override
    public Object handle(Object proxy, Method method, Object[] args) {
        return proxy == args[0];
    }
}
