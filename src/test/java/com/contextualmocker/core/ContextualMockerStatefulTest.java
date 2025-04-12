package com.contextualmocker.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.concurrent.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static com.contextualmocker.core.ContextualMocker.*;

public class ContextualMockerStatefulTest {

    private final ContextID context1 = new StringContextId("context1");
    private final ContextID context2 = new StringContextId("context2");
    private SimpleService mockService;

    enum State { LOGGED_OUT, LOGGED_IN }

    @BeforeEach
    void setUp() {
        mockService = mock(SimpleService.class);
        ContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        ContextHolder.clearContext();
    }

    @Test
    void testWhenStateIsAndWillSetStateTo() {
        ContextHolder.setContext(context1);

        // Initial state is null
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

        // Login
        assertEquals("Logged in", mockService.greet("login"));
        // Now state should be LOGGED_IN
        assertEquals("Here is your data", mockService.greet("data"));
        // Logout
        assertEquals("Logged out", mockService.greet("logout"));
        // Now state should be LOGGED_OUT
        assertNull(mockService.greet("data"));

        ContextHolder.clearContext();
    }

    @Test
    void testStateIsolationBetweenContexts() {
        // Context 1 logs in, context 2 remains logged out
        ContextHolder.setContext(context1);
        given(mockService)
            .forContext(context1)
            .when(() -> mockService.greet("login"))
            .whenStateIs(null)
            .willSetStateTo(State.LOGGED_IN)
            .thenReturn("Logged in");
        mockService.greet("login");

        ContextHolder.setContext(context2);
        // Should not be logged in in context2
        assertNull(mockService.greet("data"));
        // Logging in context2
        given(mockService)
            .forContext(context2)
            .when(() -> mockService.greet("login"))
            .whenStateIs(null)
            .willSetStateTo(State.LOGGED_IN)
            .thenReturn("Logged in");
        assertEquals("Logged in", mockService.greet("login"));

        ContextHolder.clearContext();
    }

    @Test
    void testStateIsolationBetweenMocks() {
        SimpleService mock2 = mock(SimpleService.class);

        ContextHolder.setContext(context1);
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

        ContextHolder.clearContext();
    }

    @Test
    void testConcurrentStateTransitions() throws Exception {
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

        // Alternate login/logout in parallel
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

        // Just check that no exceptions and state transitions occurred
        for (Future<String> f : results) {
            assertTrue(
                "Logged in".equals(f.get()) ||
                "Logged out".equals(f.get()) ||
                f.get() == null
            );
        }
        ContextHolder.clearContext();
    }

    @Test
    void testNullStateAsRequiredOrNextState() {
        ContextHolder.setContext(context1);
        given(mockService)
            .forContext(context1)
            .when(() -> mockService.greet("reset"))
            .whenStateIs(State.LOGGED_IN)
            .willSetStateTo(null)
            .thenReturn("Reset to null state");

        // Set state to LOGGED_IN first
        given(mockService)
            .forContext(context1)
            .when(() -> mockService.greet("login"))
            .whenStateIs(null)
            .willSetStateTo(State.LOGGED_IN)
            .thenReturn("Logged in");

        assertEquals("Logged in", mockService.greet("login"));
        assertEquals("Reset to null state", mockService.greet("reset"));
        // Now state is null again
        assertEquals("Reset to null state", mockService.greet("reset"));
        ContextHolder.clearContext();
    }
}