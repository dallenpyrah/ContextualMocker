package com.contextualmocker.core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import com.contextualmocker.core.CanonicalMockReference;

/**
 * A thread-safe registry holding all state for mocks created by ContextualMocker.
 * This includes stubbing rules, recorded invocations, and state for stateful mocking,
 * all organized by mock instance (via WeakReference) and context ID.
 */
public final class MockRegistry {
   private static final Logger logger = LoggerFactory.getLogger(MockRegistry.class);

    // Structure: Mock Instance (CanonicalMockReference) -> ContextID -> Deque of Rules (LIFO)
    private static final ConcurrentMap<CanonicalMockReference, ConcurrentMap<ContextID, Deque<StubbingRule>>> stubbingRules =
            new ConcurrentHashMap<>();

    // Structure: Mock Instance (CanonicalMockReference) -> ContextID -> Deque of Invocations
    private static final ConcurrentMap<CanonicalMockReference, ConcurrentMap<ContextID, Deque<InvocationRecord>>> invocationRecords =
            new ConcurrentHashMap<>();

    // Structure: Mock Instance (CanonicalMockReference) -> ContextID -> AtomicReference<Object> (state)
    private static final ConcurrentMap<CanonicalMockReference, ConcurrentMap<ContextID, java.util.concurrent.atomic.AtomicReference<Object>>> stateMap =
            new ConcurrentHashMap<>();

    private MockRegistry() {
    }

    /**
     * Adds a new stubbing rule for a specific mock and context.
     * Rules are added to the beginning of the list, so the last added rule is matched first.
     *
     * @param mock The mock instance.
     * @param contextId The context ID for this rule.
     * @param rule The {@link StubbingRule} to add.
     */
    public static void addStubbingRule(Object mock, ContextID contextId, StubbingRule rule) {
        CanonicalMockReference mockRef = findCanonicalReference(mock, stubbingRules);
        if (logger.isDebugEnabled()) {
            logger.debug("Adding stubbing rule for mockRef: {}, context: {}, rule: {}", mockRef, contextId, rule);
        }

        stubbingRules
                .computeIfAbsent(mockRef, k -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Creating new context map for mockRef: {}", k);
                    }
                    return new ConcurrentHashMap<>();
                })
                .computeIfAbsent(contextId, k -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Creating new rule deque for context: {}", k);
                    }
                    return new ConcurrentLinkedDeque<>();
                })
                .addFirst(rule);
        if (logger.isDebugEnabled()) {
            logger.debug("Stubbing rule added successfully for context: {}", contextId);
        }
    }

    /**
     * Finds the first matching stubbing rule for a given invocation.
     *
     * @param mock The mock instance.
     * @param contextId The context ID of the invocation.
     * @param method The invoked method.
     * @param arguments The invocation arguments.
     * @param currentState The current state of the mock for the given context (for stateful matching).
     * @return The matching {@link StubbingRule}, or null if none found.
     */
    public static StubbingRule findStubbingRule(Object mock, ContextID contextId, Method method, Object[] arguments, Object currentState) {
        CanonicalMockReference mockRef = findCanonicalReference(mock, stubbingRules);
        if (mockRef == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No mock reference found for mock: {}", mock);
            }
            return null;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Searching stubbing rule for mockRef: {}, context: {}, method: {}", mockRef, contextId, method.getName());
        }

        ConcurrentMap<ContextID, Deque<StubbingRule>> contextMap = stubbingRules.get(mockRef);
        if (contextMap == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No context map found for mockRef: {}", mockRef);
            }
            return null;
        }

        Deque<StubbingRule> rules = contextMap.get(contextId);
        if (rules == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No rules found for context: {}", contextId);
            }
            return null;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Checking rules for context: {}", contextId);
        }
        // Use iterator to safely remove expired rules while iterating
        Iterator<StubbingRule> iterator = rules.iterator();
        while (iterator.hasNext()) {
            StubbingRule rule = iterator.next();
            if (rule.isExpired()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Removing expired rule: {}", rule);
                }
                iterator.remove(); // Remove expired rule
                continue; // Check next rule
            }
            if (rule.matches(method, arguments, currentState)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Found matching rule: {}", rule);
                }
                return rule;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Rule did not match: {}", rule);
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("No matching rule found for context: {}", contextId);
        }
        return null;
    }

    /**
     * Records an invocation on a mock instance.
     *
     * @param record The {@link InvocationRecord} containing details of the invocation.
     */
    public static void recordInvocation(InvocationRecord record) {
        Object mock = record.getMock();
        if (mock == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Attempted to record invocation with null mock");
            }
            return;
        }
        ContextID contextId = record.getContextId();
        CanonicalMockReference mockRef = findCanonicalReference(mock, invocationRecords);
        if (logger.isDebugEnabled()) {
            logger.debug("Recording invocation for mockRef: {}, context: {}, method: {}", mockRef, contextId, record.getMethod().getName());
        }

        invocationRecords
                .computeIfAbsent(mockRef, k -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Creating new invocation context map for mockRef: {}", k);
                    }
                    return new ConcurrentHashMap<>();
                })
                .computeIfAbsent(contextId, k -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Creating new invocation deque for context: {}", k);
                    }
                    return new ConcurrentLinkedDeque<>();
                })
                .addLast(record);
        if (logger.isDebugEnabled()) {
            logger.debug("Invocation recorded successfully for context: {}", contextId);
        }
    }

    /**
     * Retrieves all non-stubbing invocation records for a specific mock and context.
     *
     * @param mock The mock instance.
     * @param contextId The context ID.
     * @return An immutable list of {@link InvocationRecord}s, excluding those made during stubbing setup.
     */
    public static List<InvocationRecord> getInvocationRecords(Object mock, ContextID contextId) {
        CanonicalMockReference mockRef = findCanonicalReference(mock, invocationRecords);
        if (mockRef == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No mock reference found for invocation records: {}", mock);
            }
            return List.of();
        }

        ConcurrentMap<ContextID, Deque<InvocationRecord>> contextMap = invocationRecords.get(mockRef);
        if (contextMap == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No invocation context map found for mockRef: {}", mockRef);
            }
            return List.of();
        }

        Deque<InvocationRecord> recordsDeque = contextMap.get(contextId);
        if (recordsDeque == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No invocation records found for context: {}", contextId);
            }
            return List.of();
        }

        List<InvocationRecord> records = recordsDeque.stream()
                .filter(r -> !r.isStubbing())
                .collect(Collectors.toList());
        if (logger.isDebugEnabled()) {
            logger.debug("Retrieved {} non-stubbing invocation records for context: {}", records.size(), contextId);
        }
        return records;
    }

    /**
     * Clears all invocation records associated with a specific context ID across all mocks.
     * This is typically called when a context exits to prevent memory leaks.
     *
     * @param contextId The context ID whose invocation records should be cleared.
     */
    public static void clearInvocationsForContext(ContextID contextId) {
        if (contextId == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Attempted to clear invocations for null contextId");
            }
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Clearing invocation records for context: {}", contextId);
        }
        int clearedCount = 0;
        for (ConcurrentMap<ContextID, Deque<InvocationRecord>> contextMap : invocationRecords.values()) {
            if (contextMap.remove(contextId) != null) {
                clearedCount++;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Cleared invocation records for context {} from {} mocks.", contextId, clearedCount);
        }
        // Optional: Clean up mock references if their context map becomes empty?
        // invocationRecords.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * Removes the most recently recorded invocation for a specific mock and context.
     * This is primarily used internally to discard the invocation made during `when()`.
     *
     * @param mock The mock instance.
     * @param contextId The context ID.
     */
    public static void removeLastInvocation(Object mock, ContextID contextId) {
        CanonicalMockReference mockRef = findCanonicalReference(mock, invocationRecords);
        if (mockRef == null) return;
        ConcurrentMap<ContextID, Deque<InvocationRecord>> contextMap = invocationRecords.get(mockRef);
        if (contextMap == null) return;
        Deque<InvocationRecord> recordsDeque = contextMap.get(contextId);
        if (recordsDeque == null || recordsDeque.isEmpty()) return;
        // Simply poll the last element in a thread-safe way
        recordsDeque.pollLast();
    }

    // Helper method for internal checks, potentially for verifyNoMoreInteractions across all contexts.
    static List<InvocationRecord> getAllInvocationRecords(Object mock) {
        CanonicalMockReference mockRef = findCanonicalReference(mock, invocationRecords);
        if (mockRef == null) {
            return List.of();
        }

        ConcurrentMap<ContextID, Deque<InvocationRecord>> contextMap = invocationRecords.get(mockRef);
        if (contextMap == null) {
            return List.of();
        }

        return contextMap.values().stream()
                .flatMap(Deque::stream)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Gets the current state for a specific mock and context.
     * Used for stateful mocking (`whenStateIs`).
     *
     * @param mock The mock instance.
     * @param contextId The context ID.
     * @return The current state object, or null if no state is set.
     */
    public static Object getState(Object mock, ContextID contextId) {
        CanonicalMockReference mockRef = findCanonicalReference(mock, stateMap);
        if (mockRef == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No mock reference found for state map: {}", mock);
            }
            return null;
        }
        ConcurrentMap<ContextID, java.util.concurrent.atomic.AtomicReference<Object>> contextMap = stateMap.get(mockRef);

        if (contextMap == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No state context map found for mockRef: {}", mockRef);
            }
            return null;
        }
        java.util.concurrent.atomic.AtomicReference<Object> ref = contextMap.get(contextId);
        Object state = ref == null ? null : ref.get();
        if (logger.isDebugEnabled()) {
            logger.debug("Retrieved state: {} for context: {}", state, contextId);
        }
        return state;
    }

    /**
     * Sets the state for a specific mock and context.
     * Used for stateful mocking (`willSetStateTo`).
     *
     * @param mock The mock instance.
     * @param contextId The context ID.
     * @param newState The new state object.
     */
    public static void setState(Object mock, ContextID contextId, Object newState) {
        CanonicalMockReference mockRef = findCanonicalReference(mock, stateMap);
        if (logger.isDebugEnabled()) {
            logger.debug("Setting state for mockRef: {}, context: {}, newState: {}", mockRef, contextId, newState);
        }

        // Get or create the context map for this mock in a thread-safe way
        ConcurrentMap<ContextID, java.util.concurrent.atomic.AtomicReference<Object>> contextMap =
            stateMap.computeIfAbsent(mockRef, k -> {
                if (logger.isDebugEnabled()) {
                    logger.debug("Creating new state context map for mockRef: {}", k);
                }
                return new ConcurrentHashMap<>();
            });

        // Get or create the atomic reference for this context in a thread-safe way
        java.util.concurrent.atomic.AtomicReference<Object> stateRef =
            contextMap.computeIfAbsent(contextId, k -> {
                if (logger.isDebugEnabled()) {
                    logger.debug("Creating new state reference for context: {}", k);
                }
                return new java.util.concurrent.atomic.AtomicReference<>();
            });

        // Atomically set the new state
        stateRef.set(newState);
        if (logger.isDebugEnabled()) {
            logger.debug("State set successfully to: {} for context: {}", newState, contextId);
        }
    }

    // Internal state reset, potentially useful for testing or extensions.
    static void resetState(Object mock, ContextID contextId) {
        CanonicalMockReference mockRef = findCanonicalReference(mock, stateMap);
        if (mockRef == null) {
            return;
        }
        ConcurrentMap<ContextID, java.util.concurrent.atomic.AtomicReference<Object>> contextMap = stateMap.get(mockRef);
        if (contextMap != null) {
            contextMap.remove(contextId);
        }
    }

    // Global identity map to ensure a single CanonicalMockReference per mock instance
    private static final ConcurrentHashMap<Object, CanonicalMockReference> identityMap = new ConcurrentHashMap<>();

    // Finds or creates the canonical reference for a given mock object.
    private static <V> CanonicalMockReference findCanonicalReference(Object mock, ConcurrentMap<CanonicalMockReference, V> map) {
        if (mock == null) return null;
        // Use the global identity map to ensure a single CanonicalMockReference per mock
        CanonicalMockReference ref = identityMap.computeIfAbsent(mock, CanonicalMockReference::new);
        // Double-check if the reference is still valid
        if (ref.get() == null) {
            identityMap.remove(mock);
            return identityMap.computeIfAbsent(mock, CanonicalMockReference::new);
        }
        return ref;
    }

    // Explicitly cleans up stale references from all registry maps.
    // Could be called periodically or during specific events if needed.
    static void cleanUpStaleReferences() {
        stubbingRules.keySet().removeIf(ref -> ref.get() == null);
        invocationRecords.keySet().removeIf(ref -> ref.get() == null);
        stateMap.keySet().removeIf(ref -> ref.get() == null);
        // Also clean up the identity map itself
        identityMap.keySet().removeIf(mock -> {
            CanonicalMockReference ref = identityMap.get(mock);
            return ref != null && ref.get() == null;
        });
    }

}
