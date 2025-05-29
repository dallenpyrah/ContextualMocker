package com.contextualmocker.core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the active ContextID for the current thread.
 * This is used internally by the framework for explicit context passing via the API.
 */
public final class ContextHolder {
   private static final Logger logger = LoggerFactory.getLogger(ContextHolder.class);

    private static final InheritableThreadLocal<ContextID> currentContext = new InheritableThreadLocal<>();

    private ContextHolder() {
    }

    public static void setContext(ContextID contextId) {
        if (contextId == null) {
            clearContext();
        } else {
            currentContext.set(contextId);
            if (logger.isDebugEnabled()) {
                logger.debug("Set context for thread {}: {}", Thread.currentThread().getId(), contextId);
            }
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

    public static ContextID getCurrentContextIfSet() {
        return currentContext.get();
    }

    public static void clearContext() {
        ContextID contextToClear = currentContext.get();
        if (contextToClear != null) {
            // Clear associated invocation records *before* removing from ThreadLocal
            MockRegistry.clearInvocationsForContext(contextToClear);
            currentContext.remove();
            if (logger.isDebugEnabled()) {
                logger.debug("Cleared context and associated invocations for thread {}: {}", Thread.currentThread().getId(), contextToClear);
            }
        }
    }
}
