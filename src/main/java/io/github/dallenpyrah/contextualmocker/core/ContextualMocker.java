package io.github.dallenpyrah.contextualmocker.core;
import io.github.dallenpyrah.contextualmocker.api.*;
import io.github.dallenpyrah.contextualmocker.initiators.ContextSpecificStubbingInitiatorImpl;
import io.github.dallenpyrah.contextualmocker.initiators.ContextSpecificVerificationInitiatorImpl;

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
        return VerificationModes.times(wantedNumberOfInvocations);
    }

    /**
     * Creates a verification mode that expects zero invocations.
     *
     * @return A verification mode.
     */
    public static ContextualVerificationMode never() {
        return VerificationModes.never();
    }

    /**
     * Creates a verification mode that expects at least one invocation.
     *
     * @return A verification mode.
     */
    public static ContextualVerificationMode atLeastOnce() {
        return VerificationModes.atLeastOnce();
    }

    /**
     * Creates a verification mode that expects at least a minimum number of invocations.
     *
     * @param minInvocations The minimum number of invocations expected.
     * @return A verification mode.
     */
    public static ContextualVerificationMode atLeast(int minInvocations) {
        return VerificationModes.atLeast(minInvocations);
    }

    /**
     * Creates a verification mode that expects at most a maximum number of invocations.
     *
     * @param maxInvocations The maximum number of invocations allowed.
     * @return A verification mode.
     */
    public static ContextualVerificationMode atMost(int maxInvocations) {
        return VerificationModes.atMost(maxInvocations);
    }

    /**
     * Creates a mock instance of the given class or interface.
     * <p>
     * Supports mocking of interfaces and non-final, non-abstract classes.
     * <p>
     * Limitations:
     * <ul>
     *   <li>Cannot mock final classes or classes with final methods.</li>
     *   <li>All constructors must be accessible. If no default constructor is present, the first available constructor will be used with default values for parameters.</li>
     *   <li>Static methods are not mocked.</li>
     * </ul>
     *
     * @param <T> The type of the class/interface to mock.
     * @param classToMock The class or interface to mock.
     * @return A mock instance of the specified type.
     * @throws IllegalArgumentException If mocking the given type is not supported.
     * @throws RuntimeException If mock creation fails.
     */
    public static <T> T mock(Class<T> classToMock) {
        return MockFactory.createMock(classToMock);
    }

    /**
     * Creates a spy object that wraps a real instance, allowing selective stubbing
     * while delegating unstubbed methods to the real implementation.
     * 
     * <p>Spies are useful for partial mocking scenarios where you want to:
     * <ul>
     *   <li>Stub only specific methods while keeping real behavior for others</li>
     *   <li>Verify interactions on real objects</li>
     *   <li>Test legacy code that's difficult to mock completely</li>
     * </ul>
     * 
     * <p>Example usage:
     * <pre>{@code
     * UserService realService = new UserServiceImpl();
     * UserService spy = spy(realService);
     * 
     * // Stub specific methods
     * given(spy).forContext(ctx).when(() -> spy.externalCall()).thenReturn("stubbed");
     * 
     * // Real methods still work
     * String result = spy.processData("input"); // Calls real implementation
     * }</pre>
     * 
     * <p><strong>Limitations:</strong>
     * <ul>
     *   <li>Cannot spy on final classes or interfaces</li>
     *   <li>Cannot spy on final methods</li>
     *   <li>Private methods cannot be stubbed</li>
     *   <li>Constructor calls during spying may have side effects</li>
     * </ul>
     *
     * @param <T> The type of the object to spy on.
     * @param realObject The real object instance to wrap.
     * @return A spy that delegates to the real object but allows selective stubbing.
     * @throws IllegalArgumentException If spying on the given object is not supported.
     * @throws NullPointerException If realObject is null.
     * @throws RuntimeException If spy creation fails.
     */
    public static <T> T spy(T realObject) {
        return MockFactory.createSpy(realObject);
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
        if (!isMock(mock)) {
            throw new IllegalArgumentException("Object is not a mock created by ContextualMocker: " + mock.getClass().getName());
        }
        return new ContextualStubbingInitiatorImpl<>(mock);
    }

    /**
     * Direct stubbing method that bypasses the fluent chain for simpler usage.
     * 
     * @param <T> The type of the mock object.
     * @param <R> The return type of the method being stubbed.
     * @param mock The mock object whose behavior will be stubbed.
     * @param context The context for this stubbing rule.
     * @param methodCall A lambda that calls the method to be stubbed.
     * @return An ongoing stubbing definition to specify behavior.
     * @throws NullPointerException if mock, context, or methodCall is null.
     */
    public static <T, R> OngoingContextualStubbing<T, R> when(T mock, ContextID context, java.util.function.Supplier<R> methodCall) {
        Objects.requireNonNull(mock, "Mock object cannot be null");
        Objects.requireNonNull(context, "ContextID cannot be null");
        Objects.requireNonNull(methodCall, "Method call supplier cannot be null");
        if (!isMock(mock)) {
            throw new IllegalArgumentException("Object is not a mock created by ContextualMocker: " + mock.getClass().getName());
        }
        
        ContextHolder.setContext(context);
        try {
            return new ContextSpecificStubbingInitiatorImpl<>(mock).when(methodCall);
        } finally {
            // Don't clear context here as the OngoingStubbing needs it
        }
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
        if (!isMock(mock)) {
            throw new IllegalArgumentException("Object is not a mock created by ContextualMocker: " + mock.getClass().getName());
        }
        return new ContextualVerificationInitiatorImpl<>(mock);
    }

    /**
     * Direct verification method that bypasses the fluent chain for simpler usage.
     * 
     * @param <T> The type of the mock object.
     * @param mock The mock object whose interactions will be verified.
     * @param context The context for this verification.
     * @param mode The verification mode (times, never, etc.).
     * @param methodCall A lambda that calls the method to be verified.
     * @throws NullPointerException if any parameter is null.
     * @throws AssertionError if verification fails.
     */
    public static <T> void verify(T mock, ContextID context, ContextualVerificationMode mode, java.util.function.Supplier<?> methodCall) {
        Objects.requireNonNull(mock, "Mock object cannot be null");
        Objects.requireNonNull(context, "ContextID cannot be null");
        Objects.requireNonNull(mode, "Verification mode cannot be null");
        Objects.requireNonNull(methodCall, "Method call supplier cannot be null");
        if (!isMock(mock)) {
            throw new IllegalArgumentException("Object is not a mock created by ContextualMocker: " + mock.getClass().getName());
        }
        
        ContextHolder.setContext(context);
        try {
            T verificationProxy = new ContextSpecificVerificationInitiatorImpl<>(mock).verify(mode);
            methodCall.get();
        } finally {
            ContextHolder.clearContext();
        }
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
        if (!isMock(mock)) {
            throw new IllegalArgumentException("Object is not a mock created by ContextualMocker: " + mock.getClass().getName());
        }

        List<InvocationRecord> unverifiedInvocations = MockRegistry.getInvocationRecords(mock, contextId)
                .stream()
                .filter(record -> !record.isVerified())
                .collect(Collectors.toList());

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
        if (!isMock(mock)) {
            throw new IllegalArgumentException("Object is not a mock created by ContextualMocker: " + mock.getClass().getName());
        }

        List<InvocationRecord> invocations = MockRegistry.getInvocationRecords(mock, contextId);
        
        if (!invocations.isEmpty()) {
            String details = invocations.stream()
                    .map(InvocationRecord::toString)
                    .collect(Collectors.joining("\n  ", "\n  ", ""));
             throw new AssertionError("Found interactions for context [" + contextId + "] when none were expected:" + details);
        }
    }

    /**
     * Creates a context-aware builder for performing multiple operations in the same context.
     * 
     * @param contextId The context identifier.
     * @return A ContextualMockBuilder for the given context.
     * @throws NullPointerException if contextId is null.
     */
    public static ContextualMockBuilder withContext(ContextID contextId) {
        return ContextualMockBuilder.withContext(contextId);
    }

    /**
     * Creates a scoped context that automatically manages context setup and cleanup.
     * Use in try-with-resources blocks for automatic context management.
     * 
     * @param contextId The context identifier.
     * @return A ContextScope for the given context.
     * @throws NullPointerException if contextId is null.
     */
    public static ContextScope scopedContext(ContextID contextId) {
        return ContextScope.withContext(contextId);
    }

    /**
     * Convenience method for stubbing with times(1) verification.
     * Equivalent to verify(mock, context, times(1), methodCall).
     * 
     * @param <T> The type of the mock object.
     * @param mock The mock object whose interactions will be verified.
     * @param context The context for this verification.
     * @param methodCall A lambda that calls the method to be verified.
     */
    public static <T> void verifyOnce(T mock, ContextID context, java.util.function.Supplier<?> methodCall) {
        verify(mock, context, times(1), methodCall);
    }

    /**
     * Convenience method for stubbing with never() verification.
     * Equivalent to verify(mock, context, never(), methodCall).
     * 
     * @param <T> The type of the mock object.
     * @param mock The mock object whose interactions will be verified.
     * @param context The context for this verification.
     * @param methodCall A lambda that calls the method to be verified.
     */
    public static <T> void verifyNever(T mock, ContextID context, java.util.function.Supplier<?> methodCall) {
        verify(mock, context, never(), methodCall);
    }
    
    /**
     * Checks if the given object is a mock created by this framework.
     * 
     * @param object The object to check.
     * @return true if the object is a mock created by ContextualMocker, false otherwise.
     */
    public static boolean isMock(Object object) {
        if (object == null) {
            return false;
        }
        
        // Check if the object implements our marker interface
        return object instanceof ContextualMockerMarker;
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