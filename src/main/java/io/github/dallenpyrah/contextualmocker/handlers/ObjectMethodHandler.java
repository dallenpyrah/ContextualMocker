package io.github.dallenpyrah.contextualmocker.handlers;

import java.lang.reflect.Method;

/**
 * Strategy interface for handling Object methods (equals, hashCode, toString).
 * Implements Open/Closed Principle - open for extension, closed for modification.
 */
public interface ObjectMethodHandler {
    /**
     * Handles the invocation of an Object method.
     * 
     * @param proxy The proxy object
     * @param method The method being invoked
     * @param args The method arguments
     * @return The result of the method invocation
     */
    Object handle(Object proxy, Method method, Object[] args);
}
