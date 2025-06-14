package io.github.dallenpyrah.contextualmocker.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.github.dallenpyrah.contextualmocker.core.ContextualMocker.*;
import static org.junit.jupiter.api.Assertions.*;

class ContextualMockerSpyTest {

    // Real class to spy on
    public static class RealService {
        private int counter = 0;

        public String processData(String input) {
            return "processed: " + input;
        }

        public int incrementCounter() {
            return ++counter;
        }

        public String externalCall() {
            return "real external call";
        }

        public void voidMethod() {
            counter += 10;
        }

        protected String protectedMethod() {
            return "protected method";
        }

        // Cannot be stubbed because it's final
        public final String finalMethod() {
            return "final method";
        }
    }

    // Interface for testing spy limitations
    interface TestInterface {
        String method();
    }

    // Final class for testing spy limitations
    public static final class FinalClass {
        public String method() {
            return "final class method";
        }
    }

    private ContextID testContext;

    @BeforeEach
    void setUp() {
        testContext = new StringContextId(UUID.randomUUID().toString());
        ContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        ContextHolder.clearContext();
    }

    @Test
    void spyDelegatesToRealObjectWhenNotStubbed() {
        RealService realService = new RealService();
        RealService spy = spy(realService);

        try (ContextScope scope = scopedContext(testContext)) {
            // No stubbing - should delegate to real implementation
            assertEquals("processed: test", spy.processData("test"));
            assertEquals(1, spy.incrementCounter());
            assertEquals(2, spy.incrementCounter());
        }
    }

    @Test
    void spyRecordsAllInvocations() {
        RealService realService = new RealService();
        RealService spy = spy(realService);

        try (ContextScope scope = scopedContext(testContext)) {
            // Stub one method
            scope.when(spy, () -> spy.externalCall())
                 .thenReturn("stubbed");

            // Call both stubbed and real methods
            spy.externalCall();
            spy.processData("test");
            spy.incrementCounter();

            // Verify all interactions were recorded
            scope.verify(spy, times(1), () -> spy.externalCall());
            scope.verify(spy, times(1), () -> spy.processData("test"));
            scope.verify(spy, times(1), () -> spy.incrementCounter());
        }
    }

    @Test
    void spyWorksWithVoidMethods() {
        RealService realService = new RealService();
        RealService spy = spy(realService);

        try (ContextScope scope = scopedContext(testContext)) {
            spy.voidMethod();
            
            // Verify void method was called
            scope.verify(spy, times(1), () -> { spy.voidMethod(); return null; });
            
            // Verify the method executed correctly by checking other methods in same context
            scope.verify(spy, never(), () -> spy.processData("unused"));
        }
    }

    @Test
    void spyFinalMethodsCannotBeStubbed() {
        RealService realService = new RealService();
        RealService spy = spy(realService);

        try (ContextScope scope = scopedContext(testContext)) {
            // Final methods always delegate to real implementation
            assertEquals("final method", spy.finalMethod());
            
            // We cannot stub final methods, but we can verify them
            scope.verify(spy, times(1), () -> spy.finalMethod());
        }
    }

    @Test
    void spyWithMultipleContexts() {
        RealService realService = new RealService();
        RealService spy = spy(realService);

        ContextID context1 = new StringContextId("context1");
        ContextID context2 = new StringContextId("context2");

        // Stub differently in each context
        when(spy, context1, () -> spy.externalCall())
            .thenReturn("context1 result");
        when(spy, context2, () -> spy.externalCall())
            .thenReturn("context2 result");

        // Test context1
        try (ContextScope scope1 = scopedContext(context1)) {
            assertEquals("context1 result", spy.externalCall());
            assertEquals("processed: test1", spy.processData("test1")); // Real method
        }

        // Test context2
        try (ContextScope scope2 = scopedContext(context2)) {
            assertEquals("context2 result", spy.externalCall());
            assertEquals("processed: test2", spy.processData("test2")); // Real method
        }

        // Verify context isolation
        verifyOnce(spy, context1, () -> spy.externalCall());
        verifyOnce(spy, context2, () -> spy.externalCall());
        verifyOnce(spy, context1, () -> spy.processData("test1"));
        verifyOnce(spy, context2, () -> spy.processData("test2"));
    }

    @Test
    void spyThrowsOnNullRealObject() {
        assertThrows(NullPointerException.class, () -> {
            spy(null);
        });
    }

    @Test
    void spyThrowsOnInterface() {
        TestInterface realObject = () -> "test";
        
        assertThrows(IllegalArgumentException.class, () -> {
            spy(realObject);
        });
    }

    @Test
    void spyThrowsOnFinalClass() {
        FinalClass realObject = new FinalClass();
        
        assertThrows(IllegalArgumentException.class, () -> {
            spy(realObject);
        });
    }

    @Test
    void spyToStringReflectsSpyNature() {
        RealService realService = new RealService();
        RealService spy = spy(realService);

        String toString = spy.toString();
        assertTrue(toString.startsWith("Spy of RealService@"));
    }

    @Test
    void spyEqualsAndHashCodeWork() {
        RealService realService = new RealService();
        RealService spy = spy(realService);

        // Spy should equal itself
        assertEquals(spy, spy);
        assertEquals(spy.hashCode(), spy.hashCode());

        // Different spy instances should not be equal
        RealService anotherSpy = spy(realService);
        assertNotEquals(spy, anotherSpy);
    }

    @Test
    void spyWithArgumentMatchers() {
        RealService realService = new RealService();
        RealService spy = spy(realService);

        try (ContextScope scope = scopedContext(testContext)) {
            scope.when(spy, () -> spy.processData(io.github.dallenpyrah.contextualmocker.matchers.ArgumentMatchers.startsWith("special")))
                 .thenReturn("special processing");

            // Stubbed for matching arguments
            assertEquals("special processing", spy.processData("special case"));
            
            // Real implementation for non-matching arguments
            assertEquals("processed: normal", spy.processData("normal"));

            scope.verify(spy, times(1), () -> spy.processData(io.github.dallenpyrah.contextualmocker.matchers.ArgumentMatchers.startsWith("special")));
            scope.verify(spy, times(1), () -> spy.processData("normal"));
        }
    }
}