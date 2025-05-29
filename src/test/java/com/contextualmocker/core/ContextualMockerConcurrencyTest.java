package com.contextualmocker.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;

import static com.contextualmocker.core.ContextualMocker.*;
import static com.contextualmocker.matchers.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency and parallel execution tests for ContextualMocker.
 * Covers:
 * - Concurrent stubbing, invocation, and verification from multiple threads.
 * - Context isolation and thread safety.
 * - Stateful mocking and argument matching in parallel.
 * - TTL/expiration behavior under concurrency.
 */
public class ContextualMockerConcurrencyTest {

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
    void concurrentStubbingAndInvocation_differentContexts_isIsolatedAndThreadSafe() throws Exception {
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        List<Future<String>> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            ContextID ctx = new StringContextId(UUID.randomUUID().toString());
            results.add(executor.submit(() -> {
                String stubValue = "result-" + idx;
                when(mockService, ctx, () -> mockService.process("input-" + idx))
                    .thenReturn(stubValue);

                barrier.await();

                try (ContextScope scope = scopedContext(ctx)) {
                    return mockService.process("input-" + idx);
                }
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
        assertEquals(expected, actual);
    }

    @Test
    void concurrentInvocationAndVerification_statefulMockingAndArgumentMatching() throws Exception {
        int threadCount = 6;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger state = new AtomicInteger(0);
        ContextID contextA = new StringContextId(UUID.randomUUID().toString());
        ContextID contextB = new StringContextId(UUID.randomUUID().toString());

        when(mockService, contextA, () -> mockService.process(any()))
            .thenAnswer((contextId, mock, method, args) -> "A-" + state.incrementAndGet());

        when(mockService, contextB, () -> mockService.process(eq("special")))
            .thenReturn("B-special");

        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            results.add(executor.submit(() -> {
                if (idx % 2 == 0) {
                    try (ContextScope scope = scopedContext(contextA)) {
                        return mockService.process("foo" + idx);
                    }
                } else {
                    try (ContextScope scope = scopedContext(contextB)) {
                        return mockService.process(idx == 1 ? "special" : "other");
                    }
                }
            }));
        }

        List<String> actual = new ArrayList<>();
        for (Future<String> f : results) {
            actual.add(f.get(2, TimeUnit.SECONDS));
        }
        executor.shutdownNow();

        assertTrue(actual.contains("A-1"));
        assertTrue(actual.contains("A-2"));
        assertTrue(actual.contains("A-3"));
        assertTrue(actual.contains("B-special"));
        assertTrue(actual.contains(null));
    }

    @Test
    void concurrentRuleExpiration_isThreadSafeAndRemovesExpiredRules() throws Exception {
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        String method = "expiring";
        ContextID contextA = new StringContextId(UUID.randomUUID().toString());
        
        when(mockService, contextA, () -> mockService.process(method))
            .ttlMillis(150)
            .thenReturn("will-expire");

        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            results.add(executor.submit(() -> {
                barrier.await();
                try (ContextScope scope = scopedContext(contextA)) {
                    if (idx < 2) {
                        return mockService.process(method);
                    } else {
                        TimeUnit.MILLISECONDS.sleep(200);
                        return mockService.process(method);
                    }
                }
            }));
        }

        int before = 0, after = 0;
        for (int i = 0; i < threadCount; i++) {
            String val = results.get(i).get(2, TimeUnit.SECONDS);
            if ("will-expire".equals(val))
                before++;
            else if (val == null)
                after++;
        }
        executor.shutdownNow();
        assertEquals(2, before);
        assertEquals(2, after);
    }
}