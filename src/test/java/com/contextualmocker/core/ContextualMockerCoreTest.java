package com.contextualmocker.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.contextualmocker.core.ContextualMocker.*;

import java.util.List;
import java.util.UUID;

import com.contextualmocker.core.SimpleService;
import com.contextualmocker.core.SimpleServiceImpl;
import com.contextualmocker.core.GenericService;
import static com.contextualmocker.matchers.ArgumentMatchers.*;

public class ContextualMockerCoreTest {

    private static final SimpleService mockService = mock(SimpleService.class);

    @BeforeEach
    void setUp() {
        ContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        ContextHolder.clearContext();
    }

    @Test
    void testMockInterface() {
        assertNotNull(mockService);
        assertTrue(mockService.getClass().getName().contains("ByteBuddy"));
    }

    @Test
    void testMockNullClassThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            mock(null);
        }, "Mocking null should throw NullPointerException");
    }

    @Test
    void testMockInterfaceWithGenerics() {
        GenericService<String> mockGenericService = mock(GenericService.class);
        assertNotNull(mockGenericService);
        assertTrue(mockGenericService.getClass().getName().contains("ByteBuddy"));
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        ContextHolder.setContext(context1);
        assertNull(mockGenericService.process("test"));
        assertNull(mockGenericService.getItems());
        ContextHolder.clearContext();
    }

    @Test
    void testBasicThenReturn() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());

        given(mockService)
                .forContext(context1)
                .when(() -> mockService.greet("World"))
                .thenReturn("Hello Context 1");

        assertEquals("Hello Context 1", mockService.greet("World"));

        ContextHolder.clearContext();
    }

    @Test
    void testStubbingDifferentMethodsSameContext() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());

        given(mockService)
                .forContext(context1)
                .when(() -> mockService.greet("Alice"))
                .thenReturn("Hi Alice");

        given(mockService)
                .forContext(context1)
                .when(() -> mockService.getList(5))
                .thenReturn(List.of("a", "b", "c", "d", "e"));

        assertEquals("Hi Alice", mockService.greet("Alice"));
        assertEquals(List.of("a", "b", "c", "d", "e"), mockService.getList(5));
        mockService.doSomething();

        ContextHolder.clearContext();
    }

    @Test
    void testStubbingSameMethodDifferentContexts() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        ContextID context2 = new StringContextId(UUID.randomUUID().toString());
        given(mockService)
                .forContext(context1)
                .when(() -> mockService.greet("Bob"))
                .thenReturn("Hello Bob from Context 1");

        given(mockService)
                .forContext(context2)
                .when(() -> mockService.greet("Bob"))
                .thenReturn("Greetings Bob from Context 2");

        ContextHolder.setContext(context1);
        assertEquals("Hello Bob from Context 1", mockService.greet("Bob"));

        ContextHolder.setContext(context2);
        assertEquals("Greetings Bob from Context 2", mockService.greet("Bob"));

        ContextHolder.clearContext();
    }

    @Test
    void testStubbingWithDifferentArgumentTypes() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());

        given(mockService)
                .forContext(context1)
                .when(() -> mockService.greet("Charlie"))
                .thenReturn("Hey Charlie");

        given(mockService)
                .forContext(context1)
                .when(() -> mockService.getList(3))
                .thenReturn(List.of("x", "y", "z"));

        assertEquals("Hey Charlie", mockService.greet("Charlie"));
        assertEquals(List.of("x", "y", "z"), mockService.getList(3));

        ContextHolder.clearContext();
    }

    @Test
    void testAnyMatcherMatchesAnyValue() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        given(mockService)
                .forContext(context1)
                .when(() -> mockService.greet(any()))
                .thenReturn("matched");
        assertEquals("matched", mockService.greet("foo"));
        assertEquals("matched", mockService.greet(null));
        ContextHolder.clearContext();
    }

    @Test
    void testAnyMatcherMatchesPrimitiveAndObject() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        given(mockService)
                .forContext(context1)
                .when(() -> mockService.getList(anyInt()))
                .thenReturn(List.of("any"));
        assertEquals(List.of("any"), mockService.getList(42));
        assertEquals(List.of("any"), mockService.getList(0));
        ContextHolder.clearContext();
    }

    @Test
    void testEqMatcherMatchesDeepEquals() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        given(mockService)
                .forContext(context1)
                .when(() -> mockService.greet(eq("deep")))
                .thenReturn("deep matched");
        assertEquals("deep matched", mockService.greet("deep"));
        assertNull(mockService.greet("shallow"));
        ContextHolder.clearContext();
    }

    @Test
    void testEqMatcherWithNull() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        given(mockService)
                .forContext(context1)
                .when(() -> mockService.greet(eq(null)))
                .thenReturn("null matched");
        assertEquals("null matched", mockService.greet(null));
        assertNull(mockService.greet("not null"));
        ContextHolder.clearContext();
    }

    @Test
    void testMatchersWithComplexType() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        List<String> expected = List.of("a", "b");
        given(mockService)
                .forContext(context1)
                .when(() -> mockService.getList(eq(2)))
                .thenReturn(expected);
        assertEquals(expected, mockService.getList(2));
        assertNull(mockService.getList(3));
        ContextHolder.clearContext();
    }

    @Test
    void testMatcherStateIsPerInvocation() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        given(mockService)
                .forContext(context1)
                .when(() -> mockService.greet(eq("A")))
                .thenReturn("A matched");
        given(mockService)
                .forContext(context1)
                .when(() -> mockService.greet(eq("B")))
                .thenReturn("B matched");
        assertEquals("A matched", mockService.greet("A"));
        assertEquals("B matched", mockService.greet("B"));
        assertNull(mockService.greet("C"));
        ContextHolder.clearContext();
    }

    @Test
    void testBasicVerificationTimes1() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        ContextHolder.setContext(context1);

        mockService.greet("VerifyMe");

        verify(mockService)
                .forContext(context1)
                .verify(times(1))
                .greet("VerifyMe");

        ContextHolder.clearContext();
    }

    @Test
    void testVerificationIgnoresStubbingInvocations() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());

        given(mockService)
                .forContext(context1)
                .when(() -> mockService.greet("Setup"))
                .thenReturn("Setup Response");

        mockService.greet("ActualCall");

        verify(mockService)
                .forContext(context1)
                .verify(times(1))
                .greet("ActualCall");

        verify(mockService)
                .forContext(context1)
                .verify(never())
                .greet("Setup");

        verifyNoMoreInteractions(mockService, context1);

        ContextHolder.clearContext();
    }

    @Test
    void testAnyMatcherMatchesAndToString() {
        com.contextualmocker.matchers.AnyMatcher matcher = new com.contextualmocker.matchers.AnyMatcher();
        assertTrue(matcher.matches("anything"));
        assertTrue(matcher.matches(123));
        assertTrue(matcher.matches(null));
        assertNotNull(matcher.toString());
    }
}