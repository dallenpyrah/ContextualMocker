package com.contextualmocker.core;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static com.contextualmocker.core.ContextualMocker.*;

/**
 * Tests for verification mode classes (TimesVerificationMode, AtLeastVerificationMode, AtMostVerificationMode).
 */
class VerificationModeClassesTest {

    interface TestService {
        String getData(String id);
        void updateData(String id, String data);
    }

    @Test
    void testTimesVerificationMode_Success() throws Exception {
        TimesVerificationMode mode = new TimesVerificationMode(2);
        Method method = TestService.class.getMethod("getData", String.class);
        Object[] args = {"test"};
        
        // Should not throw when count matches
        mode.verifyCount(2, method, args);
        
        // Test with context - should not throw when count matches
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
        };
        ContextID context = new StringContextId("test-context");
        List<InvocationRecord> invocations = Arrays.asList(
            new InvocationRecord(mock, method, args, context, false),
            new InvocationRecord(mock, method, args, context, false)
        );
        
        mode.verifyCountWithContext(2, method, args, mock, context, invocations);
    }

    @Test
    void testTimesVerificationMode_Failure() throws Exception {
        TimesVerificationMode mode = new TimesVerificationMode(1);
        Method method = TestService.class.getMethod("getData", String.class);
        Object[] args = {"test"};
        
        // Test legacy verifyCount method
        AssertionError error = assertThrows(AssertionError.class, () -> {
            mode.verifyCount(2, method, args);
        });
        assertTrue(error.getMessage().contains("Wanted 1 invocations but got 2"));
        
        // Test with context - should throw VerificationFailureException
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
        };
        ContextID context = new StringContextId("test-context");
        List<InvocationRecord> invocations = Arrays.asList(
            new InvocationRecord(mock, method, args, context, false),
            new InvocationRecord(mock, method, args, context, false)
        );
        
        VerificationFailureException exception = assertThrows(VerificationFailureException.class, () -> {
            mode.verifyCountWithContext(2, method, args, mock, context, invocations);
        });
        
        assertEquals(1, exception.getExpectedCount());
        assertEquals(2, exception.getActualCount());
        assertEquals("exactly 1 time", exception.getVerificationMode());
        assertTrue(exception.getMessage().contains("exactly 1 time"));
    }

    @Test
    void testTimesVerificationMode_MultipleWanted() throws Exception {
        TimesVerificationMode mode = new TimesVerificationMode(3);
        Method method = TestService.class.getMethod("getData", String.class);
        Object[] args = {"test"};
        
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
        };
        ContextID context = new StringContextId("test-context");
        
        // Test failure with multiple expected
        VerificationFailureException exception = assertThrows(VerificationFailureException.class, () -> {
            mode.verifyCountWithContext(1, method, args, mock, context, Collections.emptyList());
        });
        
        assertEquals("exactly 3 times", exception.getVerificationMode()); // Should use "times" for plural
    }

    @Test
    void testAtLeastVerificationMode_Success() throws Exception {
        AtLeastVerificationMode mode = new AtLeastVerificationMode(2);
        Method method = TestService.class.getMethod("getData", String.class);
        Object[] args = {"test"};
        
        // Should not throw when count is equal or greater
        mode.verifyCount(2, method, args);
        mode.verifyCount(3, method, args);
        
        // Test with context
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
        };
        ContextID context = new StringContextId("test-context");
        List<InvocationRecord> invocations = Arrays.asList(
            new InvocationRecord(mock, method, args, context, false),
            new InvocationRecord(mock, method, args, context, false),
            new InvocationRecord(mock, method, args, context, false)
        );
        
        mode.verifyCountWithContext(3, method, args, mock, context, invocations);
    }

    @Test
    void testAtLeastVerificationMode_Failure() throws Exception {
        AtLeastVerificationMode mode = new AtLeastVerificationMode(3);
        Method method = TestService.class.getMethod("getData", String.class);
        Object[] args = {"test"};
        
        // Test legacy verifyCount method
        AssertionError error = assertThrows(AssertionError.class, () -> {
            mode.verifyCount(1, method, args);
        });
        assertTrue(error.getMessage().contains("Wanted at least 3 invocations but got only 1"));
        
        // Test with context
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
        };
        ContextID context = new StringContextId("test-context");
        List<InvocationRecord> invocations = Arrays.asList(
            new InvocationRecord(mock, method, args, context, false)
        );
        
        VerificationFailureException exception = assertThrows(VerificationFailureException.class, () -> {
            mode.verifyCountWithContext(1, method, args, mock, context, invocations);
        });
        
        assertEquals(3, exception.getExpectedCount());
        assertEquals(1, exception.getActualCount());
        assertEquals("at least 3 times", exception.getVerificationMode());
    }

    @Test
    void testAtLeastVerificationMode_SingleWanted() throws Exception {
        AtLeastVerificationMode mode = new AtLeastVerificationMode(1);
        Method method = TestService.class.getMethod("getData", String.class);
        Object[] args = {"test"};
        
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
        };
        ContextID context = new StringContextId("test-context");
        
        // Test failure with single expected
        VerificationFailureException exception = assertThrows(VerificationFailureException.class, () -> {
            mode.verifyCountWithContext(0, method, args, mock, context, Collections.emptyList());
        });
        
        assertEquals("at least 1 time", exception.getVerificationMode()); // Should use "time" for singular
    }

    @Test
    void testAtMostVerificationMode_Success() throws Exception {
        AtMostVerificationMode mode = new AtMostVerificationMode(3);
        Method method = TestService.class.getMethod("getData", String.class);
        Object[] args = {"test"};
        
        // Should not throw when count is equal or less
        mode.verifyCount(0, method, args);
        mode.verifyCount(2, method, args);
        mode.verifyCount(3, method, args);
        
        // Test with context
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
        };
        ContextID context = new StringContextId("test-context");
        List<InvocationRecord> invocations = Arrays.asList(
            new InvocationRecord(mock, method, args, context, false),
            new InvocationRecord(mock, method, args, context, false)
        );
        
        mode.verifyCountWithContext(2, method, args, mock, context, invocations);
    }

    @Test
    void testAtMostVerificationMode_Failure() throws Exception {
        AtMostVerificationMode mode = new AtMostVerificationMode(2);
        Method method = TestService.class.getMethod("getData", String.class);
        Object[] args = {"test"};
        
        // Test legacy verifyCount method
        AssertionError error = assertThrows(AssertionError.class, () -> {
            mode.verifyCount(5, method, args);
        });
        assertTrue(error.getMessage().contains("Wanted at most 2 invocations but got 5"));
        
        // Test with context
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
        };
        ContextID context = new StringContextId("test-context");
        List<InvocationRecord> invocations = Arrays.asList(
            new InvocationRecord(mock, method, args, context, false),
            new InvocationRecord(mock, method, args, context, false),
            new InvocationRecord(mock, method, args, context, false),
            new InvocationRecord(mock, method, args, context, false)
        );
        
        VerificationFailureException exception = assertThrows(VerificationFailureException.class, () -> {
            mode.verifyCountWithContext(4, method, args, mock, context, invocations);
        });
        
        assertEquals(2, exception.getExpectedCount());
        assertEquals(4, exception.getActualCount());
        assertEquals("at most 2 times", exception.getVerificationMode());
    }

    @Test
    void testAtMostVerificationMode_SingleWanted() throws Exception {
        AtMostVerificationMode mode = new AtMostVerificationMode(1);
        Method method = TestService.class.getMethod("getData", String.class);
        Object[] args = {"test"};
        
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
        };
        ContextID context = new StringContextId("test-context");
        List<InvocationRecord> invocations = Arrays.asList(
            new InvocationRecord(mock, method, args, context, false),
            new InvocationRecord(mock, method, args, context, false)
        );
        
        // Test failure with single expected
        VerificationFailureException exception = assertThrows(VerificationFailureException.class, () -> {
            mode.verifyCountWithContext(2, method, args, mock, context, invocations);
        });
        
        assertEquals("at most 1 time", exception.getVerificationMode()); // Should use "time" for singular
    }

    @Test
    void testContextualVerificationModeInterface() throws Exception {
        // Test that the default verifyCount method calls verifyCountWithContext with nulls
        TestVerificationMode mode = new TestVerificationMode();
        
        Method method = TestService.class.getMethod("getData", String.class);
        Object[] args = {"test"};
        
        // Call the default verifyCount method
        mode.verifyCount(5, method, args);
        
        // Verify that verifyCountWithContext was called with null values
        assertTrue(mode.wasVerifyCountWithContextCalled());
    }
    
    // Helper class to test the interface default method
    private static class TestVerificationMode implements ContextualMocker.ContextualVerificationMode {
        private boolean verifyCountWithContextCalled = false;
        
        @Override
        public void verifyCountWithContext(int actual, Method method, Object[] args, 
                                         Object mock, ContextID contextId, List<InvocationRecord> allInvocations) {
            verifyCountWithContextCalled = true;
            assertEquals(5, actual);
            assertNotNull(method);
            assertNotNull(args);
            assertNull(mock);
            assertNull(contextId);
            assertNull(allInvocations);
        }
        
        public boolean wasVerifyCountWithContextCalled() {
            return verifyCountWithContextCalled;
        }
    }

    @Test
    void testVerificationModeEdgeCases() throws Exception {
        Method method = TestService.class.getMethod("getData", String.class);
        Object[] args = {"test"};
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
        };
        ContextID context = new StringContextId("edge-case-context");
        
        // Test with zero wanted for times
        TimesVerificationMode timesZero = new TimesVerificationMode(0);
        timesZero.verifyCountWithContext(0, method, args, mock, context, Collections.emptyList());
        
        // Test with zero wanted for atLeast
        AtLeastVerificationMode atLeastZero = new AtLeastVerificationMode(0);
        atLeastZero.verifyCountWithContext(5, method, args, mock, context, Collections.emptyList());
        
        // Test with zero wanted for atMost
        AtMostVerificationMode atMostZero = new AtMostVerificationMode(0);
        atMostZero.verifyCountWithContext(0, method, args, mock, context, Collections.emptyList());
        
        // Test failure for atMost with zero wanted
        assertThrows(VerificationFailureException.class, () -> {
            atMostZero.verifyCountWithContext(1, method, args, mock, context, Collections.emptyList());
        });
    }

    @Test
    void testVerificationModeWithNullArguments() throws Exception {
        Method method = TestService.class.getMethod("getData", String.class);
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
        };
        ContextID context = new StringContextId("null-args-context");
        
        TimesVerificationMode mode = new TimesVerificationMode(1);
        
        // Should handle null arguments gracefully
        assertThrows(VerificationFailureException.class, () -> {
            mode.verifyCountWithContext(2, method, null, mock, context, Collections.emptyList());
        });
    }

    @Test
    void testVerificationModeWithNullContext() throws Exception {
        Method method = TestService.class.getMethod("getData", String.class);
        Object[] args = {"test"};
        TestService mock = new TestService() {
            @Override public String getData(String id) { return null; }
            @Override public void updateData(String id, String data) {}
        };
        
        TimesVerificationMode mode = new TimesVerificationMode(1);
        
        // Should handle null context gracefully
        assertThrows(VerificationFailureException.class, () -> {
            mode.verifyCountWithContext(2, method, args, mock, null, Collections.emptyList());
        });
    }
}