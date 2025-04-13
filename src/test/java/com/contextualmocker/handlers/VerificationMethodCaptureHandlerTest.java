package com.contextualmocker.handlers;

import com.contextualmocker.core.ContextID;
import com.contextualmocker.core.ContextualMocker;
import com.contextualmocker.core.ContextHolder;
import com.contextualmocker.core.InvocationRecord;
import com.contextualmocker.core.MockRegistry;
import com.contextualmocker.matchers.ArgumentMatcher;
import com.contextualmocker.matchers.MatcherContext;
import com.contextualmocker.core.StringContextId;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VerificationMethodCaptureHandlerTest {

    static class Dummy {
        void foo() {}
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

            verify(timesMode).verifyCount(eq(1), eq(method), eq(args));
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

            verify(atLeastMode).verifyCount(eq(0), eq(method), eq(args));
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
}