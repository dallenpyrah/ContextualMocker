package com.contextualmocker.core;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MockRegistryTest {

    @Test
    void getAllInvocationRecordsReturnsRecords() throws Exception {
        Object mock = new Object();
        ContextID contextId = new StringContextId("ctx");
        Method method = Object.class.getMethod("toString");
        InvocationRecord record = new InvocationRecord(mock, method, new Object[0], contextId, false, List.of());
        MockRegistry.recordInvocation(record);

        List<InvocationRecord> all = MockRegistry.getAllInvocationRecords(mock);
        assertNotNull(all);
        assertTrue(all.contains(record));
    }

    @Test
    void resetStateClearsState() {
        Object mock = new Object();
        ContextID contextId = new StringContextId("ctx2");
        MockRegistry.setState(mock, contextId, "state");
        assertEquals("state", MockRegistry.getState(mock, contextId));
        MockRegistry.resetState(mock, contextId);
        assertNull(MockRegistry.getState(mock, contextId));
    }

    @Test
    void cleanUpStaleReferencesDoesNotThrow() {
        // This is hard to verify directly, but we can call it for coverage
        assertDoesNotThrow(MockRegistry::cleanUpStaleReferences);
    }
}