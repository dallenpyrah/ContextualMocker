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
    @SuppressWarnings("unchecked")
    public static <T> T mock(Class<T> classToMock) {
        Objects.requireNonNull(classToMock, "Class to mock cannot be null");
        InvocationHandler handler = new ContextualInvocationHandler();

        if (classToMock.isInterface()) {
            // Interface mocking (existing logic)
            try {
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
        } else {
            // Class mocking
            if (java.lang.reflect.Modifier.isFinal(classToMock.getModifiers())) {
                throw new IllegalArgumentException("Cannot mock final classes: " + classToMock.getName());
            }
            // Check for final methods
            for (java.lang.reflect.Method m : classToMock.getDeclaredMethods()) {
                if (java.lang.reflect.Modifier.isFinal(m.getModifiers())) {
                    throw new IllegalArgumentException("Cannot mock classes with final methods: " + classToMock.getName() + ", method: " + m.getName());
                }
            }
            try {
                net.bytebuddy.dynamic.DynamicType.Builder<? extends T> builder = new ByteBuddy()
                        .subclass(classToMock)
                        .method(ElementMatchers.not(ElementMatchers.isFinal())
                                .and(ElementMatchers.not(ElementMatchers.isStatic()))
                                .and(ElementMatchers.not(ElementMatchers.isPrivate())))
                        .intercept(InvocationHandlerAdapter.of(handler));

                Class<? extends T> mockClass = builder
                        .make()
                        .load(classToMock.getClassLoader())
                        .getLoaded();

                // Try default constructor first
                try {
                    return mockClass.getDeclaredConstructor().newInstance();
                } catch (NoSuchMethodException e) {
                    // No default constructor, try to use the first available constructor with default values
                    java.lang.reflect.Constructor<?>[] ctors = mockClass.getDeclaredConstructors();
                    if (ctors.length == 0) {
                        throw new RuntimeException("No accessible constructor found for " + classToMock.getName());
                    }
                    java.lang.reflect.Constructor<?> ctor = ctors[0];
                    Class<?>[] paramTypes = ctor.getParameterTypes();
                    Object[] params = new Object[paramTypes.length];
                    for (int i = 0; i < paramTypes.length; i++) {
                        params[i] = getDefaultValue(paramTypes[i]);
                    }
                    return (T) ctor.newInstance(params);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to create mock for class " + classToMock.getName(), e);
            }
        }
    }

    // Helper for default values for constructor params
    private static Object getDefaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0d;
        if (type == char.class) return '\u0000';
        return null;
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
    @SuppressWarnings("unchecked")
    public static <T> T spy(T realObject) {
        Objects.requireNonNull(realObject, "Real object cannot be null");
        
        Class<T> classToSpy = (Class<T>) realObject.getClass();
        
        // Cannot spy on final classes
        if (java.lang.reflect.Modifier.isFinal(classToSpy.getModifiers())) {
            throw new IllegalArgumentException("Cannot spy on final classes: " + classToSpy.getName());
        }
        
        // Cannot spy on interfaces
        if (classToSpy.isInterface()) {
            throw new IllegalArgumentException("Cannot spy on interfaces, use mock() instead: " + classToSpy.getName());
        }

        InvocationHandler handler = new com.contextualmocker.handlers.SpyInvocationHandler(realObject);

        // Check for final methods that cannot be stubbed
        for (java.lang.reflect.Method m : classToSpy.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isFinal(m.getModifiers()) && 
                !java.lang.reflect.Modifier.isPrivate(m.getModifiers())) {
                // Note: We allow final methods but they cannot be stubbed
                // This is consistent with other mocking frameworks
            }
        }

        try {
            net.bytebuddy.dynamic.DynamicType.Builder<? extends T> builder = new ByteBuddy()
                    .subclass(classToSpy)
                    .method(ElementMatchers.not(ElementMatchers.isFinal())
                            .and(ElementMatchers.not(ElementMatchers.isStatic()))
                            .and(ElementMatchers.not(ElementMatchers.isPrivate())))
                    .intercept(InvocationHandlerAdapter.of(handler));

            Class<? extends T> spyClass = builder
                    .make()
                    .load(classToSpy.getClassLoader())
                    .getLoaded();

            // Try default constructor first
            try {
                return spyClass.getDeclaredConstructor().newInstance();
            } catch (NoSuchMethodException e) {
                // No default constructor, try to use the first available constructor with default values
                java.lang.reflect.Constructor<?>[] ctors = spyClass.getDeclaredConstructors();
                if (ctors.length == 0) {
                    throw new RuntimeException("No accessible constructor found for " + classToSpy.getName());
                }
                java.lang.reflect.Constructor<?> ctor = ctors[0];
                Class<?>[] paramTypes = ctor.getParameterTypes();
                Object[] params = new Object[paramTypes.length];
                for (int i = 0; i < paramTypes.length; i++) {
                    params[i] = getDefaultValue(paramTypes[i]);
                }
                return (T) ctor.newInstance(params);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create spy for class " + classToSpy.getName(), e);
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
        validateMockObject(mock, "given");
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
        validateMockObject(mock, "verify");
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

    private static boolean isMockObject(Object obj) {
        if (obj == null) {
            return false;
        }
        
        String className = obj.getClass().getName();
        return className.contains("ByteBuddy");
    }
    
    private static void validateMockObject(Object mock, String methodName) {
        if (!isMockObject(mock)) {
            throw new IllegalArgumentException(
                "Object passed to " + methodName + "() is not a mock created by ContextualMocker. " +
                "Only mocks created via ContextualMocker.mock() or ContextualMocker.spy() can be used."
            );
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
        T verify(ContextualVerificationMode mode);

        /**
         * Directly verifies a method call with the specified mode and method call.
         * This eliminates the need for a separate verify() call on the returned proxy.
         * 
         * @param mode The verification mode.
         * @param methodCall A lambda that calls the method to be verified.
         */
        void that(ContextualVerificationMode mode, java.util.function.Supplier<?> methodCall);
    }

    /**
     * Interface for different verification modes (times, atLeast, etc.).
     */
    public interface ContextualVerificationMode {
        /**
         * Verifies the count with enhanced error reporting.
         */
        default void verifyCount(int actual, java.lang.reflect.Method method, Object[] args) {
            verifyCountWithContext(actual, method, args, null, null, null);
        }
        
        /**
         * Verifies the count with full context for enhanced error messages.
         */
        void verifyCountWithContext(int actual, java.lang.reflect.Method method, Object[] args, 
                                  Object mock, ContextID contextId, java.util.List<InvocationRecord> allInvocations);
    }

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
        
        @Override
        public void verifyCountWithContext(int actual, java.lang.reflect.Method method, Object[] args, 
                                         Object mock, ContextID contextId, java.util.List<InvocationRecord> allInvocations) {
            if (actual != wanted) {
                throw new VerificationFailureException(
                    mock, contextId, method, args, wanted, actual, 
                    "exactly " + wanted + " time" + (wanted == 1 ? "" : "s"), allInvocations);
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
        
        @Override
        public void verifyCountWithContext(int actual, java.lang.reflect.Method method, Object[] args, 
                                         Object mock, ContextID contextId, java.util.List<InvocationRecord> allInvocations) {
            if (actual < min) {
                throw new VerificationFailureException(
                    mock, contextId, method, args, min, actual, 
                    "at least " + min + " time" + (min == 1 ? "" : "s"), allInvocations);
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
        
        @Override
        public void verifyCountWithContext(int actual, java.lang.reflect.Method method, Object[] args, 
                                         Object mock, ContextID contextId, java.util.List<InvocationRecord> allInvocations) {
            if (actual > max) {
                throw new VerificationFailureException(
                    mock, contextId, method, args, max, actual, 
                    "at most " + max + " time" + (max == 1 ? "" : "s"), allInvocations);
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