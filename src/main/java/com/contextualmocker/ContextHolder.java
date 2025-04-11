package com.contextualmocker;

/**
 * Manages the active ContextID for the current thread.
 * This is used internally by the framework for explicit context passing via the API.
 */
final class ContextHolder {

    private static final ThreadLocal<ContextID> currentContext = new ThreadLocal<>();

    private ContextHolder() {
        // Utility class
    }

    static void setContext(ContextID contextId) {
        if (contextId == null) {
            currentContext.remove();
        } else {
            currentContext.set(contextId);
        }
    }

    static ContextID getContext() {
        ContextID context = currentContext.get();
        if (context == null) {
            // This should ideally not happen if the API is used correctly.
            // Consider throwing an exception or using a default context.
            // For now, let's throw an error indicating misuse.
            throw new IllegalStateException("No ContextID set for the current operation. " +
                    "Ensure forContext() was called before the mock interaction.");
        }
        return context;
    }

    static void clearContext() {
        currentContext.remove();
    }
}