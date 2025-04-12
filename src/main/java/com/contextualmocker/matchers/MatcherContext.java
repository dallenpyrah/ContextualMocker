package com.contextualmocker.matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.contextualmocker.core.ContextHolder;
import com.contextualmocker.core.ContextID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores argument matchers in a context-specific and thread-specific manner.
 * Ensures matchers are isolated per context, even within the same thread.
 */
public final class MatcherContext {
    private static final Logger logger = LoggerFactory.getLogger(MatcherContext.class);
    private static final ThreadLocal<Map<ContextID, List<ArgumentMatcher<?>>>> matchers =
            ThreadLocal.withInitial(HashMap::new);

    static void addMatcher(ArgumentMatcher<?> matcher) {
        ContextID contextId = ContextHolder.getContext();
        Map<ContextID, List<ArgumentMatcher<?>>> contextMap = matchers.get();
        List<ArgumentMatcher<?>> matcherList = contextMap.computeIfAbsent(contextId, k -> new ArrayList<>());
        matcherList.add(matcher);
    }

    public static List<ArgumentMatcher<?>> consumeMatchers() {
        ContextID contextId = ContextHolder.getContext();
        Map<ContextID, List<ArgumentMatcher<?>>> contextMap = matchers.get();
        List<ArgumentMatcher<?>> matcherList = contextMap.get(contextId);
        List<ArgumentMatcher<?>> current = matcherList == null ? new ArrayList<>() : new ArrayList<>(matcherList);
        logger.debug("[MatcherContext] Thread: {}, Context: {} consuming matchers: {}", Thread.currentThread().getName(), contextId, current);
        if (matcherList != null) {
            matcherList.clear();
        }
        return current;
    }

    static void clear() {
        ContextID contextId = ContextHolder.getContext();
        Map<ContextID, List<ArgumentMatcher<?>>> contextMap = matchers.get();
        List<ArgumentMatcher<?>> matcherList = contextMap.get(contextId);
        if (matcherList != null) {
            matcherList.clear();
        }
    }
}