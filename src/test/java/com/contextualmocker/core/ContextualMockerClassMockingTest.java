package com.contextualmocker.core;

import org.junit.jupiter.api.Test;

import static com.contextualmocker.core.ContextualMocker.*;
import static org.junit.jupiter.api.Assertions.*;

class ContextualMockerClassMockingTest {

    @Test
    void canMockConcreteClassWithDefaultConstructor() {
        SimpleServiceImpl mock = mock(SimpleServiceImpl.class);
        given(mock).forContext(new StringContextId("CTX")).when(() -> mock.greet("Alice")).thenReturn("Hi Alice");
        ContextHolder.setContext(new StringContextId("CTX"));
        assertEquals("Hi Alice", mock.greet("Alice"));
        // Default behavior for unstubbed method
        assertNull(mock.getList(5));
    }

    @Test
    void canMockClassWithNoDefaultConstructor() {
        ClassWithNoDefaultConstructor mock = mock(ClassWithNoDefaultConstructor.class);
        given(mock).forContext(new StringContextId("CTX")).when(() -> mock.greet("Bob")).thenReturn("Hello Bob");
        ContextHolder.setContext(new StringContextId("CTX"));
        assertEquals("Hello Bob", mock.greet("Bob"));
        // Default behavior for unstubbed method
        assertNull(mock.getValue());
    }

    @Test
    void toStringWorksForClassAndInterfaceMocks() {
        SimpleServiceImpl classMock = mock(SimpleServiceImpl.class);
        SimpleService interfaceMock = mock(SimpleService.class);
        assertNotNull(classMock.toString());
        assertNotNull(interfaceMock.toString());
        assertTrue(classMock.toString().contains("Mock") || classMock.toString().contains("mock"));
        assertTrue(interfaceMock.toString().contains("Mock") || interfaceMock.toString().contains("mock"));
    }

    // NOTE: Context-aware stubbing and verification for class mocks is not reliably supported
    // due to proxying limitations. This is covered for interfaces in other tests.

    @Test
    void cannotMockFinalClass() {
        assertThrows(IllegalArgumentException.class, () -> mock(FinalClass.class));
    }

    @Test
    void cannotMockClassWithFinalMethod() {
        assertThrows(IllegalArgumentException.class, () -> mock(ClassWithFinalMethod.class));
    }

    @Test
    void staticMethodsAreNotMocked() {
        ClassWithStaticMethod mock = mock(ClassWithStaticMethod.class);
        // Instance method can be stubbed
        given(mock).forContext(new StringContextId("CTX")).when(() -> mock.sayHello()).thenReturn("Mocked");
        ContextHolder.setContext(new StringContextId("CTX"));
        assertEquals("Mocked", mock.sayHello());
        // Static method is not mocked
        assertEquals("Static", ClassWithStaticMethod.staticMethod());
    }
}