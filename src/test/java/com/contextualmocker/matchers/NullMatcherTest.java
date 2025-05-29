package com.contextualmocker.matchers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NullMatcher.
 */
class NullMatcherTest {

    @Test
    void testMatches_Null() {
        NullMatcher<String> matcher = new NullMatcher<>();
        
        assertTrue(matcher.matches(null));
    }

    @Test
    void testMatches_NonNull() {
        NullMatcher<String> matcher = new NullMatcher<>();
        
        assertFalse(matcher.matches("not null"));
        assertFalse(matcher.matches(""));
        assertFalse(matcher.matches(0));
        assertFalse(matcher.matches(false));
        assertFalse(matcher.matches(new Object()));
        assertFalse(matcher.matches(new String[]{}));
    }

    @Test
    void testMatches_DifferentTypes() {
        NullMatcher<Object> matcher = new NullMatcher<>();
        
        // Test with various non-null types
        assertFalse(matcher.matches(123));
        assertFalse(matcher.matches(123L));
        assertFalse(matcher.matches(123.45));
        assertFalse(matcher.matches(123.45f));
        assertFalse(matcher.matches(true));
        assertFalse(matcher.matches('a'));
        assertFalse(matcher.matches(new byte[]{1, 2, 3}));
        assertFalse(matcher.matches(new int[]{1, 2, 3}));
    }

    @Test
    void testToString() {
        NullMatcher<String> matcher = new NullMatcher<>();
        
        assertEquals("isNull()", matcher.toString());
    }

    @Test
    void testGenericTypeHandling() {
        // Test that matcher works with different generic types
        NullMatcher<String> stringMatcher = new NullMatcher<>();
        NullMatcher<Integer> intMatcher = new NullMatcher<>();
        NullMatcher<Object> objectMatcher = new NullMatcher<>();
        
        // All should behave the same way for null
        assertTrue(stringMatcher.matches(null));
        assertTrue(intMatcher.matches(null));
        assertTrue(objectMatcher.matches(null));
        
        // All should behave the same way for non-null
        assertFalse(stringMatcher.matches("test"));
        assertFalse(intMatcher.matches("test"));
        assertFalse(objectMatcher.matches("test"));
    }

    @Test
    void testImplementsArgumentMatcher() {
        NullMatcher<String> matcher = new NullMatcher<>();
        
        // Verify it implements ArgumentMatcher
        assertTrue(matcher instanceof ArgumentMatcher);
        
        // Verify the interface methods work
        assertNotNull(matcher.toString());
        assertTrue(matcher.matches(null));
        assertFalse(matcher.matches("not null"));
    }

    @Test
    void testMultipleInstances() {
        // Test that multiple instances behave consistently
        NullMatcher<String> matcher1 = new NullMatcher<>();
        NullMatcher<String> matcher2 = new NullMatcher<>();
        
        assertTrue(matcher1.matches(null));
        assertTrue(matcher2.matches(null));
        
        assertFalse(matcher1.matches("test"));
        assertFalse(matcher2.matches("test"));
        
        assertEquals(matcher1.toString(), matcher2.toString());
    }

    @Test
    void testEdgeCaseValues() {
        NullMatcher<Object> matcher = new NullMatcher<>();
        
        // Test with edge case values that are not null
        assertFalse(matcher.matches(Double.NaN));
        assertFalse(matcher.matches(Double.POSITIVE_INFINITY));
        assertFalse(matcher.matches(Double.NEGATIVE_INFINITY));
        assertFalse(matcher.matches(Float.NaN));
        assertFalse(matcher.matches(Long.MAX_VALUE));
        assertFalse(matcher.matches(Long.MIN_VALUE));
        assertFalse(matcher.matches(Integer.MAX_VALUE));
        assertFalse(matcher.matches(Integer.MIN_VALUE));
    }
}