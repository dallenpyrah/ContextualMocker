package com.contextualmocker.core;

/**
 * Manages the active ContextID for the current thread.
 * This is used internally by the framework for explicit context passing via the API.
 */
public final class ContextHolder {

    private static final ThreadLocal<ContextID> currentContext = new ThreadLocal<>();

    private ContextHolder() {
        // Utility class
    }

    public static void setContext(ContextID contextId) {
        if (contextId == null) {
            currentContext.remove();
        } else {
            currentContext.set(contextId);
        }
    }

    public static ContextID getContext() {
        ContextID context = currentContext.get();
        if (context == null) {
            throw new IllegalStateException("No ContextID set for the current operation. " +
                    "Ensure forContext() was called before the mock interaction.");
        }
        return context;
    }

    static void clearContext() {
        currentContext.remove();
    }
}