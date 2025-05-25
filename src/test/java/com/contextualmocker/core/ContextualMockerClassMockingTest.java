package com.contextualmocker.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static com.contextualmocker.core.ContextualMocker.*;
import static org.junit.jupiter.api.Assertions.*;

class ContextualMockerClassMockingTest {

    private static final SimpleServiceImpl mockSimpleService = mock(SimpleServiceImpl.class);
    private static final ClassWithNoDefaultConstructor mockNoDefaultConstructor = mock(ClassWithNoDefaultConstructor.class);
    private static final SimpleService mockSimpleInterface = mock(SimpleService.class);
    private static final ClassWithStaticMethod mockStaticMethodClass = mock(ClassWithStaticMethod.class);

    @BeforeEach
    void setUp() {
        ContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        ContextHolder.clearContext();
    }

    @Test
    void canMockConcreteClassWithDefaultConstructor() {
        ContextID ctx = new StringContextId(UUID.randomUUID().toString());
        given(mockSimpleService).forContext(ctx).when(() -> mockSimpleService.greet("Alice")).thenReturn("Hi Alice");
        assertEquals("Hi Alice", mockSimpleService.greet("Alice"));
        assertNull(mockSimpleService.getList(5));
    }

    @Test
    void canMockClassWithNoDefaultConstructor() {
        ContextID ctx = new StringContextId(UUID.randomUUID().toString());
        given(mockNoDefaultConstructor).forContext(ctx).when(() -> mockNoDefaultConstructor.greet("Bob")).thenReturn("Hello Bob");
        assertEquals("Hello Bob", mockNoDefaultConstructor.greet("Bob"));
        assertNull(mockNoDefaultConstructor.getValue());
    }

    @Test
    void toStringWorksForClassAndInterfaceMocks() {
        assertNotNull(mockSimpleService.toString());
        assertNotNull(mockSimpleInterface.toString());
        assertTrue(mockSimpleService.toString().contains("Mock") || mockSimpleService.toString().contains("mock"));
        assertTrue(mockSimpleInterface.toString().contains("Mock") || mockSimpleInterface.toString().contains("mock"));
    }

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
        ContextID ctx = new StringContextId(UUID.randomUUID().toString());
        given(mockStaticMethodClass).forContext(ctx).when(() -> mockStaticMethodClass.sayHello()).thenReturn("Mocked");
        assertEquals("Mocked", mockStaticMethodClass.sayHello());
        assertEquals("Static", ClassWithStaticMethod.staticMethod());
    }
}