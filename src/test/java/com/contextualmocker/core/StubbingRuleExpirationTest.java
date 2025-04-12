package com.contextualmocker.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.contextualmocker.core.ContextualMocker.*;

import java.util.concurrent.TimeUnit;


public class StubbingRuleExpirationTest {

    private Service mockService;
    private final ContextID context1 = new StringContextId("context1");
    private final ContextID context2 = new StringContextId("context2");

    @BeforeEach
    void setUp() {
        mockService = mock(Service.class);
        ContextHolder.clearContext();
    }

    @Test
    void testRuleWithTtlIsMatchedBeforeExpiration() {
        given(mockService)
                .forContext(context1)
                .when(() -> mockService.process("data"))
                .ttlMillis(500)
                .thenReturn("processed_ttl");

        ContextHolder.setContext(context1); // Set context for interaction
        String result = mockService.process("data");

        assertEquals("processed_ttl", result, "Rule with TTL should be matched before expiration");
    }

    @Test
    void testRuleWithTtlIsNotMatchedAfterExpiration() throws InterruptedException {
        given(mockService)
                .forContext(context1)
                .when(() -> mockService.process("expire_me"))
                .ttlMillis(100)
                .thenReturn("expired_value");

        ContextHolder.setContext(context1); // Set context for interaction
        assertEquals("expired_value", mockService.process("expire_me"), "Rule should match initially");

        TimeUnit.MILLISECONDS.sleep(150);

        ContextHolder.setContext(context1); // Set context for interaction
        String result = mockService.process("expire_me");

        assertNull(result, "Rule with TTL should not be matched after expiration");

        // Verify the rule was removed (optional check, implicitly tested above)
        ContextHolder.setContext(context1);
        assertNull(mockService.process("expire_me"), "Expired rule should remain removed");
    }

    @Test
    void testRuleWithoutTtlDoesNotExpire() throws InterruptedException {
        given(mockService)
                .forContext(context2)
                .when(() -> mockService.process("persistent"))
                .thenReturn("persistent_value");

        ContextHolder.setContext(context2); // Set context for interaction
        assertEquals("persistent_value", mockService.process("persistent"), "Rule without TTL should match initially");

        TimeUnit.MILLISECONDS.sleep(150);

        ContextHolder.setContext(context2); // Set context for interaction
        String result = mockService.process("persistent");

        assertEquals("persistent_value", result, "Rule without TTL should not expire");
    }

    @Test
    void testMultipleRulesWithDifferentTtls() throws InterruptedException {
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

        ContextHolder.setContext(context1); // Set context for interactions
        assertEquals("short_lived", mockService.process("short"));
        assertEquals("long_lived", mockService.process("long"));

        TimeUnit.MILLISECONDS.sleep(150);

        ContextHolder.setContext(context1); // Set context for interactions
        assertNull(mockService.process("short"), "Short rule should have expired");
        assertEquals("long_lived", mockService.process("long"), "Long rule should still be active");

        TimeUnit.MILLISECONDS.sleep(400); // Total wait > 500ms

        ContextHolder.setContext(context1); // Set context for interactions
        assertNull(mockService.process("short"), "Short rule should remain expired");
        assertNull(mockService.process("long"), "Long rule should have expired");

    }
}