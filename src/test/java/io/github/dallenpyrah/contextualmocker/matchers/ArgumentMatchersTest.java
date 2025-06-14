package io.github.dallenpyrah.contextualmocker.matchers;

import io.github.dallenpyrah.contextualmocker.core.ContextHolder;
import io.github.dallenpyrah.contextualmocker.core.StringContextId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ArgumentMatchersTest {

    @BeforeEach
    void setUp() {
        ContextHolder.setContext(new StringContextId(UUID.randomUUID().toString()));
    }

    @AfterEach
    void tearDown() {
        ContextHolder.clearContext();
    }

    @Test
    void testAnyLong() {
        Object result = ArgumentMatchers.anyLong();
        assertTrue(result instanceof Long, "anyLong should return a Long");
    }

    @Test
    void testAnyDouble() {
        Object result = ArgumentMatchers.anyDouble();
        assertTrue(result instanceof Double, "anyDouble should return a Double");
    }

    @Test
    void testAnyFloat() {
        Object result = ArgumentMatchers.anyFloat();
        assertTrue(result instanceof Float, "anyFloat should return a Float");
    }

    @Test
    void testAnyBoolean() {
        Object result = ArgumentMatchers.anyBoolean();
        assertTrue(result instanceof Boolean, "anyBoolean should return a Boolean");
    }

    @Test
    void testAnyByte() {
        Object result = ArgumentMatchers.anyByte();
        assertTrue(result instanceof Byte, "anyByte should return a Byte");
    }

    @Test
    void testAnyShort() {
        Object result = ArgumentMatchers.anyShort();
        assertTrue(result instanceof Short, "anyShort should return a Short");
    }

    @Test
    void testAnyChar() {
        Object result = ArgumentMatchers.anyChar();
        assertTrue(result instanceof Character, "anyChar should return a Character");
    }
}