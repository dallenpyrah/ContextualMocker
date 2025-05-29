package com.contextualmocker.core;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VerificationFailureException.
 */
class VerificationFailureExceptionTest {

    interface TestService {
        String getData(String id);
        void updateData(String id, String data);
        int processValues(int a, int b, String c);
        void noArgsMethod();
    }

    @Test
    void testExceptionWithNoInvocations() throws Exception {
        // Setup
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
            @Override public int processValues(int a, int b, String c) { return 0; }
            @Override public void noArgsMethod() {}
        };
        
        ContextID context = new StringContextId("test-context");
        Method method = TestService.class.getMethod("getData", String.class);
        Object[] expectedArgs = {"test-id"};
        
        // Create exception
        VerificationFailureException exception = new VerificationFailureException(
            mock, context, method, expectedArgs, 
            1, 0, "exactly 1 time", Collections.emptyList()
        );
        
        // Verify getters
        assertEquals(mock, exception.getMock());
        assertEquals(context, exception.getContextId());
        assertEquals(method, exception.getMethod());
        assertArrayEquals(expectedArgs, exception.getExpectedArgs());
        assertEquals(1, exception.getExpectedCount());
        assertEquals(0, exception.getActualCount());
        assertEquals("exactly 1 time", exception.getVerificationMode());
        assertTrue(exception.getActualInvocations().isEmpty());
        
        // Verify message content
        String message = exception.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("VERIFICATION FAILURE"));
        assertTrue(message.contains("Expected: exactly 1 time (1 invocations)"));
        assertTrue(message.contains("Actual:   0 invocations"));
        assertTrue(message.contains("TestService.getData(String)"));
        assertTrue(message.contains("\"test-id\""));
        assertTrue(message.contains("test-context"));
        assertTrue(message.contains("NO INVOCATIONS RECORDED"));
        assertTrue(message.contains("Ensure the method is called on the correct mock instance"));
    }

    @Test
    void testExceptionWithActualInvocations() throws Exception {
        // Setup
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
            @Override public int processValues(int a, int b, String c) { return 0; }
            @Override public void noArgsMethod() {}
        };
        
        ContextID context = new StringContextId("multi-invocation-context");
        Method method = TestService.class.getMethod("getData", String.class);
        Object[] expectedArgs = {"expected-id"};
        
        // Create invocation records
        List<InvocationRecord> invocations = Arrays.asList(
            new InvocationRecord(mock, method, new Object[]{"actual-id-1"}, context, false),
            new InvocationRecord(mock, method, new Object[]{"actual-id-2"}, context, false)
        );
        
        // Create exception - too many invocations
        VerificationFailureException exception = new VerificationFailureException(
            mock, context, method, expectedArgs,
            1, 2, "exactly 1 time", invocations
        );
        
        // Verify getters
        assertEquals(2, exception.getActualCount());
        assertEquals(2, exception.getActualInvocations().size());
        
        // Verify message content
        String message = exception.getMessage();
        assertTrue(message.contains("Actual:   2 invocations"));
        assertTrue(message.contains("Actual Invocations (2):"));
        assertTrue(message.contains("1. getData(\"actual-id-1\") at "));
        assertTrue(message.contains("2. getData(\"actual-id-2\") at "));
        assertTrue(message.contains("Look for unexpected additional method calls"));
        assertTrue(message.contains("method is called multiple times unintentionally"));
    }

    @Test
    void testExceptionWithNullArguments() throws Exception {
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
            @Override public int processValues(int a, int b, String c) { return 0; }
            @Override public void noArgsMethod() {}
        };
        
        ContextID context = new StringContextId("null-args-context");
        Method method = TestService.class.getMethod("getData", String.class);
        
        // Test with null expected args
        VerificationFailureException exception1 = new VerificationFailureException(
            mock, context, method, null,
            1, 0, "exactly 1 time", Collections.emptyList()
        );
        
        String message1 = exception1.getMessage();
        assertTrue(message1.contains("(no arguments)"));
        
        // Test with empty expected args
        VerificationFailureException exception2 = new VerificationFailureException(
            mock, context, method, new Object[0],
            1, 0, "exactly 1 time", Collections.emptyList()
        );
        
        String message2 = exception2.getMessage();
        assertTrue(message2.contains("(no arguments)"));
    }

    @Test
    void testExceptionWithNoArgsMethod() throws Exception {
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
            @Override public int processValues(int a, int b, String c) { return 0; }
            @Override public void noArgsMethod() {}
        };
        
        ContextID context = new StringContextId("no-args-context");
        Method method = TestService.class.getMethod("noArgsMethod");
        
        VerificationFailureException exception = new VerificationFailureException(
            mock, context, method, new Object[0],
            1, 0, "exactly 1 time", Collections.emptyList()
        );
        
        String message = exception.getMessage();
        assertTrue(message.contains("TestService.noArgsMethod()"));
    }

    @Test
    void testExceptionWithComplexArguments() throws Exception {
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
            @Override public int processValues(int a, int b, String c) { return 0; }
            @Override public void noArgsMethod() {}
        };
        
        ContextID context = new StringContextId("complex-args-context");
        Method method = TestService.class.getMethod("processValues", int.class, int.class, String.class);
        Object[] expectedArgs = {42, null, "test-string"};
        
        // Create invocation with different argument types
        List<InvocationRecord> invocations = Arrays.asList(
            new InvocationRecord(mock, method, new Object[]{100, 200, "actual-string"}, context, false)
        );
        
        VerificationFailureException exception = new VerificationFailureException(
            mock, context, method, expectedArgs,
            2, 1, "exactly 2 times", invocations
        );
        
        String message = exception.getMessage();
        assertTrue(message.contains("TestService.processValues(int, int, String)"));
        assertTrue(message.contains("42, null, \"test-string\""));
        assertTrue(message.contains("processValues(100, 200, \"actual-string\")"));
        assertTrue(message.contains("Check if some calls were made in different contexts"));
    }

    @Test
    void testExceptionWithCharacterArguments() throws Exception {
        // Test character formatting
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
            @Override public int processValues(int a, int b, String c) { return 0; }
            @Override public void noArgsMethod() {}
        };
        
        ContextID context = new StringContextId("char-args-context");
        Method method = TestService.class.getMethod("getData", String.class);
        
        // Create invocation record with character argument (simulated)
        List<InvocationRecord> invocations = Arrays.asList(
            new InvocationRecord(mock, method, new Object[]{'A'}, context, false)
        );
        
        VerificationFailureException exception = new VerificationFailureException(
            mock, context, method, new Object[]{'B'},
            1, 1, "exactly 1 time", invocations
        );
        
        String message = exception.getMessage();
        assertTrue(message.contains("'B'"));
        assertTrue(message.contains("'A'"));
    }

    @Test
    void testExceptionWithNullContext() throws Exception {
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
            @Override public int processValues(int a, int b, String c) { return 0; }
            @Override public void noArgsMethod() {}
        };
        
        Method method = TestService.class.getMethod("getData", String.class);
        
        VerificationFailureException exception = new VerificationFailureException(
            mock, null, method, new Object[]{"test"},
            1, 0, "exactly 1 time", Collections.emptyList()
        );
        
        String message = exception.getMessage();
        assertTrue(message.contains("No context"));
        assertNull(exception.getContextId());
    }

    @Test
    void testExceptionWithManyInvocations() throws Exception {
        // Test truncation of invocation list
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
            @Override public int processValues(int a, int b, String c) { return 0; }
            @Override public void noArgsMethod() {}
        };
        
        ContextID context = new StringContextId("many-invocations-context");
        Method method = TestService.class.getMethod("getData", String.class);
        
        // Create 15 invocation records (should truncate to 10)
        List<InvocationRecord> invocations = Arrays.asList(
            new InvocationRecord(mock, method, new Object[]{"call-1"}, context, false),
            new InvocationRecord(mock, method, new Object[]{"call-2"}, context, false),
            new InvocationRecord(mock, method, new Object[]{"call-3"}, context, false),
            new InvocationRecord(mock, method, new Object[]{"call-4"}, context, false),
            new InvocationRecord(mock, method, new Object[]{"call-5"}, context, false),
            new InvocationRecord(mock, method, new Object[]{"call-6"}, context, false),
            new InvocationRecord(mock, method, new Object[]{"call-7"}, context, false),
            new InvocationRecord(mock, method, new Object[]{"call-8"}, context, false),
            new InvocationRecord(mock, method, new Object[]{"call-9"}, context, false),
            new InvocationRecord(mock, method, new Object[]{"call-10"}, context, false),
            new InvocationRecord(mock, method, new Object[]{"call-11"}, context, false),
            new InvocationRecord(mock, method, new Object[]{"call-12"}, context, false),
            new InvocationRecord(mock, method, new Object[]{"call-13"}, context, false),
            new InvocationRecord(mock, method, new Object[]{"call-14"}, context, false),
            new InvocationRecord(mock, method, new Object[]{"call-15"}, context, false)
        );
        
        VerificationFailureException exception = new VerificationFailureException(
            mock, context, method, new Object[]{"expected"},
            1, 15, "exactly 1 time", invocations
        );
        
        String message = exception.getMessage();
        assertTrue(message.contains("Actual Invocations (15):"));
        assertTrue(message.contains("10. getData(\"call-10\")"));
        assertTrue(message.contains("... and 5 more invocations"));
        assertFalse(message.contains("call-11")); // Should be truncated
    }

    @Test
    void testExceptionAsAssertionError() {
        // Verify it extends AssertionError for test framework compatibility
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
            @Override public int processValues(int a, int b, String c) { return 0; }
            @Override public void noArgsMethod() {}
        };
        
        try {
            Method method = TestService.class.getMethod("getData", String.class);
            VerificationFailureException exception = new VerificationFailureException(
                mock, new StringContextId("test"), method, new Object[]{"test"},
                1, 0, "exactly 1 time", Collections.emptyList()
            );
            
            assertTrue(exception instanceof AssertionError);
            assertNotNull(exception.getMessage());
            
        } catch (Exception e) {
            fail("Should not throw exception during test setup");
        }
    }
}