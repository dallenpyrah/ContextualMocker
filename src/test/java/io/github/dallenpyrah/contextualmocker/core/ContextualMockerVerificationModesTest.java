package io.github.dallenpyrah.contextualmocker.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static io.github.dallenpyrah.contextualmocker.core.ContextualMocker.*;
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
        ContextualVerificationMode mode = atLeastOnce();
        assertNotNull(mode);
        assertTrue(mode instanceof ContextualVerificationMode);
    }

    @Test
    void atLeastReturnsVerificationMode() {
        ContextualVerificationMode mode = atLeast(2);
        assertNotNull(mode);
        assertTrue(mode instanceof ContextualVerificationMode);
    }

    @Test
    void atMostReturnsVerificationMode() {
        ContextualVerificationMode mode = atMost(3);
        assertNotNull(mode);
        assertTrue(mode instanceof ContextualVerificationMode);
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
        // Don't clear context - interactions should still be there
        
        assertThrows(AssertionError.class, () -> verifyNoInteractions(mockService, contextId));
        ContextHolder.clearContext();
    }
}