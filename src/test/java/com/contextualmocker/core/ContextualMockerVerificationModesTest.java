package com.contextualmocker.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static com.contextualmocker.core.ContextualMocker.*;
import static org.junit.jupiter.api.Assertions.*;

public class ContextualMockerVerificationModesTest {

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
    void atLeastOnceReturnsVerificationMode() {
        ContextualMocker.ContextualVerificationMode mode = atLeastOnce();
        assertNotNull(mode);
        assertTrue(mode instanceof ContextualMocker.ContextualVerificationMode);
    }

    @Test
    void atLeastReturnsVerificationMode() {
        ContextualMocker.ContextualVerificationMode mode = atLeast(2);
        assertNotNull(mode);
        assertTrue(mode instanceof ContextualMocker.ContextualVerificationMode);
    }

    @Test
    void atMostReturnsVerificationMode() {
        ContextualMocker.ContextualVerificationMode mode = atMost(3);
        assertNotNull(mode);
        assertTrue(mode instanceof ContextualMocker.ContextualVerificationMode);
    }

    @Test
    void verifyNoInteractionsDoesNotThrowOnUnusedMock() {
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        assertDoesNotThrow(() -> verifyNoInteractions(mockService, contextId));
    }

    @Test
    void verifyNoInteractionsThrowsOnUsedMock() {
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        ContextHolder.setContext(contextId);
        mockService.process("test");
        assertThrows(AssertionError.class, () -> verifyNoInteractions(mockService, contextId));
    }
}