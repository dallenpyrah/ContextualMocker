package io.github.dallenpyrah.contextualmocker.core;

import io.github.dallenpyrah.contextualmocker.handlers.ContextualInvocationHandler;
import io.github.dallenpyrah.contextualmocker.handlers.SpyInvocationHandler;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.InvocationHandler;
import java.util.Objects;

/**
 * Factory for creating mock and spy instances.
 * Extracted from ContextualMocker to follow Single Responsibility Principle.
 */
public final class MockFactory {
    
    private MockFactory() {}
    
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
    public static <T> T createMock(Class<T> classToMock) {
        Objects.requireNonNull(classToMock, "Class to mock cannot be null");
        InvocationHandler handler = new ContextualInvocationHandler();

        if (classToMock.isInterface()) {
            return createInterfaceMock(classToMock, handler);
        } else {
            return createClassMock(classToMock, handler);
        }
    }
    
    /**
     * Creates a spy object that wraps a real instance, allowing selective stubbing
     * while delegating unstubbed methods to the real implementation.
     * 
     * @param <T> The type of the object to spy on.
     * @param realObject The real object instance to wrap.
     * @return A spy that delegates to the real object but allows selective stubbing.
     * @throws IllegalArgumentException If spying on the given object is not supported.
     * @throws NullPointerException If realObject is null.
     * @throws RuntimeException If spy creation fails.
     */
    @SuppressWarnings("unchecked")
    public static <T> T createSpy(T realObject) {
        Objects.requireNonNull(realObject, "Real object cannot be null");
        
        Class<T> classToSpy = (Class<T>) realObject.getClass();
        
        validateSpyable(classToSpy);
        
        InvocationHandler handler = new SpyInvocationHandler(realObject);
        
        try {
            return createSpyInstance(classToSpy, handler);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create spy for class " + classToSpy.getName(), e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private static <T> T createInterfaceMock(Class<T> classToMock, InvocationHandler handler) {
        try {
            return (T) new ByteBuddy()
                    .subclass(Object.class)
                    .implement(classToMock)
                    .implement(ContextualMockerMarker.class)
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
    
    @SuppressWarnings("unchecked")
    private static <T> T createClassMock(Class<T> classToMock, InvocationHandler handler) {
        validateMockable(classToMock);
        
        try {
            net.bytebuddy.dynamic.DynamicType.Builder<? extends T> builder = new ByteBuddy()
                    .subclass(classToMock)
                    .implement(ContextualMockerMarker.class)
                    .method(ElementMatchers.not(ElementMatchers.isFinal())
                            .and(ElementMatchers.not(ElementMatchers.isStatic()))
                            .and(ElementMatchers.not(ElementMatchers.isPrivate())))
                    .intercept(InvocationHandlerAdapter.of(handler));

            Class<? extends T> mockClass = builder
                    .make()
                    .load(classToMock.getClassLoader())
                    .getLoaded();

            return instantiateMockClass(mockClass, classToMock);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mock for class " + classToMock.getName(), e);
        }
    }
    
    private static void validateMockable(Class<?> classToMock) {
        if (java.lang.reflect.Modifier.isFinal(classToMock.getModifiers())) {
            throw new IllegalArgumentException("Cannot mock final classes: " + classToMock.getName());
        }
        
        // Check for final methods
        for (java.lang.reflect.Method m : classToMock.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isFinal(m.getModifiers())) {
                throw new IllegalArgumentException("Cannot mock classes with final methods: " + 
                    classToMock.getName() + ", method: " + m.getName());
            }
        }
    }
    
    private static void validateSpyable(Class<?> classToSpy) {
        if (java.lang.reflect.Modifier.isFinal(classToSpy.getModifiers())) {
            throw new IllegalArgumentException("Cannot spy on final classes: " + classToSpy.getName());
        }
        
        if (classToSpy.isInterface()) {
            throw new IllegalArgumentException("Cannot spy on interfaces, use mock() instead: " + classToSpy.getName());
        }
    }
    
    @SuppressWarnings("unchecked")
    private static <T> T instantiateMockClass(Class<? extends T> mockClass, Class<T> originalClass) throws Exception {
        // Try default constructor first
        try {
            return mockClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            // No default constructor, try to use the first available constructor with default values
            java.lang.reflect.Constructor<?>[] ctors = mockClass.getDeclaredConstructors();
            if (ctors.length == 0) {
                throw new RuntimeException("No accessible constructor found for " + originalClass.getName());
            }
            java.lang.reflect.Constructor<?> ctor = ctors[0];
            Class<?>[] paramTypes = ctor.getParameterTypes();
            Object[] params = new Object[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                params[i] = DefaultValueProvider.getDefaultValue(paramTypes[i]);
            }
            return (T) ctor.newInstance(params);
        }
    }
    
    @SuppressWarnings("unchecked")
    private static <T> T createSpyInstance(Class<T> classToSpy, InvocationHandler handler) throws Exception {
        net.bytebuddy.dynamic.DynamicType.Builder<? extends T> builder = new ByteBuddy()
                .subclass(classToSpy)
                .implement(ContextualMockerMarker.class)
                .method(ElementMatchers.not(ElementMatchers.isFinal())
                        .and(ElementMatchers.not(ElementMatchers.isStatic()))
                        .and(ElementMatchers.not(ElementMatchers.isPrivate())))
                .intercept(InvocationHandlerAdapter.of(handler));

        Class<? extends T> spyClass = builder
                .make()
                .load(classToSpy.getClassLoader())
                .getLoaded();

        return instantiateMockClass(spyClass, classToSpy);
    }
}
