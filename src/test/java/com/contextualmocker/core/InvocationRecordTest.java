package com.contextualmocker.core;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InvocationRecordTest {

    @Test
    void gettersReturnExpectedValues() throws Exception {
        Object mock = new Object();
        ContextID contextId = new StringContextId("ctx");
        Method method = Object.class.getMethod("toString");
        Object[] args = new Object[] { "a" };
        List matchers = List.of();

        InvocationRecord record = new InvocationRecord(mock, method, args, contextId, false, matchers);

        assertNotNull(record.getMockRef());
        assertEquals(mock, record.getMock());
        assertEquals(method, record.getMethod());
        assertArrayEquals(args, record.getArguments());
        assertEquals(contextId, record.getContextId());
        assertNotNull(record.getTimestamp());
        assertTrue(record.getThreadId() > 0);
        assertEquals(matchers, record.getMatchers());
        assertFalse(record.isVerified());
        record.markVerified();
        assertTrue(record.isVerified());
        assertNotNull(record.toString());
    }
}