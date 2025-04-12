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
import java.util.List;
import java.util.stream.Collectors;

/**
 * The main entry point for the ContextualMocker framework.
 * Provides static methods for creating mocks, setting up stubbing rules,
 * and verifying interactions in a context-aware and parallel-safe manner.
 */
public final class ContextualMocker {

    private ContextualMocker() {}

    /**
     * Creates a verification mode that expects an exact number of invocations.
     *
     * @param wantedNumberOfInvocations The exact number of invocations expected.
     * @return A verification mode.
     */
    public static ContextualVerificationMode times(int wantedNumberOfInvocations) {
        return new TimesVerificationMode(wantedNumberOfInvocations);
    }

    /**
     * Creates a verification mode that expects zero invocations.
     *
     * @return A verification mode.
     */
    public static ContextualVerificationMode never() {
        return new TimesVerificationMode(0);
    }

    /**
     * Creates a verification mode that expects at least one invocation.
     *
     * @return A verification mode.
     */
    public static ContextualVerificationMode atLeastOnce() {
        return new AtLeastVerificationMode(1);
    }

    /**
     * Creates a verification mode that expects at least a minimum number of invocations.
     *
     * @param minInvocations The minimum number of invocations expected.
     * @return A verification mode.
     */
    public static ContextualVerificationMode atLeast(int minInvocations) {
        return new AtLeastVerificationMode(minInvocations);
    }

    /**
     * Creates a verification mode that expects at most a maximum number of invocations.
     *
     * @param maxInvocations The maximum number of invocations allowed.
     * @return A verification mode.
     */
    public static ContextualVerificationMode atMost(int maxInvocations) {
        return new AtMostVerificationMode(maxInvocations);
    }

    /**
     * Creates a mock instance of the given class or interface.
     * <p>
     * Currently, only interfaces are fully supported.
     *
     * @param <T> The type of the class/interface to mock.
     * @param classToMock The class or interface to mock.
     * @return A mock instance of the specified type.
     * @throws IllegalArgumentException If mocking the given type is not supported.
     * @throws RuntimeException If mock creation fails.
     */
    @SuppressWarnings("unchecked")
    public static <T> T mock(Class<T> classToMock) {
        Objects.requireNonNull(classToMock, "Class to mock cannot be null");
        if (!classToMock.isInterface()) {
            throw new IllegalArgumentException("ContextualMocker v1.0 currently only supports mocking interfaces. Cannot mock: " + classToMock.getName());
        }

        try {
            InvocationHandler handler = new ContextualInvocationHandler();

            return (T) new ByteBuddy()
                    .subclass(Object.class)
                    .implement(classToMock)
                    .method(ElementMatchers.any())
                    .intercept(InvocationHandlerAdapter.of(handler))
                    .make()
                    .load(classToMock.getClassLoader())
                    .getLoaded()
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mock for " + classToMock.getName(), e);
        }
    }

    /**
     * Initiates the stubbing process for a given mock object.
     *
     * @param <T> The type of the mock object.
     * @param mock The mock object whose behavior will be stubbed.
     * @return A stubbing initiator to specify the context and method call.
     * @throws NullPointerException if the mock is null.
     */
    public static <T> ContextualStubbingInitiator<T> given(T mock) {
        Objects.requireNonNull(mock, "Mock object cannot be null");
        // TODO: Add check to ensure 'mock' is actually a mock created by this framework
        return new ContextualStubbingInitiatorImpl<>(mock);
    }

    /**
     * Initiates the verification process for a given mock object.
     *
     * @param <T> The type of the mock object.
     * @param mock The mock object whose interactions will be verified.
     * @return A verification initiator to specify the context and verification mode.
     * @throws NullPointerException if the mock is null.
     */
    public static <T> ContextualVerificationInitiator<T> verify(T mock) {
        Objects.requireNonNull(mock, "Mock object cannot be null");
        // TODO: Add check to ensure 'mock' is actually a mock created by this framework
        return new ContextualVerificationInitiatorImpl<>(mock);
    }

    /**
     * Verifies that no interactions occurred on the given mock within the specified context
     * beyond those already verified.
     *
     * @param <T> The type of the mock object.
     * @param mock The mock object to check.
     * @param contextId The context within which to check for interactions.
     * @throws NullPointerException if mock or contextId is null.
     * @throws AssertionError if unverified interactions are found.
     */
    public static <T> void verifyNoMoreInteractions(T mock, ContextID contextId) {
        Objects.requireNonNull(mock, "Mock object cannot be null");
        Objects.requireNonNull(contextId, "ContextID cannot be null for verification");

        List<InvocationRecord> unverifiedInvocations = MockRegistry.getInvocationRecords(mock, contextId)
                .stream()
                .filter(record -> !record.isVerified())
                .toList();

        if (!unverifiedInvocations.isEmpty()) {
            String details = unverifiedInvocations.stream()
                    .map(InvocationRecord::toString)
                    .collect(Collectors.joining("\n  ", "\n  ", ""));
            throw new AssertionError("Found unverified interactions for context [" + contextId + "]:" + details);
        }
    }

    /**
     * Verifies that no interactions occurred at all on the given mock within the specified context.
     *
     * @param <T> The type of the mock object.
     * @param mock The mock object to check.
     * @param contextId The context within which to check for interactions.
     * @throws NullPointerException if mock or contextId is null.
     * @throws AssertionError if any interactions are found.
     */
    public static <T> void verifyNoInteractions(T mock, ContextID contextId) {
        Objects.requireNonNull(mock, "Mock object cannot be null");
        Objects.requireNonNull(contextId, "ContextID cannot be null for verification");

        List<InvocationRecord> invocations = MockRegistry.getInvocationRecords(mock, contextId);
        
        if (!invocations.isEmpty()) {
            String details = invocations.stream()
                    .map(InvocationRecord::toString)
                    .collect(Collectors.joining("\n  ", "\n  ", ""));
             throw new AssertionError("Found interactions for context [" + contextId + "] when none were expected:" + details);
        }
    }



    /**
     * Initiates stubbing for a specific mock, requiring context next.
     * @param <T> Mock type.
     */
    public interface ContextualStubbingInitiator<T> {
        /**
         * Specifies the context for the stubbing rule.
         * @param contextId The context identifier.
         * @return A context-specific stubbing initiator.
         */
        ContextSpecificStubbingInitiator<T> forContext(ContextID contextId);
    }

    /**
     * Initiates stubbing for a specific method call within a context.
     * @param <T> Mock type.
     */
    public interface ContextSpecificStubbingInitiator<T> {
        /**
         * Specifies the method call to be stubbed.
         * The method should be called on the mock object passed to this method.
         * Example: {@code .when(() -> mock.someMethod(arg1, arg2))}
         * @param <R> The return type of the method being stubbed.
         * @param methodCallSupplier A lambda or method reference that calls the method to be stubbed.
         * @return An ongoing stubbing definition.
         */
        <R> OngoingContextualStubbing<T, R> when(java.util.function.Supplier<R> methodCallSupplier);
    }

    /**
     * Defines the behavior (return value, exception, answer, state changes) for a stubbed method call.
     * @param <T> Mock type.
     * @param <R> Return type of the stubbed method.
     */
    public interface OngoingContextualStubbing<T, R> {
        /**
         * Specifies a state condition for this stubbing rule.
         * The rule will only apply if the mock is in the given state for the current context.
         * @param state The required state.
         * @return This ongoing stubbing definition.
         */
        OngoingContextualStubbing<T, R> whenStateIs(Object state);
        /**
         * Specifies a state transition to occur after this stubbed method is invoked.
         * The mock's state for the current context will be updated to the new state.
         * @param newState The target state.
         * @return This ongoing stubbing definition.
         */
        OngoingContextualStubbing<T, R> willSetStateTo(Object newState);
        /**
         * Sets the time-to-live (TTL) for this specific stubbing rule.
         * If the rule is not matched within this duration (in milliseconds) after creation,
         * it will be automatically removed. A TTL of 0 or less means the rule never expires.
         * @param ttlMillis The time-to-live in milliseconds.
         * @return This ongoing stubbing definition, allowing chaining before thenReturn/thenThrow/thenAnswer.
         */
        OngoingContextualStubbing<T, R> ttlMillis(long ttlMillis);
        /**
         * Specifies the value to return when the stubbed method is called.
         * @param value The value to return.
         * @return A context-specific stubbing initiator for defining further rules.
         */
        ContextSpecificStubbingInitiator<T> thenReturn(R value);
        /**
         * Specifies the throwable to be thrown when the stubbed method is called.
         * @param throwable The throwable instance to throw.
         * @return A context-specific stubbing initiator for defining further rules.
         */
        ContextSpecificStubbingInitiator<T> thenThrow(Throwable throwable);
        /**
         * Specifies a custom answer (dynamic behavior) when the stubbed method is called.
         * @param answer The custom answer implementation.
         * @return A context-specific stubbing initiator for defining further rules.
         */
        ContextSpecificStubbingInitiator<T> thenAnswer(ContextualAnswer<R> answer);
    }

    /**
     * Initiates verification for a specific mock, requiring context next.
     * @param <T> Mock type.
     */
    public interface ContextualVerificationInitiator<T> {
        /**
         * Specifies the context for the verification.
         * @param contextId The context identifier.
         * @return A context-specific verification initiator.
         */
        ContextSpecificVerificationInitiator<T> forContext(ContextID contextId);
        // Add verification modes directly? e.g., verify(mock, times(2)).forContext(ctx)...
        // Or keep it separate? Let's follow the design doc structure for now.
    }

    /**
     * Initiates verification for a specific method call within a context, using a verification mode.
     * @param <T> Mock type.
     */
    public interface ContextSpecificVerificationInitiator<T> {
        /**
         * Specifies the verification mode (e.g., times(1), never()) and prepares for the method call.
         * @param mode The verification mode.
         * @return The mock object proxy; call the method to be verified on this proxy.
         */
        T verify(ContextualVerificationMode mode); // Returns the mock proxy to allow method call
    }

    /**
     * Marker interface for different verification modes (times, atLeast, etc.).
     */
    public interface ContextualVerificationMode {}

    /**
     * Verification mode specifying an exact number of invocations.
     */
    public static class TimesVerificationMode implements ContextualVerificationMode {
        private final int wanted;
        public TimesVerificationMode(int wanted) {
            this.wanted = wanted;
        }
        public void verifyCount(int actual, java.lang.reflect.Method method, Object[] args) {
            if (actual != wanted) {
                throw new AssertionError("Wanted " + wanted + " invocations but got " + actual + " for method '" + method.getName() + "' with arguments " + java.util.Arrays.toString(args));
            }
        }
    }

    /**
     * Verification mode specifying a minimum number of invocations.
     */
    public static class AtLeastVerificationMode implements ContextualVerificationMode {
        private final int min;
        public AtLeastVerificationMode(int min) {
            this.min = min;
        }
        public void verifyCount(int actual, java.lang.reflect.Method method, Object[] args) {
            if (actual < min) {
                throw new AssertionError("Wanted at least " + min + " invocations but got only " + actual + " for method '" + method.getName() + "' with arguments " + java.util.Arrays.toString(args));
            }
        }
    }

    /**
     * Verification mode specifying a maximum number of invocations.
     */
    public static class AtMostVerificationMode implements ContextualVerificationMode {
        private final int max;

        public AtMostVerificationMode(int max) {
            this.max = max;
        }

        public void verifyCount(int actual, java.lang.reflect.Method method, Object[] args) {
            if (actual > max) {
                throw new AssertionError(
                        "Wanted at most " + max + " invocations but got " + actual + " for method '" + method.getName() + "' with arguments " + java.util.Arrays.toString(args));
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

}