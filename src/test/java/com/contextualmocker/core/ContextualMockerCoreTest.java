package com.contextualmocker.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.contextualmocker.core.ContextualMocker.*;

import java.util.List;

import com.contextualmocker.core.SimpleService;
import com.contextualmocker.core.SimpleServiceImpl;
import com.contextualmocker.core.GenericService;
import static com.contextualmocker.matchers.ArgumentMatchers.*;

public class ContextualMockerCoreTest {


    private final ContextID context1 = new StringContextId("context1");
    private final ContextID context2 = new StringContextId("context2");
    private SimpleService mockService;

    @BeforeEach
    void setUp() {
        mockService = mock(SimpleService.class);
        ContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        ContextHolder.clearContext();
    }

    // --- Mock Creation Tests (Plan 224-234) ---

    @Test
    void testMockInterface() {
        // Mock creation moved to setUp, just assert here
        assertNotNull(mockService);
        // Verify it's a proxy (implementation detail, but confirms mocking happened)
        assertTrue(mockService.getClass().getName().contains("ByteBuddy"));
    }

    @Test
    void testMockNullClassThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            mock(null);
        }, "Mocking null should throw NullPointerException");
    }

    // Assuming mocking concrete classes is not supported or throws specific exception
    // based on plan line 228 mentioning only interfaces. Adjust if implementation differs.

    @Test
    void testMockInterfaceWithGenerics() {
        // Use the static inner interface
        GenericService<String> mockGenericService = mock(GenericService.class);
        assertNotNull(mockGenericService);
        assertTrue(mockGenericService.getClass().getName().contains("ByteBuddy"));
        // Basic interaction check (will return default null without stubbing)
        // Set context for interaction if required by handler logic, though not strictly needed for null check
        ContextHolder.setContext(context1);
        assertNull(mockGenericService.process("test"));
        assertNull(mockGenericService.getItems());
        ContextHolder.clearContext();
    }

    // --- Basic Stubbing Tests (Plan 257, 261, 262, 263) ---

    @Test
    void testBasicThenReturn() {
        ContextHolder.setContext(context1);

        // Corrected 'when' usage with Supplier
        given(mockService)
            .forContext(context1)
            .when(() -> mockService.greet("World")) // Use Supplier lambda
            .thenReturn("Hello Context 1");

        assertEquals("Hello Context 1", mockService.greet("World"));

        ContextHolder.clearContext();
    }

    @Test
    void testStubbingDifferentMethodsSameContext() {
        ContextHolder.setContext(context1);

        // Corrected 'when' usage
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
        mockService.doSomething(); // No stubbing, should just work (void)

        ContextHolder.clearContext();
    }

    @Test
    void testStubbingSameMethodDifferentContexts() {
        // Stub for context1
        ContextHolder.setContext(context1);
        given(mockService)
            .forContext(context1)
            .when(() -> mockService.greet("Bob")) // Corrected 'when'
            .thenReturn("Hello Bob from Context 1");

        // Stub for context2
        ContextHolder.setContext(context2);
        given(mockService)
            .forContext(context2)
            .when(() -> mockService.greet("Bob")) // Corrected 'when'
            .thenReturn("Greetings Bob from Context 2");

        // Verify context1
        ContextHolder.setContext(context1);
        assertEquals("Hello Bob from Context 1", mockService.greet("Bob"));

        // Verify context2
        ContextHolder.setContext(context2);
        assertEquals("Greetings Bob from Context 2", mockService.greet("Bob"));

        ContextHolder.clearContext();
    }

     @Test
    void testStubbingWithDifferentArgumentTypes() {
        ContextHolder.setContext(context1);

        given(mockService)
            .forContext(context1)
            .when(() -> mockService.greet("Charlie")) // Corrected 'when'
            .thenReturn("Hey Charlie");

        given(mockService)
            .forContext(context1)
            .when(() -> mockService.getList(3)) // Corrected 'when'
            .thenReturn(List.of("x", "y", "z"));

       assertEquals("Hey Charlie", mockService.greet("Charlie"));
       assertEquals(List.of("x", "y", "z"), mockService.getList(3));


       ContextHolder.clearContext();
    }
   

   // --- Argument Matcher Edge Case Tests (Plan 308-320) ---

   @Test
   void testAnyMatcherMatchesAnyValue() {
       ContextHolder.setContext(context1);
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
       ContextHolder.setContext(context1);
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
       ContextHolder.setContext(context1);
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
       ContextHolder.setContext(context1);
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
       ContextHolder.setContext(context1);
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
       ContextHolder.setContext(context1);
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


    // --- Basic Verification Tests (Plan 278, 287) ---

    @Test
    void testBasicVerificationTimes1() {
        ContextHolder.setContext(context1);

        // No stubbing needed for basic verification test
        mockService.greet("VerifyMe"); // Actual invocation

        // Corrected Verification flow
        verify(mockService)                 // Start verification
            .forContext(context1)           // Specify context
            .verify(times(1))               // Specify mode, returns mock proxy
            .greet("VerifyMe");             // Call method on proxy

        ContextHolder.clearContext();
    }

    @Test
    void testVerificationIgnoresStubbingInvocations() {
        ContextHolder.setContext(context1);

        // This invocation happens during stubbing setup
        given(mockService)
            .forContext(context1)
            .when(() -> mockService.greet("Setup")) // Corrected 'when'
            .thenReturn("Setup Response");

        // Actual invocation to be verified
        mockService.greet("ActualCall");

        // Verify only the actual call, not the setup call
        verify(mockService)
            .forContext(context1)
            .verify(times(1))
            .greet("ActualCall");

        // Verify the setup call was NOT recorded for verification purposes
         verify(mockService)
             .forContext(context1)
             .verify(never()) // Use never() mode
             .greet("Setup");

        // Verify no other calls happened using the static helper
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