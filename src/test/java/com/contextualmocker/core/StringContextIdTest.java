package com.contextualmocker.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StringContextIdTest {

    @Test
    void getIdReturnsCorrectValue() {
        StringContextId id = new StringContextId("test-id");
        assertEquals("test-id", id.getId());
    }

    @Test
    void toStringReturnsId() {
        StringContextId id = new StringContextId("abc123");
        assertTrue(id.toString().contains("abc123"));
    }

    @Test
    void equalsAndHashCode() {
        StringContextId id1 = new StringContextId("x");
        StringContextId id2 = new StringContextId("x");
        StringContextId id3 = new StringContextId("y");
        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertEquals(id1.hashCode(), id2.hashCode());
    }
}