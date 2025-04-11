package com.contextualmocker;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

final class MockRegistry {

    // Structure: Mock Instance (WeakRef) -> ContextID -> List of Rules
    private static final ConcurrentMap<WeakReference<Object>, ConcurrentMap<ContextID, List<StubbingRule>>> stubbingRules =
            new ConcurrentHashMap<>();

    // Structure: Mock Instance (WeakRef) -> ContextID -> Queue of Invocations
    private static final ConcurrentMap<WeakReference<Object>, ConcurrentMap<ContextID, Queue<InvocationRecord>>> invocationRecords =
            new ConcurrentHashMap<>();

    private MockRegistry() {
        // Static utility class
    }

    static void addStubbingRule(Object mock, ContextID contextId, StubbingRule rule) {
        WeakReference<Object> mockRef = findWeakReference(mock, stubbingRules);
        if (mockRef == null) {
            mockRef = new WeakReference<>(mock);
        }

        stubbingRules
                .computeIfAbsent(mockRef, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(contextId, k -> new CopyOnWriteArrayList<>())
                .add(0, rule); // Add to the beginning for LIFO matching
    }

    static StubbingRule findStubbingRule(Object mock, ContextID contextId, Method method, Object[] arguments) {
        WeakReference<Object> mockRef = findWeakReference(mock, stubbingRules);
        if (mockRef == null) {
            return null;
        }

        ConcurrentMap<ContextID, List<StubbingRule>> contextMap = stubbingRules.get(mockRef);
        if (contextMap == null) {
            return null;
        }

        List<StubbingRule> rules = contextMap.get(contextId);
        if (rules == null) {
            return null;
        }

        // Find the first matching rule (LIFO order)
        for (StubbingRule rule : rules) {
            if (rule.matches(method, arguments)) {
                return rule;
            }
        }

        return null;
    }

    static void recordInvocation(InvocationRecord record) {
        Object mock = record.getMock();
        if (mock == null) {
            return; // Mock was GC'd
        }
        ContextID contextId = record.getContextId();
        WeakReference<Object> mockRef = findWeakReference(mock, invocationRecords);
         if (mockRef == null) {
            mockRef = new WeakReference<>(mock);
        }


        invocationRecords
                .computeIfAbsent(mockRef, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(contextId, k -> new ConcurrentLinkedQueue<>())
                .offer(record);
    }

    static List<InvocationRecord> getInvocationRecords(Object mock, ContextID contextId) {
        WeakReference<Object> mockRef = findWeakReference(mock, invocationRecords);
        if (mockRef == null) {
            return List.of();
        }

        ConcurrentMap<ContextID, Queue<InvocationRecord>> contextMap = invocationRecords.get(mockRef);
        if (contextMap == null) {
            return List.of();
        }

        Queue<InvocationRecord> recordsQueue = contextMap.get(contextId);
        if (recordsQueue == null) {
            return List.of();
        }

        // Return an immutable list copy
        return List.copyOf(recordsQueue);
    }

    static List<InvocationRecord> getAllInvocationRecords(Object mock) {
        WeakReference<Object> mockRef = findWeakReference(mock, invocationRecords);
        if (mockRef == null) {
            return List.of();
        }

        ConcurrentMap<ContextID, Queue<InvocationRecord>> contextMap = invocationRecords.get(mockRef);
        if (contextMap == null) {
            return List.of();
        }

        // Collect records from all contexts for this mock
        return contextMap.values().stream()
                .flatMap(Queue::stream)
                .collect(Collectors.toUnmodifiableList());
    }


    // Helper to find the existing WeakReference for a given mock object
    private static <V> WeakReference<Object> findWeakReference(Object mock, ConcurrentMap<WeakReference<Object>, V> map) {
        // Clean up stale references while searching - might impact performance slightly
        // Consider a separate cleanup thread if this becomes a bottleneck
        map.keySet().removeIf(ref -> ref.get() == null);

        for (WeakReference<Object> ref : map.keySet()) {
            if (ref.get() == mock) {
                return ref;
            }
        }
        return null;
    }

    // Optional: Method to explicitly clean up GC'd references if needed
    static void cleanUpStaleReferences() {
        stubbingRules.keySet().removeIf(ref -> ref.get() == null);
        invocationRecords.keySet().removeIf(ref -> ref.get() == null);
        // Could also clean inner maps if necessary, but less likely needed
    }
}