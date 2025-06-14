package io.github.dallenpyrah.contextualmocker.handlers;

import java.lang.reflect.Method;

/**
 * Handler for the toString() method.
 * Part of the strategy pattern for Object method handling.
 */
public class ToStringHandler implements ObjectMethodHandler {
    @Override
    public Object handle(Object proxy, Method method, Object[] args) {
        Class<?>[] interfaces = proxy.getClass().getInterfaces();
        String typeName;
        if (interfaces.length > 0) {
            typeName = interfaces[0].getSimpleName();
        } else {
            typeName = proxy.getClass().getSuperclass() != null
                ? proxy.getClass().getSuperclass().getSimpleName()
                : proxy.getClass().getSimpleName();
        }
        return "ContextualMock<" + typeName + ">@" + Integer.toHexString(System.identityHashCode(proxy));
    }
}
