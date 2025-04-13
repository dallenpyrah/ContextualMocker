package com.contextualmocker.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ContextualMockerVerificationModesTest {

    @Test
    void atLeastOnceReturnsVerificationMode() {
        ContextualMocker.ContextualVerificationMode mode = ContextualMocker.atLeastOnce();
        assertNotNull(mode);
        assertTrue(mode instanceof ContextualMocker.ContextualVerificationMode);
    }

    @Test
    void atLeastReturnsVerificationMode() {
        ContextualMocker.ContextualVerificationMode mode = ContextualMocker.atLeast(2);
        assertNotNull(mode);
        assertTrue(mode instanceof ContextualMocker.ContextualVerificationMode);
    }

    @Test
    void atMostReturnsVerificationMode() {
        ContextualMocker.ContextualVerificationMode mode = ContextualMocker.atMost(3);
        assertNotNull(mode);
        assertTrue(mode instanceof ContextualMocker.ContextualVerificationMode);
    }

    @Test
    void verifyNoInteractionsDoesNotThrowOnUnusedMock() {
        Service mock = ContextualMocker.mock(Service.class);
        ContextID contextId = new StringContextId("test-context");
        assertDoesNotThrow(() -> ContextualMocker.verifyNoInteractions(mock, contextId));
    }

    @Test
    void verifyNoInteractionsThrowsOnUsedMock() {
        Service mock = ContextualMocker.mock(Service.class);
        ContextID contextId = new StringContextId("test-context");
        ContextHolder.setContext(contextId);
        mock.process("test"); // use a real Service method to ensure interaction is recorded
        assertThrows(AssertionError.class, () -> ContextualMocker.verifyNoInteractions(mock, contextId));
        ContextHolder.clearContext();
    }
}