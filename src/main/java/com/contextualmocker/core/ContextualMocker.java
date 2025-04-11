package com.contextualmocker.core;
import com.contextualmocker.handlers.ContextualInvocationHandler;
import com.contextualmocker.handlers.ContextualAnswer;
import com.contextualmocker.initiators.ContextSpecificStubbingInitiatorImpl;
import com.contextualmocker.initiators.ContextSpecificVerificationInitiatorImpl;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.InvocationHandler;
import java.util.Objects;

public final class ContextualMocker {

    private ContextualMocker() {}

    public static ContextualVerificationMode times(int wantedNumberOfInvocations) {
        return new TimesVerificationMode(wantedNumberOfInvocations);
    }

    public static ContextualVerificationMode never() {
        return new TimesVerificationMode(0);
    }

    public static ContextualVerificationMode atLeastOnce() {
        return new AtLeastVerificationMode(1);
    }

    public static ContextualVerificationMode atLeast(int minInvocations) {
        return new AtLeastVerificationMode(minInvocations);
    }

    public static ContextualVerificationMode atMost(int maxInvocations) {
        return new AtMostVerificationMode(maxInvocations);
    }

    @SuppressWarnings("unchecked")
    public static <T> T mock(Class<T> classToMock) {
        Objects.requireNonNull(classToMock, "Class to mock cannot be null");
        if (!classToMock.isInterface()) {
            // V1 Scope: Only interfaces are supported initially
            throw new IllegalArgumentException("ContextualMocker v1.0 currently only supports mocking interfaces. Cannot mock: " + classToMock.getName());
            // Future: Extend to support classes using subclassing
        }

        try {
            // Create the invocation handler instance (assuming it exists)
            InvocationHandler handler = new ContextualInvocationHandler();

            // Use ByteBuddy to create a proxy
            return (T) new ByteBuddy()
                    .subclass(Object.class) // Subclass Object
                    .implement(classToMock) // Implement the target interface
                    .method(ElementMatchers.any()) // Intercept all methods
                    .intercept(InvocationHandlerAdapter.of(handler))
                    .make()
                    .load(classToMock.getClassLoader()) // Load in the same classloader
                    .getLoaded()
                    .getDeclaredConstructor() // Get default constructor
                    .newInstance(); // Create instance
        } catch (Exception e) {
            // Catch potential reflection/ByteBuddy exceptions
            throw new RuntimeException("Failed to create mock for " + classToMock.getName(), e);
        }
    }

    public static <T> ContextualStubbingInitiator<T> given(T mock) {
        Objects.requireNonNull(mock, "Mock object cannot be null");
        // TODO: Add check to ensure 'mock' is actually a mock created by this framework
        return new ContextualStubbingInitiatorImpl<>(mock);
    }

    public static <T> ContextualVerificationInitiator<T> verify(T mock) {
        Objects.requireNonNull(mock, "Mock object cannot be null");
        // TODO: Add check to ensure 'mock' is actually a mock created by this framework
        return new ContextualVerificationInitiatorImpl<>(mock);
    }


    // --- Inner classes for Fluent API ---
    // These will be implemented in separate files or later steps if they grow complex,
    // but placing placeholders here for now.

    // Stubbing Initiator
    public interface ContextualStubbingInitiator<T> {
        ContextSpecificStubbingInitiator<T> forContext(ContextID contextId);
    }

    // Context-Specific Stubbing
    public interface ContextSpecificStubbingInitiator<T> {
        <R> OngoingContextualStubbing<T, R> when(R methodCall); // R is the return type of the method call
    }

    // Ongoing Stubbing
    public interface OngoingContextualStubbing<T, R> {
        OngoingContextualStubbing<T, R> whenStateIs(Object state);
        OngoingContextualStubbing<T, R> willSetStateTo(Object newState);
        ContextSpecificStubbingInitiator<T> thenReturn(R value);
        ContextSpecificStubbingInitiator<T> thenThrow(Throwable throwable);
        ContextSpecificStubbingInitiator<T> thenAnswer(ContextualAnswer<R> answer);
    }

    // Verification Initiator
    public interface ContextualVerificationInitiator<T> {
        ContextSpecificVerificationInitiator<T> forContext(ContextID contextId);
        // Add verification modes directly? e.g., verify(mock, times(2)).forContext(ctx)...
        // Or keep it separate? Let's follow the design doc structure for now.
    }

    // Context-Specific Verification
    public interface ContextSpecificVerificationInitiator<T> {
        T verify(ContextualVerificationMode mode); // Returns the mock proxy to allow method call
    }

    // Verification Mode (Placeholder - needs more detail)
    public interface ContextualVerificationMode {}

    public static class TimesVerificationMode implements ContextualVerificationMode {
        private final int wanted;
        public TimesVerificationMode(int wanted) {
            this.wanted = wanted;
        }
        public void verifyCount(int actual, java.lang.reflect.Method method, Object[] args) {
            if (actual != wanted) {
                throw new AssertionError("Expected " + wanted + " invocations but got " + actual + " for " + method.getName());
            }
        }
    }

    public static class AtLeastVerificationMode implements ContextualVerificationMode {
        private final int min;
        public AtLeastVerificationMode(int min) {
            this.min = min;
        }
        public void verifyCount(int actual, java.lang.reflect.Method method, Object[] args) {
            if (actual < min) {
                throw new AssertionError("Expected at least " + min + " invocations but got " + actual + " for " + method.getName());
            }
        }
    }

    public static class AtMostVerificationMode implements ContextualVerificationMode {
        private final int max;

        public AtMostVerificationMode(int max) {
            this.max = max;
        }

        public void verifyCount(int actual, java.lang.reflect.Method method, Object[] args) {
            if (actual > max) {
                throw new AssertionError(
                        "Expected at most " + max + " invocations but got " + actual + " for " + method.getName());
            }
        }
    }
    
    private static class ContextualStubbingInitiatorImpl<T> implements ContextualStubbingInitiator<T> {
        private final T mock;

        ContextualStubbingInitiatorImpl(T mock) {
            this.mock = mock;
        }

        @Override
        public ContextSpecificStubbingInitiator<T> forContext(ContextID contextId) {
            Objects.requireNonNull(contextId, "ContextID cannot be null for stubbing");
            ContextHolder.setContext(contextId); // Set context for the subsequent 'when' call
            return new ContextSpecificStubbingInitiatorImpl<>(mock);
        }
    }

    private static class ContextualVerificationInitiatorImpl<T> implements ContextualVerificationInitiator<T> {
        private final T mock;

        ContextualVerificationInitiatorImpl(T mock) {
            this.mock = mock;
        }

        @Override
        public ContextSpecificVerificationInitiator<T> forContext(ContextID contextId) {
            Objects.requireNonNull(contextId, "ContextID cannot be null for verification");
            ContextHolder.setContext(contextId);
            return new ContextSpecificVerificationInitiatorImpl<T>(mock);
        }
    }

    // --- More implementation classes needed for the full fluent API ---
    // ContextSpecificStubbingInitiatorImpl, OngoingContextualStubbingImpl,
    // ContextSpecificVerificationInitiatorImpl, etc.
    // These will be created in the next steps.

}