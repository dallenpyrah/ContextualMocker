package io.github.dallenpyrah.contextualmocker.core;

import io.github.dallenpyrah.contextualmocker.matchers.ArgumentMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import static io.github.dallenpyrah.contextualmocker.core.ContextualMocker.*;
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
            when(mockService, null, () -> mockService.process("foo")).thenReturn("X");
        });
    }

    @Test
    void verifyWithNullContextThrows() {
        assertThrows(NullPointerException.class, () -> {
            verify(mockService, null, times(1), () -> mockService.process("foo"));
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
        
        when(mockService, ctx, () -> mockService.process(ArgumentMatchers.any()))
            .thenReturn("matched");
            
        try (ContextScope scope = scopedContext(ctx)) {
            assertEquals("matched", mockService.process("anything"));
        }
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
        
        // Verify the invocation exists before clearing
        verifyOnce(mockService, ctx, () -> mockService.process("foo"));
        
        ContextHolder.clearContext();
        
        // After clearing context, invocations should be gone
        // So verification should pass because there are no invocations to count
        verifyNoInteractions(mockService, ctx);
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