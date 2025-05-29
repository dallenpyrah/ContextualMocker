package com.contextualmocker.handlers;

import com.contextualmocker.core.ContextID;
import com.contextualmocker.core.ContextualMocker;
import com.contextualmocker.core.ContextHolder;
import com.contextualmocker.core.InvocationRecord;
import com.contextualmocker.core.MockRegistry;
import com.contextualmocker.matchers.ArgumentMatcher;
import com.contextualmocker.matchers.MatcherContext;
import com.contextualmocker.core.StringContextId;
import com.contextualmocker.initiators.ContextSpecificVerificationInitiatorImpl;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VerificationMethodCaptureHandlerTest {

    static class Dummy {
        void foo() {}
        String getData(String id) { return null; }
        int processValues(int a, String b) { return 0; }
        void methodWithDifferentArgs(String a, int b, boolean c) {}
    }

    @BeforeEach
    void setUp() {
        ContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        ContextHolder.clearContext();
    }

    @Test
    void testInvoke_NoInvocations_DoesNotThrow() throws Throwable {
        Dummy mock = new Dummy();
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        ContextualMocker.ContextualVerificationMode mode = mock(ContextualMocker.ContextualVerificationMode.class);

        Method method = Dummy.class.getDeclaredMethod("foo");
        Object[] args = new Object[0];

        try (var mockStatic = mockStatic(MockRegistry.class)) {
            mockStatic.when(() -> MockRegistry.getInvocationRecords(any(), any()))
                    .thenReturn(Collections.emptyList());

            VerificationMethodCaptureHandler<Dummy> handler =
                    new VerificationMethodCaptureHandler<>(mock, mode, contextId);

            ContextHolder.setContext(contextId);
            assertDoesNotThrow(() -> handler.invoke(mock, method, args));
        }
    }

    @Test
    void testInvoke_WithMatchingInvocation_TimesVerificationMode() throws Throwable {
        Dummy mock = new Dummy();
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        Method method = Dummy.class.getDeclaredMethod("foo");
        Object[] args = new Object[0];

        InvocationRecord record = mock(InvocationRecord.class);
        when(record.getMethod()).thenReturn(method);
        when(record.getArguments()).thenReturn(args);

        try (var mockStatic = mockStatic(MockRegistry.class)) {
            mockStatic.when(() -> MockRegistry.getInvocationRecords(any(), any()))
                    .thenReturn(Collections.singletonList(record));

            ContextualMocker.TimesVerificationMode timesMode = mock(ContextualMocker.TimesVerificationMode.class);

            VerificationMethodCaptureHandler<Dummy> handler =
                    new VerificationMethodCaptureHandler<>(mock, timesMode, contextId);
            ContextHolder.setContext(contextId);
            handler.invoke(mock, method, args);

            verify(timesMode).verifyCountWithContext(eq(1), eq(method), eq(args), eq(mock), eq(contextId), any());
        }
    }

    @Test
    void testInvoke_WithNonMatchingInvocation_AtLeastVerificationMode() throws Throwable {
        Dummy mock = new Dummy();
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        Method method = Dummy.class.getDeclaredMethod("foo");
        Object[] args = new Object[] { "foo" };

        InvocationRecord record = mock(InvocationRecord.class);
        when(record.getMethod()).thenReturn(method);
        when(record.getArguments()).thenReturn(new Object[] { "bar" });

        try (var mockStatic = mockStatic(MockRegistry.class)) {
            mockStatic.when(() -> MockRegistry.getInvocationRecords(any(), any()))
                    .thenReturn(Collections.singletonList(record));

            ContextualMocker.AtLeastVerificationMode atLeastMode = mock(ContextualMocker.AtLeastVerificationMode.class);

            VerificationMethodCaptureHandler<Dummy> handler =
                    new VerificationMethodCaptureHandler<>(mock, atLeastMode, contextId);
            ContextHolder.setContext(contextId);
            handler.invoke(mock, method, args);

            verify(atLeastMode).verifyCountWithContext(eq(0), eq(method), eq(args), eq(mock), eq(contextId), any());
        }
    }

    @Test
    void testGetDefaultValue_PrimitivesAndReference() throws Exception {
        Dummy mock = new Dummy();
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        ContextualMocker.ContextualVerificationMode mode = mock(ContextualMocker.ContextualVerificationMode.class);

        VerificationMethodCaptureHandler<?> handler =
                new VerificationMethodCaptureHandler<>(mock, mode, contextId);

        Method getDefaultValue = VerificationMethodCaptureHandler.class
                .getDeclaredMethod("getDefaultValue", Class.class);
        getDefaultValue.setAccessible(true);

        assertNull(getDefaultValue.invoke(handler, String.class));
        assertEquals(false, getDefaultValue.invoke(handler, boolean.class));
        assertEquals((byte)0, getDefaultValue.invoke(handler, byte.class));
        assertEquals((short)0, getDefaultValue.invoke(handler, short.class));
        assertEquals(0, getDefaultValue.invoke(handler, int.class));
        assertEquals(0L, getDefaultValue.invoke(handler, long.class));
        assertEquals(0.0f, getDefaultValue.invoke(handler, float.class));
        assertEquals(0.0d, getDefaultValue.invoke(handler, double.class));
        assertEquals('\u0000', getDefaultValue.invoke(handler, char.class));
    }

    @Test
    void testVerificationWithArgumentMatchers() throws Throwable {
        Dummy mock = new Dummy();
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        Method method = Dummy.class.getDeclaredMethod("getData", String.class);
        
        // Create a real invocation record
        InvocationRecord record = new InvocationRecord(mock, method, new Object[]{"actual-id"}, contextId, false);

        ArgumentMatcher<String> matcher = new ArgumentMatcher<String>() {
            @Override
            public boolean matches(Object argument) {
                return "actual-id".equals(argument);
            }
        };

        try (var mockStatic = mockStatic(MockRegistry.class);
             var matcherMockStatic = mockStatic(MatcherContext.class)) {
            
            mockStatic.when(() -> MockRegistry.getInvocationRecords(any(), any()))
                    .thenReturn(Collections.singletonList(record));
            
            matcherMockStatic.when(MatcherContext::consumeMatchers)
                    .thenReturn(Collections.singletonList(matcher));

            ContextualMocker.TimesVerificationMode timesMode = new ContextualMocker.TimesVerificationMode(1);

            ContextSpecificVerificationInitiatorImpl<Dummy> initiator = 
                new ContextSpecificVerificationInitiatorImpl<>(mock);
            
            ContextHolder.setContext(contextId);
            
            // Test the that() method which uses the inner handler
            assertDoesNotThrow(() -> initiator.that(timesMode, () -> mock.getData("test-id")));
        }
    }

    @Test
    void testVerificationInitiatorThatMethodWithNullContext() {
        Dummy mock = new Dummy();
        ContextSpecificVerificationInitiatorImpl<Dummy> initiator = 
            new ContextSpecificVerificationInitiatorImpl<>(mock);
        
        ContextualMocker.TimesVerificationMode timesMode = new ContextualMocker.TimesVerificationMode(1);
        
        // Should throw when context is null (actually throws IllegalStateException)
        assertThrows(IllegalStateException.class, () -> {
            initiator.that(timesMode, () -> mock.getData("test"));
        });
    }

    @Test
    void testVerificationInitiatorThatMethodWithNullMode() {
        Dummy mock = new Dummy();
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        ContextSpecificVerificationInitiatorImpl<Dummy> initiator = 
            new ContextSpecificVerificationInitiatorImpl<>(mock);
        
        ContextHolder.setContext(contextId);
        
        // Should throw when mode is null
        assertThrows(NullPointerException.class, () -> {
            initiator.that(null, () -> mock.getData("test"));
        });
    }

    @Test
    void testVerificationInitiatorThatMethodWithNullMethodCall() {
        Dummy mock = new Dummy();
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        ContextSpecificVerificationInitiatorImpl<Dummy> initiator = 
            new ContextSpecificVerificationInitiatorImpl<>(mock);
        
        ContextHolder.setContext(contextId);
        ContextualMocker.TimesVerificationMode timesMode = new ContextualMocker.TimesVerificationMode(1);
        
        // Should throw when method call is null
        assertThrows(NullPointerException.class, () -> {
            initiator.that(timesMode, null);
        });
    }

    @Test
    void testVerificationInitiatorVerifyMethodWithNullContext() {
        Dummy mock = new Dummy();
        ContextSpecificVerificationInitiatorImpl<Dummy> initiator = 
            new ContextSpecificVerificationInitiatorImpl<>(mock);
        
        ContextualMocker.TimesVerificationMode timesMode = new ContextualMocker.TimesVerificationMode(1);
        
        // Should throw when context is null (actually throws IllegalStateException)
        assertThrows(IllegalStateException.class, () -> {
            initiator.verify(timesMode);
        });
    }

    @Test
    void testVerificationMethodCaptureHandler_ComplexArgumentMatching() throws Throwable {
        Dummy mock = new Dummy();
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        Method method = Dummy.class.getDeclaredMethod("processValues", int.class, String.class);
        
        // Create multiple invocation records with different argument combinations
        InvocationRecord record1 = new InvocationRecord(mock, method, new Object[]{1, "test"}, contextId, false);
        InvocationRecord record2 = new InvocationRecord(mock, method, new Object[]{2, "other"}, contextId, false);
        InvocationRecord record3 = new InvocationRecord(mock, method, new Object[]{1, "test"}, contextId, false); // duplicate
        
        List<InvocationRecord> allRecords = Arrays.asList(record1, record2, record3);
        
        try (var mockStatic = mockStatic(MockRegistry.class)) {
            mockStatic.when(() -> MockRegistry.getInvocationRecords(any(), any()))
                    .thenReturn(allRecords);
            
            ContextualMocker.TimesVerificationMode timesMode = mock(ContextualMocker.TimesVerificationMode.class);
            
            VerificationMethodCaptureHandler<Dummy> handler =
                    new VerificationMethodCaptureHandler<>(mock, timesMode, contextId);
            
            ContextHolder.setContext(contextId);
            
            // Test exact argument matching - should find 2 matching records
            Object[] testArgs = {1, "test"};
            handler.invoke(mock, method, testArgs);
            
            verify(timesMode).verifyCountWithContext(eq(2), eq(method), eq(testArgs), eq(mock), eq(contextId), eq(allRecords));
        }
    }

    @Test
    void testVerificationMethodCaptureHandler_DifferentArgumentLengths() throws Throwable {
        Dummy mock = new Dummy();
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        Method method = Dummy.class.getDeclaredMethod("getData", String.class);
        
        // Create invocation record with single argument
        InvocationRecord record = new InvocationRecord(mock, method, new Object[]{"test"}, contextId, false);
        
        try (var mockStatic = mockStatic(MockRegistry.class)) {
            mockStatic.when(() -> MockRegistry.getInvocationRecords(any(), any()))
                    .thenReturn(Collections.singletonList(record));
            
            ContextualMocker.TimesVerificationMode timesMode = mock(ContextualMocker.TimesVerificationMode.class);
            
            VerificationMethodCaptureHandler<Dummy> handler =
                    new VerificationMethodCaptureHandler<>(mock, timesMode, contextId);
            
            ContextHolder.setContext(contextId);
            
            // Test with different argument count - should find 0 matches
            Object[] testArgs = {"test", "extra"}; // 2 args instead of 1
            handler.invoke(mock, method, testArgs);
            
            verify(timesMode).verifyCountWithContext(eq(0), eq(method), eq(testArgs), eq(mock), eq(contextId), any());
        }
    }

    @Test
    void testVerificationMethodCaptureHandler_WithArgumentMatchersPartialMatch() throws Throwable {
        Dummy mock = new Dummy();
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        Method method = Dummy.class.getDeclaredMethod("processValues", int.class, String.class);
        
        InvocationRecord record = new InvocationRecord(mock, method, new Object[]{42, "matching"}, contextId, false);
        
        // Create matchers - one that matches, one that doesn't
        ArgumentMatcher<Integer> intMatcher = new ArgumentMatcher<Integer>() {
            @Override
            public boolean matches(Object argument) {
                return argument instanceof Integer && ((Integer) argument) == 42;
            }
        };
        
        ArgumentMatcher<String> stringMatcher = new ArgumentMatcher<String>() {
            @Override
            public boolean matches(Object argument) {
                return "nomatch".equals(argument);
            }
        };
        
        try (var mockStatic = mockStatic(MockRegistry.class);
             var matcherMockStatic = mockStatic(MatcherContext.class)) {
            
            mockStatic.when(() -> MockRegistry.getInvocationRecords(any(), any()))
                    .thenReturn(Collections.singletonList(record));
            
            matcherMockStatic.when(MatcherContext::consumeMatchers)
                    .thenReturn(Arrays.asList(intMatcher, stringMatcher));
            
            ContextualMocker.TimesVerificationMode timesMode = mock(ContextualMocker.TimesVerificationMode.class);
            
            VerificationMethodCaptureHandler<Dummy> handler =
                    new VerificationMethodCaptureHandler<>(mock, timesMode, contextId);
            
            ContextHolder.setContext(contextId);
            
            // Should find 0 matches because string matcher doesn't match
            Object[] testArgs = {42, "matching"};
            handler.invoke(mock, method, testArgs);
            
            verify(timesMode).verifyCountWithContext(eq(0), eq(method), eq(testArgs), eq(mock), eq(contextId), any());
        }
    }

    @Test
    void testVerificationMethodCaptureHandler_WithArgumentMatchersPartialMatcherList() throws Throwable {
        Dummy mock = new Dummy();
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        Method method = Dummy.class.getDeclaredMethod("processValues", int.class, String.class);
        
        InvocationRecord record = new InvocationRecord(mock, method, new Object[]{42, "test"}, contextId, false);
        
        // Create a matcher list with only one matcher (fewer than arguments)
        ArgumentMatcher<Integer> intMatcher = new ArgumentMatcher<Integer>() {
            @Override
            public boolean matches(Object argument) {
                return argument instanceof Integer && ((Integer) argument) == 42;
            }
        };
        
        try (var mockStatic = mockStatic(MockRegistry.class);
             var matcherMockStatic = mockStatic(MatcherContext.class)) {
            
            mockStatic.when(() -> MockRegistry.getInvocationRecords(any(), any()))
                    .thenReturn(Collections.singletonList(record));
            
            // Only one matcher for two arguments
            matcherMockStatic.when(MatcherContext::consumeMatchers)
                    .thenReturn(Collections.singletonList(intMatcher));
            
            ContextualMocker.TimesVerificationMode timesMode = mock(ContextualMocker.TimesVerificationMode.class);
            
            VerificationMethodCaptureHandler<Dummy> handler =
                    new VerificationMethodCaptureHandler<>(mock, timesMode, contextId);
            
            ContextHolder.setContext(contextId);
            
            // Should find 1 match: first arg matches with matcher, second arg compared directly
            Object[] testArgs = {42, "test"};
            handler.invoke(mock, method, testArgs);
            
            verify(timesMode).verifyCountWithContext(eq(1), eq(method), eq(testArgs), eq(mock), eq(contextId), any());
        }
    }

    @Test
    void testVerificationMethodCaptureHandler_NullArgs() throws Throwable {
        Dummy mock = new Dummy();
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        Method method = Dummy.class.getDeclaredMethod("foo");
        
        InvocationRecord record = new InvocationRecord(mock, method, new Object[0], contextId, false);
        
        try (var mockStatic = mockStatic(MockRegistry.class)) {
            mockStatic.when(() -> MockRegistry.getInvocationRecords(any(), any()))
                    .thenReturn(Collections.singletonList(record));
            
            ContextualMocker.TimesVerificationMode timesMode = mock(ContextualMocker.TimesVerificationMode.class);
            
            VerificationMethodCaptureHandler<Dummy> handler =
                    new VerificationMethodCaptureHandler<>(mock, timesMode, contextId);
            
            ContextHolder.setContext(contextId);
            
            // Test with null args - should be converted to empty array
            handler.invoke(mock, method, null);
            
            verify(timesMode).verifyCountWithContext(eq(1), eq(method), eq(new Object[0]), eq(mock), eq(contextId), any());
        }
    }

    @Test
    void testVerificationMethodCaptureHandler_MatchCountCorrect() throws Throwable {
        Dummy mock = new Dummy();
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        Method method = Dummy.class.getDeclaredMethod("getData", String.class);
        
        // Create multiple records with different argument combinations
        InvocationRecord record1 = new InvocationRecord(mock, method, new Object[]{"test"}, contextId, false);
        InvocationRecord record2 = new InvocationRecord(mock, method, new Object[]{"test"}, contextId, false);
        InvocationRecord nonMatchingRecord = new InvocationRecord(mock, method, new Object[]{"other"}, contextId, false);
        
        List<InvocationRecord> allRecords = Arrays.asList(record1, record2, nonMatchingRecord);
        
        try (var mockStatic = mockStatic(MockRegistry.class)) {
            mockStatic.when(() -> MockRegistry.getInvocationRecords(any(), any()))
                    .thenReturn(allRecords);
            
            ContextualMocker.TimesVerificationMode timesMode = mock(ContextualMocker.TimesVerificationMode.class);
            
            VerificationMethodCaptureHandler<Dummy> handler =
                    new VerificationMethodCaptureHandler<>(mock, timesMode, contextId);
            
            ContextHolder.setContext(contextId);
            
            Object[] testArgs = {"test"};
            handler.invoke(mock, method, testArgs);
            
            // Verify that the handler found 2 matching records (this tests the matching logic)
            verify(timesMode).verifyCountWithContext(eq(2), eq(method), eq(testArgs), eq(mock), eq(contextId), eq(allRecords));
        }
    }

    @Test
    void testVerificationMethodCaptureHandler_SecondInvocationDoesNotProcess() throws Throwable {
        Dummy mock = new Dummy();
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        Method method = Dummy.class.getDeclaredMethod("getData", String.class);
        
        try (var mockStatic = mockStatic(MockRegistry.class)) {
            mockStatic.when(() -> MockRegistry.getInvocationRecords(any(), any()))
                    .thenReturn(Collections.emptyList());
            
            ContextualMocker.TimesVerificationMode timesMode = mock(ContextualMocker.TimesVerificationMode.class);
            
            VerificationMethodCaptureHandler<Dummy> handler =
                    new VerificationMethodCaptureHandler<>(mock, timesMode, contextId);
            
            ContextHolder.setContext(contextId);
            
            Object[] testArgs = {"test"};
            
            // First invocation should process
            handler.invoke(mock, method, testArgs);
            verify(timesMode, times(1)).verifyCountWithContext(anyInt(), any(), any(), any(), any(), any());
            
            // Second invocation should not process (method already set)
            handler.invoke(mock, method, testArgs);
            // Still only called once
            verify(timesMode, times(1)).verifyCountWithContext(anyInt(), any(), any(), any(), any(), any());
        }
    }

    @Test
    void testVerificationMethodCaptureHandler_DifferentMethods() throws Throwable {
        Dummy mock = new Dummy();
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        Method getDataMethod = Dummy.class.getDeclaredMethod("getData", String.class);
        Method fooMethod = Dummy.class.getDeclaredMethod("foo");
        
        // Create records for different methods
        InvocationRecord getDataRecord = new InvocationRecord(mock, getDataMethod, new Object[]{"test"}, contextId, false);
        InvocationRecord fooRecord = new InvocationRecord(mock, fooMethod, new Object[0], contextId, false);
        
        List<InvocationRecord> allRecords = Arrays.asList(getDataRecord, fooRecord);
        
        try (var mockStatic = mockStatic(MockRegistry.class)) {
            mockStatic.when(() -> MockRegistry.getInvocationRecords(any(), any()))
                    .thenReturn(allRecords);
            
            ContextualMocker.TimesVerificationMode timesMode = mock(ContextualMocker.TimesVerificationMode.class);
            
            VerificationMethodCaptureHandler<Dummy> handler =
                    new VerificationMethodCaptureHandler<>(mock, timesMode, contextId);
            
            ContextHolder.setContext(contextId);
            
            // Verify foo method - should only match fooRecord
            handler.invoke(mock, fooMethod, new Object[0]);
            
            verify(timesMode).verifyCountWithContext(eq(1), eq(fooMethod), any(), eq(mock), eq(contextId), eq(allRecords));
        }
    }
}