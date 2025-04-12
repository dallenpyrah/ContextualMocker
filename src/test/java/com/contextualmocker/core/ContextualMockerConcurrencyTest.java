package com.contextualmocker.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.contextualmocker.core.ContextualMocker.*;
import static com.contextualmocker.matchers.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency and parallel execution tests for ContextualMocker.
 * Covers:
 *  - Concurrent stubbing, invocation, and verification from multiple threads.
 *  - Context isolation and thread safety.
 *  - Stateful mocking and argument matching in parallel.
 *  - TTL/expiration behavior under concurrency.
 */
public class ContextualMockerConcurrencyTest {

    private Service mockService;
    private final ContextID contextA = new StringContextId("A");
    private final ContextID contextB = new StringContextId("B");

    @BeforeEach
    void setUp() {
        mockService = mock(Service.class);
        ContextHolder.clearContext();
    }

    @Test
    void concurrentStubbingAndInvocation_differentContexts_isIsolatedAndThreadSafe() throws Exception {
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        List<Future<String>> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            results.add(executor.submit(() -> {
                ContextID ctx = (idx % 2 == 0) ? contextA : contextB;
                String stubValue = "result-" + idx;
                // Each thread stubs its own value for its context
                given(mockService)
                        .forContext(ctx)
                        .when(() -> mockService.process("input-" + idx))
                        .thenReturn(stubValue);

                barrier.await(); // synchronize start

                ContextHolder.setContext(ctx);
                String result = mockService.process("input-" + idx);
                ContextHolder.clearContext();
                return result;
            }));
        }

        Set<String> expected = new HashSet<>();
        for (int i = 0; i < threadCount; i++) {
            expected.add("result-" + i);
        }

        Set<String> actual = new HashSet<>();
        for (Future<String> f : results) {
            actual.add(f.get(2, TimeUnit.SECONDS));
        }
        executor.shutdownNow();
        assertEquals(expected, actual, "Each thread should get its own stubbed value in its context");
    }

    @Test
    void concurrentInvocationAndVerification_statefulMockingAndArgumentMatching() throws Exception {
        int threadCount = 6;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger state = new AtomicInteger(0);

        // Stubbing with argument matcher and stateful answer
        given(mockService)
                .forContext(contextA)
                .when(() -> mockService.process(any()))
                .thenAnswer((contextId, mock, method, args) -> "A-" + state.incrementAndGet());

        given(mockService)
                .forContext(contextB)
                .when(() -> mockService.process(eq("special")))
                .thenReturn("B-special");

        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            results.add(executor.submit(() -> {
                if (idx % 2 == 0) {
                    ContextHolder.setContext(contextA);
                    String res = mockService.process("foo" + idx);
                    ContextHolder.clearContext();
                    return res;
                } else {
                    ContextHolder.setContext(contextB);
                    String res = mockService.process(idx == 1 ? "special" : "other");
                    ContextHolder.clearContext();
                    return res;
                }
            }));
        }

        List<String> actual = new ArrayList<>();
        for (Future<String> f : results) {
            actual.add(f.get(2, TimeUnit.SECONDS));
        }
        executor.shutdownNow();

        // Check stateful increments and argument matching
        assertTrue(actual.contains("A-1"));
        assertTrue(actual.contains("A-2"));
        assertTrue(actual.contains("A-3"));
        assertTrue(actual.contains("B-special"));
        // "other" in contextB should return null (no stub)
        assertTrue(actual.contains(null));
    }

    @Test
    void concurrentRuleExpiration_isThreadSafeAndRemovesExpiredRules() throws Exception {
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        String method = "expiring";
        // Rule with short TTL
        given(mockService)
                .forContext(contextA)
                .when(() -> mockService.process(method))
                .ttlMillis(150)
                .thenReturn("will-expire");

        // All threads will try to invoke the method, some before and some after expiration
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            results.add(executor.submit(() -> {
                barrier.await();
                ContextHolder.setContext(contextA);
                if (idx < 2) {
                    // Before expiration
                    String res = mockService.process(method);
                    ContextHolder.clearContext();
                    return res;
                } else {
                    // Wait for expiration
                    TimeUnit.MILLISECONDS.sleep(200);
                    String res = mockService.process(method);
                    ContextHolder.clearContext();
                    return res;
                }
            }));
        }

        int before = 0, after = 0;
        for (int i = 0; i < threadCount; i++) {
            String val = results.get(i).get(2, TimeUnit.SECONDS);
            if ("will-expire".equals(val)) before++;
            else if (val == null) after++;
        }
        executor.shutdownNow();
        assertEquals(2, before, "Two threads should get the value before expiration");
        assertEquals(2, after, "Two threads should get null after expiration");
    }
}