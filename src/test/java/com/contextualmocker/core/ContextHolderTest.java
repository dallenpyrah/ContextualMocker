package com.contextualmocker.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

public class ContextHolderTest {

    private final ContextID context1 = new StringContextId("testContext1");
    private final ContextID context2 = new StringContextId("testContext2");

    private MockedStatic<MockRegistry> mockRegistryStatic;

    @BeforeEach
    void setUp() {
        ContextHolder.clearContext();
        mockRegistryStatic = Mockito.mockStatic(MockRegistry.class);
    }

    @AfterEach
    void tearDown() {
        ContextHolder.clearContext();
        if (mockRegistryStatic != null) {
            mockRegistryStatic.close();
        }
    }

    @Test
    void testSetAndGetContext() {
        assertThrows(IllegalStateException.class, ContextHolder::getContext,
                "Getting context when none is set should throw IllegalStateException");

        ContextHolder.setContext(context1);
        assertEquals(context1, ContextHolder.getContext(), "Should retrieve the set context");

        ContextHolder.setContext(context2);
        assertEquals(context2, ContextHolder.getContext(), "Should retrieve the newly set context");
    }

    @Test
    void testClearContext() {
        ContextHolder.setContext(context1);
        assertEquals(context1, ContextHolder.getContext());

        ContextHolder.clearContext();

        assertThrows(IllegalStateException.class, ContextHolder::getContext,
                "Getting context after clearing should throw IllegalStateException");
    }

     @Test
    void testClearContextWhenNoneSet() {
        assertDoesNotThrow(ContextHolder::clearContext);
    }

    @Test
    void testClearContextTriggersRegistryCleanup() {
        ContextHolder.setContext(context1);
        assertEquals(context1, ContextHolder.getContext());

        ContextHolder.clearContext();

        mockRegistryStatic.verify(() -> MockRegistry.clearInvocationsForContext(context1), Mockito.times(1));

        mockRegistryStatic.verify(() -> MockRegistry.clearInvocationsForContext(context2), Mockito.never());
    }

    // Note: Testing InheritableThreadLocal propagation requires actual thread creation,
    // which is more complex and might be deferred to integration/concurrency tests (Plan lines 246-248).
    // The basic set/get/clear functionality is covered here.
}