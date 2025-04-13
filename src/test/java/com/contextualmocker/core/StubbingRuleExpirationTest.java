package com.contextualmocker.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.contextualmocker.core.ContextualMocker.*;

import java.util.concurrent.TimeUnit;
import java.util.UUID;

public class StubbingRuleExpirationTest {

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
    void testRuleWithTtlIsMatchedBeforeExpiration() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        given(mockService)
                .forContext(context1)
                .when(() -> mockService.process("data"))
                .ttlMillis(500)
                .thenReturn("processed_ttl");

        ContextHolder.setContext(context1);
        String result = mockService.process("data");

        assertEquals("processed_ttl", result);
    }

    @Test
    void testRuleWithTtlIsNotMatchedAfterExpiration() throws InterruptedException {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        given(mockService)
                .forContext(context1)
                .when(() -> mockService.process("expire_me"))
                .ttlMillis(100)
                .thenReturn("expired_value");

        ContextHolder.setContext(context1);
        assertEquals("expired_value", mockService.process("expire_me"));

        TimeUnit.MILLISECONDS.sleep(150);

        ContextHolder.setContext(context1);
        String result = mockService.process("expire_me");

        assertNull(result);

        ContextHolder.setContext(context1);
        assertNull(mockService.process("expire_me"));
    }

    @Test
    void testRuleWithoutTtlDoesNotExpire() throws InterruptedException {
        ContextID context2 = new StringContextId(UUID.randomUUID().toString());
        given(mockService)
                .forContext(context2)
                .when(() -> mockService.process("persistent"))
                .thenReturn("persistent_value");

        ContextHolder.setContext(context2);
        assertEquals("persistent_value", mockService.process("persistent"));

        TimeUnit.MILLISECONDS.sleep(150);

        ContextHolder.setContext(context2);
        String result = mockService.process("persistent");

        assertEquals("persistent_value", result);
    }

    @Test
    void testMultipleRulesWithDifferentTtls() throws InterruptedException {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        given(mockService)
                .forContext(context1)
                .when(() -> mockService.process("short"))
                .ttlMillis(100)
                .thenReturn("short_lived");
        given(mockService)
                .forContext(context1)
                .when(() -> mockService.process("long"))
                .ttlMillis(500)
                .thenReturn("long_lived");

        ContextHolder.setContext(context1);
        assertEquals("short_lived", mockService.process("short"));
        assertEquals("long_lived", mockService.process("long"));

        TimeUnit.MILLISECONDS.sleep(150);

        ContextHolder.setContext(context1);
        assertNull(mockService.process("short"));
        assertEquals("long_lived", mockService.process("long"));

        TimeUnit.MILLISECONDS.sleep(400);

        ContextHolder.setContext(context1);
        assertNull(mockService.process("short"));
        assertNull(mockService.process("long"));
    }
}