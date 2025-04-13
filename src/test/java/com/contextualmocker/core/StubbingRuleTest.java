package com.contextualmocker.core;

import com.contextualmocker.handlers.ContextualAnswer;
import com.contextualmocker.matchers.ArgumentMatcher;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class StubbingRuleTest {

    @Test
    void builderCreatesStubbingRule() throws Exception {
        Method method = Object.class.getMethod("toString");
        StubbingRule rule = StubbingRule.builder(method)
                .expectedArguments(new Object[0])
                .argumentMatchers(new ArgumentMatcher[0])
                .answer((contextId, mock, m, args) -> "answer")
                .returnValue("return")
                .throwable(null)
                .requiredState(null)
                .nextState("state2")
                .ttlMillis(1000L)
                .build();

        assertNotNull(rule);
        assertTrue(rule.matches(method, new Object[0], null));
        assertEquals("state2", rule.getNextState());
        assertFalse(rule.isExpired());
        assertNotNull(rule.toString());
    }

    @Test
    void toStringIsNotNull() throws Exception {
        Method method = Object.class.getMethod("hashCode");
        StubbingRule rule = StubbingRule.builder(method).build();
        assertNotNull(rule.toString());
    }
}