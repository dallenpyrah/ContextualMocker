package com.contextualmocker.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CanonicalMockReferenceTest {

    @Test
    void testEqualsSameReferent() {
        Object obj = new Object();
        CanonicalMockReference ref1 = new CanonicalMockReference(obj);
        CanonicalMockReference ref2 = new CanonicalMockReference(obj);
        assertEquals(ref1, ref2);
        assertEquals(ref2, ref1);
        assertEquals(ref1, ref1);
    }

    @Test
    void testNotEqualsDifferentReferent() {
        CanonicalMockReference ref1 = new CanonicalMockReference(new Object());
        CanonicalMockReference ref2 = new CanonicalMockReference(new Object());
        assertNotEquals(ref1, ref2);
    }

    @Test
    void testNotEqualsNullOrOtherType() {
        CanonicalMockReference ref = new CanonicalMockReference(new Object());
        assertNotEquals(ref, null);
        assertNotEquals(ref, "not a reference");
    }
}