package io.github.dallenpyrah.contextualmocker.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ContextHolderTest {

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
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        ContextID context2 = new StringContextId(UUID.randomUUID().toString());

        assertThrows(IllegalStateException.class, ContextHolder::getContext);

        ContextHolder.setContext(context1);
        assertEquals(context1, ContextHolder.getContext());

        ContextHolder.setContext(context2);
        assertEquals(context2, ContextHolder.getContext());
    }

    @Test
    void testClearContext() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        ContextHolder.setContext(context1);
        assertEquals(context1, ContextHolder.getContext());

        ContextHolder.clearContext();

        assertThrows(IllegalStateException.class, ContextHolder::getContext);
    }

     @Test
    void testClearContextWhenNoneSet() {
        assertDoesNotThrow(ContextHolder::clearContext);
    }

    @Test
    void testClearContextTriggersRegistryCleanup() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        ContextID context2 = new StringContextId(UUID.randomUUID().toString());
        ContextHolder.setContext(context1);
        assertEquals(context1, ContextHolder.getContext());

        ContextHolder.clearContext();

        mockRegistryStatic.verify(() -> MockRegistry.clearInvocationsForContext(context1), Mockito.times(1));

        mockRegistryStatic.verify(() -> MockRegistry.clearInvocationsForContext(context2), Mockito.never());
    }
}