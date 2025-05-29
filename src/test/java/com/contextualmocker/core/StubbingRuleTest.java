package com.contextualmocker.core;

import com.contextualmocker.handlers.ContextualAnswer;
import com.contextualmocker.matchers.ArgumentMatcher;
import com.contextualmocker.matchers.AnyMatcher;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class StubbingRuleTest {

    public interface TestService {
        String getData(String id);
        void updateData(String id, String data);
        int getNumber();
        boolean isEnabled();
        void voidMethod();
        byte getByteValue();
        short getShortValue();
        long getLongValue();
        float getFloatValue();
        double getDoubleValue();
        char getCharValue();
    }

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

    @Test
    void testBuilderWithReturnValue() throws Exception {
        Method method = TestService.class.getMethod("getData", String.class);
        StubbingRule rule = StubbingRule.builder(method)
                .expectedArguments(new Object[]{"test"})
                .returnValue("result")
                .build();

        assertTrue(rule.matches(method, new Object[]{"test"}, null));
        
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        try {
            Object result = rule.apply(contextId, new Object(), method, new Object[]{"test"});
            assertEquals("result", result);
        } catch (Throwable e) {
            fail("Unexpected throwable: " + e);
        }
    }

    @Test
    void testBuilderWithThrowable() throws Exception {
        Method method = TestService.class.getMethod("getData", String.class);
        RuntimeException expectedException = new RuntimeException("Test exception");
        
        StubbingRule rule = StubbingRule.builder(method)
                .expectedArguments(new Object[]{"test"})
                .throwable(expectedException)
                .build();

        assertTrue(rule.matches(method, new Object[]{"test"}, null));
        
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        try {
            rule.apply(contextId, new Object(), method, new Object[]{"test"});
            fail("Expected RuntimeException to be thrown");
        } catch (RuntimeException e) {
            assertSame(expectedException, e);
        } catch (Throwable e) {
            fail("Unexpected throwable type: " + e);
        }
    }

    @Test
    void testBuilderWithAnswer() throws Exception {
        Method method = TestService.class.getMethod("getData", String.class);
        ContextualAnswer<String> answer = (contextId, mock, m, args) -> "answer-" + args[0];
        
        StubbingRule rule = StubbingRule.builder(method)
                .expectedArguments(new Object[]{"test"})
                .answer(answer)
                .build();

        assertTrue(rule.matches(method, new Object[]{"test"}, null));
        
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        try {
            Object result = rule.apply(contextId, new Object(), method, new Object[]{"test"});
            assertEquals("answer-test", result);
        } catch (Throwable e) {
            fail("Unexpected throwable: " + e);
        }
    }

    @Test
    void testMatchesWithDifferentMethod() throws Exception {
        Method method1 = TestService.class.getMethod("getData", String.class);
        Method method2 = TestService.class.getMethod("getNumber");
        
        StubbingRule rule = StubbingRule.builder(method1)
                .expectedArguments(new Object[]{"test"})
                .returnValue("result")
                .build();

        assertFalse(rule.matches(method2, new Object[]{"test"}, null));
    }

    @Test
    void testMatchesWithDifferentState() throws Exception {
        Method method = TestService.class.getMethod("getData", String.class);
        
        StubbingRule rule = StubbingRule.builder(method)
                .expectedArguments(new Object[]{"test"})
                .requiredState("required-state")
                .returnValue("result")
                .build();

        assertTrue(rule.matches(method, new Object[]{"test"}, "required-state"));
        assertFalse(rule.matches(method, new Object[]{"test"}, "different-state"));
        assertFalse(rule.matches(method, new Object[]{"test"}, null));
    }

    @Test
    void testMatchesWithNullState() throws Exception {
        Method method = TestService.class.getMethod("getData", String.class);
        
        StubbingRule rule = StubbingRule.builder(method)
                .expectedArguments(new Object[]{"test"})
                .requiredState(null)
                .returnValue("result")
                .build();

        assertTrue(rule.matches(method, new Object[]{"test"}, null));
        assertFalse(rule.matches(method, new Object[]{"test"}, "some-state"));
    }

    @Test
    void testMatchesWithDifferentArgumentCount() throws Exception {
        Method method = TestService.class.getMethod("updateData", String.class, String.class);
        
        StubbingRule rule = StubbingRule.builder(method)
                .expectedArguments(new Object[]{"id", "data"})
                .build();

        assertFalse(rule.matches(method, new Object[]{"id"}, null)); // Too few args
        assertFalse(rule.matches(method, new Object[]{"id", "data", "extra"}, null)); // Too many args
        assertTrue(rule.matches(method, new Object[]{"id", "data"}, null)); // Correct args
    }

    @Test
    void testMatchesWithNullArguments() throws Exception {
        Method method = TestService.class.getMethod("voidMethod");
        
        StubbingRule rule = StubbingRule.builder(method)
                .expectedArguments(new Object[0])
                .build();

        assertTrue(rule.matches(method, null, null)); // Null args should be treated as empty array
        assertTrue(rule.matches(method, new Object[0], null));
    }

    @Test
    void testMatchesWithArgumentMatchers() throws Exception {
        Method method = TestService.class.getMethod("getData", String.class);
        
        ArgumentMatcher<String> matcher = new ArgumentMatcher<String>() {
            @Override
            public boolean matches(Object argument) {
                return argument instanceof String && ((String) argument).startsWith("test");
            }
        };
        
        StubbingRule rule = StubbingRule.builder(method)
                .argumentMatchers(new ArgumentMatcher[]{matcher})
                .returnValue("result")
                .build();

        assertTrue(rule.matches(method, new Object[]{"test123"}, null));
        assertFalse(rule.matches(method, new Object[]{"other"}, null));
        assertFalse(rule.matches(method, new Object[]{"best"}, null));
    }

    @Test
    void testMatchesWithAnyMatcher() throws Exception {
        Method method = TestService.class.getMethod("getData", String.class);
        
        AnyMatcher anyMatcher = new AnyMatcher();
        
        StubbingRule rule = StubbingRule.builder(method)
                .argumentMatchers(new ArgumentMatcher[]{anyMatcher})
                .returnValue("result")
                .build();

        assertTrue(rule.matches(method, new Object[]{"anything"}, null));
        assertTrue(rule.matches(method, new Object[]{"test"}, null));
        assertTrue(rule.matches(method, new Object[]{null}, null));
    }

    @Test
    void testMatchesWithNullMatcher() throws Exception {
        Method method = TestService.class.getMethod("getData", String.class);
        
        StubbingRule rule = StubbingRule.builder(method)
                .expectedArguments(new Object[]{"test"})
                .argumentMatchers(new ArgumentMatcher[]{null}) // Null matcher should fallback to deepEquals
                .returnValue("result")
                .build();

        assertTrue(rule.matches(method, new Object[]{"test"}, null));
        assertFalse(rule.matches(method, new Object[]{"other"}, null));
    }

    @Test
    void testMatchesWithMixedMatchers() throws Exception {
        Method method = TestService.class.getMethod("updateData", String.class, String.class);
        
        ArgumentMatcher<String> firstMatcher = new ArgumentMatcher<String>() {
            @Override
            public boolean matches(Object argument) {
                return "id".equals(argument);
            }
        };
        
        StubbingRule rule = StubbingRule.builder(method)
                .expectedArguments(new Object[]{"id", "data"})
                .argumentMatchers(new ArgumentMatcher[]{firstMatcher, null}) // First uses matcher, second uses deepEquals
                .build();

        assertTrue(rule.matches(method, new Object[]{"id", "data"}, null));
        assertFalse(rule.matches(method, new Object[]{"other", "data"}, null)); // First arg fails matcher
        assertFalse(rule.matches(method, new Object[]{"id", "other"}, null)); // Second arg fails deepEquals
    }

    @Test
    void testApplyWithDefaultValues() throws Exception {
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        Object mock = new Object();
        
        // Test primitive defaults
        testDefaultValue(TestService.class.getMethod("isEnabled"), contextId, mock, false);
        testDefaultValue(TestService.class.getMethod("getByteValue"), contextId, mock, (byte) 0);
        testDefaultValue(TestService.class.getMethod("getShortValue"), contextId, mock, (short) 0);
        testDefaultValue(TestService.class.getMethod("getNumber"), contextId, mock, 0);
        testDefaultValue(TestService.class.getMethod("getLongValue"), contextId, mock, 0L);
        testDefaultValue(TestService.class.getMethod("getFloatValue"), contextId, mock, 0.0f);
        testDefaultValue(TestService.class.getMethod("getDoubleValue"), contextId, mock, 0.0d);
        testDefaultValue(TestService.class.getMethod("getCharValue"), contextId, mock, '\u0000');
        
        // Test reference type default (null)
        testDefaultValue(TestService.class.getMethod("getData", String.class), contextId, mock, null);
        
        // Test void method default (null)
        testDefaultValue(TestService.class.getMethod("voidMethod"), contextId, mock, null);
    }

    private void testDefaultValue(Method method, ContextID contextId, Object mock, Object expectedDefault) {
        StubbingRule rule = StubbingRule.builder(method).build();
        try {
            Object result = rule.apply(contextId, mock, method, new Object[0]);
            assertEquals(expectedDefault, result);
        } catch (Throwable e) {
            fail("Unexpected throwable: " + e);
        }
    }

    @Test
    void testTtlExpiration() throws Exception {
        Method method = TestService.class.getMethod("getData", String.class);
        
        // Rule with 50ms TTL
        StubbingRule rule = StubbingRule.builder(method)
                .expectedArguments(new Object[]{"test"})
                .returnValue("result")
                .ttlMillis(50)
                .build();

        assertFalse(rule.isExpired()); // Should not be expired immediately
        
        // Wait for expiration
        Thread.sleep(60);
        
        assertTrue(rule.isExpired()); // Should be expired now
    }

    @Test
    void testNoTtlNeverExpires() throws Exception {
        Method method = TestService.class.getMethod("getData", String.class);
        
        // Rule with no TTL (default: 0)
        StubbingRule rule = StubbingRule.builder(method)
                .expectedArguments(new Object[]{"test"})
                .returnValue("result")
                .build();

        assertFalse(rule.isExpired()); // Should never expire
        
        // Rule with negative TTL
        StubbingRule rule2 = StubbingRule.builder(method)
                .expectedArguments(new Object[]{"test"})
                .returnValue("result")
                .ttlMillis(-1)
                .build();

        assertFalse(rule2.isExpired()); // Should never expire
    }

    @Test
    void testToStringWithDifferentActions() throws Exception {
        Method method = TestService.class.getMethod("getData", String.class);
        
        // Test toString with answer
        StubbingRule ruleWithAnswer = StubbingRule.builder(method)
                .answer((ctx, mock, m, args) -> "answer")
                .build();
        String toStringAnswer = ruleWithAnswer.toString();
        assertTrue(toStringAnswer.contains("answer="));
        assertFalse(toStringAnswer.contains("ttlMillis="));
        
        // Test toString with throwable
        StubbingRule ruleWithThrowable = StubbingRule.builder(method)
                .throwable(new RuntimeException("error"))
                .build();
        String toStringThrowable = ruleWithThrowable.toString();
        assertTrue(toStringThrowable.contains("throwable="));
        
        // Test toString with return value
        StubbingRule ruleWithReturn = StubbingRule.builder(method)
                .returnValue("value")
                .build();
        String toStringReturn = ruleWithReturn.toString();
        assertTrue(toStringReturn.contains("returnValue="));
        
        // Test toString with default value
        StubbingRule ruleWithDefault = StubbingRule.builder(method).build();
        String toStringDefault = ruleWithDefault.toString();
        assertTrue(toStringDefault.contains("default value"));
    }

    @Test
    void testToStringWithTtl() throws Exception {
        Method method = TestService.class.getMethod("getData", String.class);
        
        StubbingRule rule = StubbingRule.builder(method)
                .returnValue("value")
                .ttlMillis(5000)
                .build();
        
        String toString = rule.toString();
        assertTrue(toString.contains("ttlMillis=5000"));
        assertTrue(toString.contains("creationTimeMillis="));
    }

    @Test
    void testToStringWithArgumentMatchers() throws Exception {
        Method method = TestService.class.getMethod("getData", String.class);
        
        ArgumentMatcher<String> matcher = new ArgumentMatcher<String>() {
            @Override
            public boolean matches(Object argument) {
                return true;
            }
            
            @Override
            public String toString() {
                return "CustomMatcher";
            }
        };
        
        StubbingRule rule = StubbingRule.builder(method)
                .argumentMatchers(new ArgumentMatcher[]{matcher})
                .returnValue("value")
                .build();
        
        String toString = rule.toString();
        assertTrue(toString.contains("argumentMatchers="));
        assertTrue(toString.contains("CustomMatcher"));
    }

    @Test
    void testToStringWithStates() throws Exception {
        Method method = TestService.class.getMethod("getData", String.class);
        
        StubbingRule rule = StubbingRule.builder(method)
                .requiredState("required")
                .nextState("next")
                .returnValue("value")
                .build();
        
        String toString = rule.toString();
        assertTrue(toString.contains("requiredState=required"));
        assertTrue(toString.contains("nextState=next"));
    }

    @Test
    void testPrecedenceOfActions() throws Exception {
        Method method = TestService.class.getMethod("getData", String.class);
        ContextID contextId = new StringContextId(UUID.randomUUID().toString());
        Object mock = new Object();
        
        // Answer should take precedence over return value and throwable
        StubbingRule ruleWithAnswer = StubbingRule.builder(method)
                .answer((ctx, m, mth, args) -> "answer-result")
                .returnValue("return-result")
                .throwable(new RuntimeException("should not be thrown"))
                .build();
        
        try {
            Object result = ruleWithAnswer.apply(contextId, mock, method, new Object[]{"test"});
            assertEquals("answer-result", result);
        } catch (Throwable e) {
            fail("Unexpected throwable: " + e);
        }
        
        // Return value should take precedence over throwable (when no answer)
        StubbingRule ruleWithReturnAndThrowable = StubbingRule.builder(method)
                .throwable(new RuntimeException("should not be thrown"))
                .returnValue("return-value-wins")
                .build();
        
        try {
            Object result = ruleWithReturnAndThrowable.apply(contextId, mock, method, new Object[]{"test"});
            assertEquals("return-value-wins", result);
        } catch (Throwable e) {
            fail("Unexpected throwable: " + e);
        }
        
        // Test throwable alone (without return value)
        StubbingRule ruleWithOnlyThrowable = StubbingRule.builder(method)
                .throwable(new RuntimeException("expected exception"))
                .build();
        
        try {
            ruleWithOnlyThrowable.apply(contextId, mock, method, new Object[]{"test"});
            fail("Expected RuntimeException to be thrown");
        } catch (RuntimeException e) {
            assertEquals("expected exception", e.getMessage());
        } catch (Throwable e) {
            fail("Unexpected throwable type: " + e);
        }
    }

    @Test
    void testBuilderMethodChaining() throws Exception {
        Method method = TestService.class.getMethod("getData", String.class);
        
        // Test that all builder methods return the same builder instance for chaining
        StubbingRule.Builder builder = StubbingRule.builder(method);
        
        assertSame(builder, builder.expectedArguments(new Object[]{"test"}));
        assertSame(builder, builder.argumentMatchers(new ArgumentMatcher[0]));
        assertSame(builder, builder.answer((ctx, mock, m, args) -> "result"));
        assertSame(builder, builder.returnValue("value"));
        assertSame(builder, builder.throwable(new RuntimeException()));
        assertSame(builder, builder.requiredState("state"));
        assertSame(builder, builder.nextState("nextState"));
        assertSame(builder, builder.ttlMillis(1000));
        
        StubbingRule rule = builder.build();
        assertNotNull(rule);
    }

    @Test
    void testNullMethodThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            StubbingRule.builder(null).build();
        });
    }

    @Test
    void testNullExpectedArgumentsHandledGracefully() throws Exception {
        Method method = TestService.class.getMethod("voidMethod");
        
        StubbingRule rule = StubbingRule.builder(method)
                .expectedArguments(null) // Should be converted to empty array
                .build();
        
        assertTrue(rule.matches(method, new Object[0], null));
        assertTrue(rule.matches(method, null, null)); // Null args should match empty expected args
    }
}