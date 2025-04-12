package com.contextualmocker.core;

import com.contextualmocker.matchers.ArgumentMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import static com.contextualmocker.core.ContextualMocker.*;
import static org.junit.jupiter.api.Assertions.*;

class ContextualMockerEdgeCasesTest {

    private static final ContextID CTX1 = new StringContextId("CTX1");
    private static final ContextID CTX2 = new StringContextId("CTX2");

    @AfterEach
    void cleanup() {
        ContextHolder.clearContext();
    }

    // --- Error Handling & API Misuse ---


    @Test
    void stubbingWithNullContextThrows() {
        Service mock = mock(Service.class);
        assertThrows(NullPointerException.class, () -> {
            given(mock).forContext(null).when(() -> mock.process("foo")).thenReturn("X");
        });
    }

    @Test
    void verifyWithNullContextThrows() {
        Service mock = mock(Service.class);
        assertThrows(NullPointerException.class, () -> {
            verify(mock).forContext(null).verify(times(1)).process("foo");
        });
    }


    @Test
    void givenWithNullMockThrows() {
        assertThrows(NullPointerException.class, () -> {
            given(null);
        });
    }

    @Test
    void verifyWithNullMockThrows() {
        assertThrows(NullPointerException.class, () -> {
            verify(null);
        });
    }

    @Test
    void mockWithNullClassThrows() {
        assertThrows(NullPointerException.class, () -> {
            mock(null);
        });
    }

    @Test
    void forContextWithInvalidContextIdTypeThrows() {
        Service mock = mock(Service.class);
        Object invalidContext = new Object();
        assertThrows(ClassCastException.class, () -> {
            verify(mock).forContext((ContextID) invalidContext).verify(times(1)).process("foo");
        });
    }

    @Test
    void usingArgumentMatcherForSingleArgumentWorks() {
        Service mock = mock(Service.class);
        given(mock).forContext(CTX1).when(() -> mock.process(ArgumentMatchers.any())).thenReturn("matched");
        ContextHolder.setContext(CTX1);
        assertEquals("matched", mock.process("anything"));
    }

    @Test
    void callingMockMethodOutsideStubbingOrVerificationThrows() {
        Service mock = mock(Service.class);
        // No stubbing or verification context
        assertThrows(IllegalStateException.class, () -> mock.process("foo"));
    }

    // --- Resource Cleanup & Memory Management ---

    @Test
    void clearContextCleansUpInvocations() {
        Service mock = mock(Service.class);
        ContextHolder.setContext(CTX1);
        mock.process("foo");
        ContextHolder.clearContext();
        // After clearing, invocations for CTX1 should be gone
        ContextHolder.setContext(CTX1);
        assertThrows(AssertionError.class, () -> {
            verify(mock).forContext(CTX1).verify(times(1)).process("foo");
        });
    }

    @Test
    void mockIsGarbageCollectedWhenNoStrongReferences() throws Exception {
        WeakReference<Service> ref;
        {
            Service mock = mock(Service.class);
            ref = new WeakReference<>(mock);
        }
        // Try to force GC
        for (int i = 0; i < 10 && ref.get() != null; i++) {
            System.gc();
            TimeUnit.MILLISECONDS.sleep(50);
        }
        assertNull(ref.get(), "Mock should be garbage collected when no strong references exist");
    }

}