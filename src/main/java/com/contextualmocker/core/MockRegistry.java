package com.contextualmocker.core;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public final class MockRegistry {

    // Structure: Mock Instance (WeakRef) -> ContextID -> List of Rules
    private static final ConcurrentMap<WeakReference<Object>, ConcurrentMap<ContextID, List<StubbingRule>>> stubbingRules =
            new ConcurrentHashMap<>();

    // Structure: Mock Instance (WeakRef) -> ContextID -> Queue of Invocations
    private static final ConcurrentMap<WeakReference<Object>, ConcurrentMap<ContextID, Queue<InvocationRecord>>> invocationRecords =
            new ConcurrentHashMap<>();

    // Structure: Mock Instance (WeakRef) -> ContextID -> AtomicReference<Object> (state)
    private static final ConcurrentMap<WeakReference<Object>, ConcurrentMap<ContextID, java.util.concurrent.atomic.AtomicReference<Object>>> stateMap =
            new ConcurrentHashMap<>();

    private MockRegistry() {
    }

    public static void addStubbingRule(Object mock, ContextID contextId, StubbingRule rule) {
        WeakReference<Object> mockRef = findWeakReference(mock, stubbingRules);
        if (mockRef == null) {
            mockRef = new WeakReference<>(mock);
        }

        stubbingRules
                .computeIfAbsent(mockRef, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(contextId, k -> new CopyOnWriteArrayList<>())
                .add(0, rule);
    }

    public static StubbingRule findStubbingRule(Object mock, ContextID contextId, Method method, Object[] arguments, Object currentState) {
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

        for (StubbingRule rule : rules) {
            if (rule.matches(method, arguments, currentState)) {
                return rule;
            }
        }

        return null;
    }

    public static void recordInvocation(InvocationRecord record) {
        Object mock = record.getMock();
        if (mock == null) {
            return;
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

    public static List<InvocationRecord> getInvocationRecords(Object mock, ContextID contextId) {
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

        return recordsQueue.stream()
                .filter(r -> !r.isStubbing())
                .toList();
    }

    public static void removeLastInvocation(Object mock, ContextID contextId) {
        WeakReference<Object> mockRef = findWeakReference(mock, invocationRecords);
        if (mockRef == null) return;
        ConcurrentMap<ContextID, Queue<InvocationRecord>> contextMap = invocationRecords.get(mockRef);
        if (contextMap == null) return;
        Queue<InvocationRecord> recordsQueue = contextMap.get(contextId);
        if (recordsQueue == null || recordsQueue.isEmpty()) return;
        if (recordsQueue instanceof java.util.concurrent.ConcurrentLinkedQueue) {
            java.util.List<InvocationRecord> list = new java.util.ArrayList<>(recordsQueue);
            if (!list.isEmpty()) {
                recordsQueue.remove(list.get(list.size() - 1));
            }
        } else if (recordsQueue instanceof java.util.Deque) {
            ((java.util.Deque<InvocationRecord>) recordsQueue).removeLast();
        } else if (recordsQueue instanceof java.util.List) {
            java.util.List<InvocationRecord> list = (java.util.List<InvocationRecord>) recordsQueue;
            if (!list.isEmpty()) {
                list.remove(list.size() - 1);
            }
        }
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

        return contextMap.values().stream()
                .flatMap(Queue::stream)
                .collect(Collectors.toUnmodifiableList());
    }

    public static Object getState(Object mock, ContextID contextId) {
        WeakReference<Object> mockRef = findWeakReference(mock, stateMap);
        if (mockRef == null) {
            return null;
        }
        ConcurrentMap<ContextID, java.util.concurrent.atomic.AtomicReference<Object>> contextMap = stateMap.get(mockRef);
        if (contextMap == null) {
            return null;
        }
        java.util.concurrent.atomic.AtomicReference<Object> ref = contextMap.get(contextId);
        return ref == null ? null : ref.get();
    }

    public static void setState(Object mock, ContextID contextId, Object newState) {
        WeakReference<Object> mockRef = findWeakReference(mock, stateMap);
        if (mockRef == null) {
            mockRef = new WeakReference<>(mock);
        }
        stateMap
                .computeIfAbsent(mockRef, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(contextId, k -> new java.util.concurrent.atomic.AtomicReference<>())
                .set(newState);
    }

    static void resetState(Object mock, ContextID contextId) {
        WeakReference<Object> mockRef = findWeakReference(mock, stateMap);
        if (mockRef == null) {
            return;
        }
        ConcurrentMap<ContextID, java.util.concurrent.atomic.AtomicReference<Object>> contextMap = stateMap.get(mockRef);
        if (contextMap != null) {
            contextMap.remove(contextId);
        }
    }

    private static <V> WeakReference<Object> findWeakReference(Object mock, ConcurrentMap<WeakReference<Object>, V> map) {
        map.keySet().removeIf(ref -> ref.get() == null);

        for (WeakReference<Object> ref : map.keySet()) {
            if (ref.get() == mock) {
                return ref;
            }
        }
        return null;
    }

    static void cleanUpStaleReferences() {
        stubbingRules.keySet().removeIf(ref -> ref.get() == null);
        invocationRecords.keySet().removeIf(ref -> ref.get() == null);
        stateMap.keySet().removeIf(ref -> ref.get() == null);
    }
}