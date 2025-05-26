package com.contextualmocker.core;

import com.contextualmocker.matchers.ArgumentMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import static com.contextualmocker.core.ContextualMocker.*;
import static org.junit.jupiter.api.Assertions.*;

class ContextualMockerEdgeCasesTest {

    private static final Service mockService = mock(Service.class);

    @BeforeEach
    void setUp() {
        ContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        ContextHolder.clearContext();
    }

    @Test
    void stubbingWithNullContextThrows() {
        assertThrows(NullPointerException.class, () -> {
            given(mockService).forContext(null).when(() -> mockService.process("foo")).thenReturn("X");
        });
    }

    @Test
    void verifyWithNullContextThrows() {
        assertThrows(NullPointerException.class, () -> {
            verify(mockService).forContext(null).verify(times(1)).process("foo");
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
        Object invalidContext = new Object();
        assertThrows(ClassCastException.class, () -> {
            verify(mockService).forContext((ContextID) invalidContext).verify(times(1)).process("foo");
        });
    }

    @Test
    void usingArgumentMatcherForSingleArgumentWorks() {
        ContextID ctx = new StringContextId(UUID.randomUUID().toString());
        given(mockService).forContext(ctx).when(() -> mockService.process(ArgumentMatchers.any())).thenReturn("matched");
        assertEquals("matched", mockService.process("anything"));
    }

    @Test
    void callingMockMethodOutsideStubbingOrVerificationThrows() {
        assertThrows(IllegalStateException.class, () -> mockService.process("foo"));
    }

    @Test
    void clearContextCleansUpInvocations() {
        ContextID ctx = new StringContextId(UUID.randomUUID().toString());
        ContextHolder.setContext(ctx);
        mockService.process("foo");
        ContextHolder.clearContext();
        ContextHolder.setContext(ctx);
        assertThrows(AssertionError.class, () -> {
            verify(mockService).forContext(ctx).verify(times(1)).process("foo");
        });
    }

    @Test
    void mockIsGarbageCollectedWhenNoStrongReferences() throws Exception {
        WeakReference<Service> ref;
        {
            Service localMock = mock(Service.class);
            ref = new WeakReference<>(localMock);
        }
        for (int i = 0; i < 10 && ref.get() != null; i++) {
            System.gc();
            TimeUnit.MILLISECONDS.sleep(50);
        }
        assertNull(ref.get());
    }

}