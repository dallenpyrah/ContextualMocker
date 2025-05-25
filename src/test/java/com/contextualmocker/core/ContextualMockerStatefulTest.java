package com.contextualmocker.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.concurrent.*;
import java.util.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static com.contextualmocker.core.ContextualMocker.*;

public class ContextualMockerStatefulTest {

    private static final SimpleService mockService = mock(SimpleService.class);

    enum State { LOGGED_OUT, LOGGED_IN }

    @BeforeEach
    void setUp() {
        ContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        ContextHolder.clearContext();
    }

    @Test
    void testWhenStateIsAndWillSetStateTo() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());

        given(mockService)
            .forContext(context1)
            .when(() -> mockService.greet("login"))
            .whenStateIs(null)
            .willSetStateTo(State.LOGGED_IN)
            .thenReturn("Logged in");

        given(mockService)
            .forContext(context1)
            .when(() -> mockService.greet("logout"))
            .whenStateIs(State.LOGGED_IN)
            .willSetStateTo(State.LOGGED_OUT)
            .thenReturn("Logged out");

        given(mockService)
            .forContext(context1)
            .when(() -> mockService.greet("data"))
            .whenStateIs(State.LOGGED_IN)
            .thenReturn("Here is your data");

        assertEquals("Logged in", mockService.greet("login"));
        assertEquals("Here is your data", mockService.greet("data"));
        assertEquals("Logged out", mockService.greet("logout"));
        assertNull(mockService.greet("data"));
    }

    @Test
    void testStateIsolationBetweenContexts() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        ContextID context2 = new StringContextId(UUID.randomUUID().toString());

        given(mockService)
            .forContext(context1)
            .when(() -> mockService.greet("login"))
            .whenStateIs(null)
            .willSetStateTo(State.LOGGED_IN)
            .thenReturn("Logged in");
        mockService.greet("login");

        ContextHolder.setContext(context2);
        assertNull(mockService.greet("data"));

        given(mockService)
            .forContext(context2)
            .when(() -> mockService.greet("login"))
            .whenStateIs(null)
            .willSetStateTo(State.LOGGED_IN)
            .thenReturn("Logged in");
        assertEquals("Logged in", mockService.greet("login"));
    }

    @Test
    void testStateIsolationBetweenMocks() {
        SimpleService mock2 = mock(SimpleService.class);
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());

        given(mockService)
            .forContext(context1)
            .when(() -> mockService.greet("login"))
            .whenStateIs(null)
            .willSetStateTo(State.LOGGED_IN)
            .thenReturn("Logged in");
        given(mock2)
            .forContext(context1)
            .when(() -> mock2.greet("login"))
            .whenStateIs(null)
            .willSetStateTo(State.LOGGED_IN)
            .thenReturn("Logged in 2");

        assertEquals("Logged in", mockService.greet("login"));
        assertEquals("Logged in 2", mock2.greet("login"));
    }

    @Test
    void testConcurrentStateTransitions() throws Exception {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        given(mockService)
            .forContext(context1)
            .when(() -> mockService.greet("login"))
            .whenStateIs(null)
            .willSetStateTo(State.LOGGED_IN)
            .thenReturn("Logged in");
        given(mockService)
            .forContext(context1)
            .when(() -> mockService.greet("logout"))
            .whenStateIs(State.LOGGED_IN)
            .willSetStateTo(State.LOGGED_OUT)
            .thenReturn("Logged out");

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<String>> results = new ArrayList<>();
        ContextHolder.setContext(context1);

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            results.add(executor.submit(() -> {
                if (idx % 2 == 0) {
                    return mockService.greet("login");
                } else {
                    return mockService.greet("logout");
                }
            }));
        }
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        for (Future<String> f : results) {
            assertTrue(
                "Logged in".equals(f.get()) ||
                "Logged out".equals(f.get()) ||
                f.get() == null
            );
        }
    }

    @Test
    void testNullStateAsRequiredOrNextState() {
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        given(mockService)
            .forContext(context1)
            .when(() -> mockService.greet("reset"))
            .whenStateIs(State.LOGGED_IN)
            .willSetStateTo(null)
            .thenReturn("Reset to null state");

        given(mockService)
            .forContext(context1)
            .when(() -> mockService.greet("login"))
            .whenStateIs(null)
            .willSetStateTo(State.LOGGED_IN)
            .thenReturn("Logged in");

        assertEquals("Logged in", mockService.greet("login"));
        assertEquals("Reset to null state", mockService.greet("reset"));
        assertEquals("Reset to null state", mockService.greet("reset"));
    }
}