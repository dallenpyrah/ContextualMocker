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
        
        try (ContextScope scope = scopedContext(context1)) {
            assertNull(mockGenericService.process("test"));
            assertNull(mockGenericService.getItems());
        }
    }

    @Test
    void testBasicThenReturn() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());

        try (ContextScope scope = scopedContext(context1)) {
            scope.when(mockService, () -> mockService.greet("World"))
                 .thenReturn("Hello Context 1");

            assertEquals("Hello Context 1", mockService.greet("World"));
        }
    }

    @Test
    void testStubbingDifferentMethodsSameContext() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());

        try (ContextScope scope = scopedContext(context1)) {
            scope.when(mockService, () -> mockService.greet("Alice"))
                 .thenReturn("Hi Alice");

            scope.when(mockService, () -> mockService.getList(5))
                 .thenReturn(List.of("a", "b", "c", "d", "e"));

            assertEquals("Hi Alice", mockService.greet("Alice"));
            assertEquals(List.of("a", "b", "c", "d", "e"), mockService.getList(5));
            mockService.doSomething();
        }
    }

    @Test
    void testStubbingSameMethodDifferentContexts() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        ContextID context2 = new StringContextId(UUID.randomUUID().toString());
        
        when(mockService, context1, () -> mockService.greet("Bob"))
            .thenReturn("Hello Bob from Context 1");

        when(mockService, context2, () -> mockService.greet("Bob"))
            .thenReturn("Greetings Bob from Context 2");

        try (ContextScope scope1 = scopedContext(context1)) {
            assertEquals("Hello Bob from Context 1", mockService.greet("Bob"));
        }

        try (ContextScope scope2 = scopedContext(context2)) {
            assertEquals("Greetings Bob from Context 2", mockService.greet("Bob"));
        }
    }

    @Test
    void testStubbingWithDifferentArgumentTypes() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());

        withContext(context1)
            .stub(mockService, () -> mockService.greet("Charlie"))
            .thenReturn("Hey Charlie")
            .stub(mockService, () -> mockService.getList(3))
            .thenReturn(List.of("x", "y", "z"));

        try (ContextScope scope = scopedContext(context1)) {
            assertEquals("Hey Charlie", mockService.greet("Charlie"));
            assertEquals(List.of("x", "y", "z"), mockService.getList(3));
        }
    }

    @Test
    void testAnyMatcherMatchesAnyValue() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        
        try (ContextScope scope = scopedContext(context1)) {
            scope.when(mockService, () -> mockService.greet(any()))
                 .thenReturn("matched");
            assertEquals("matched", mockService.greet("foo"));
            assertEquals("matched", mockService.greet(null));
        }
    }

    @Test
    void testAnyMatcherMatchesPrimitiveAndObject() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        
        try (ContextScope scope = scopedContext(context1)) {
            scope.when(mockService, () -> mockService.getList(anyInt()))
                 .thenReturn(List.of("any"));
            assertEquals(List.of("any"), mockService.getList(42));
            assertEquals(List.of("any"), mockService.getList(0));
        }
    }

    @Test
    void testEqMatcherMatchesDeepEquals() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        
        try (ContextScope scope = scopedContext(context1)) {
            scope.when(mockService, () -> mockService.greet(eq("deep")))
                 .thenReturn("deep matched");
            assertEquals("deep matched", mockService.greet("deep"));
            assertNull(mockService.greet("shallow"));
        }
    }

    @Test
    void testEqMatcherWithNull() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        
        try (ContextScope scope = scopedContext(context1)) {
            scope.when(mockService, () -> mockService.greet(eq(null)))
                 .thenReturn("null matched");
            assertEquals("null matched", mockService.greet(null));
            assertNull(mockService.greet("not null"));
        }
    }

    @Test
    void testMatchersWithComplexType() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        
        List<String> expected = List.of("a", "b");
        when(mockService, context1, () -> mockService.getList(eq(2)))
            .thenReturn(expected);
            
        try (ContextScope scope = scopedContext(context1)) {
            assertEquals(expected, mockService.getList(2));
            assertNull(mockService.getList(3));
        }
    }

    @Test
    void testMatcherStateIsPerInvocation() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        
        withContext(context1)
            .stub(mockService, () -> mockService.greet(eq("A")))
            .thenReturn("A matched")
            .stub(mockService, () -> mockService.greet(eq("B")))
            .thenReturn("B matched");
            
        try (ContextScope scope = scopedContext(context1)) {
            assertEquals("A matched", mockService.greet("A"));
            assertEquals("B matched", mockService.greet("B"));
            assertNull(mockService.greet("C"));
        }
    }

    @Test
    void testBasicVerificationTimes1() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());

        try (ContextScope scope = scopedContext(context1)) {
            mockService.greet("VerifyMe");
            scope.verify(mockService, times(1), () -> mockService.greet("VerifyMe"));
        }
    }

    @Test
    void testVerificationIgnoresStubbingInvocations() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());

        try (ContextScope scope = scopedContext(context1)) {
            scope.when(mockService, () -> mockService.greet("Setup"))
                 .thenReturn("Setup Response");

            mockService.greet("ActualCall");

            scope.verify(mockService, times(1), () -> mockService.greet("ActualCall"));
            scope.verify(mockService, never(), () -> mockService.greet("Setup"));
            scope.verifyNoMoreInteractions(mockService);
        }
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