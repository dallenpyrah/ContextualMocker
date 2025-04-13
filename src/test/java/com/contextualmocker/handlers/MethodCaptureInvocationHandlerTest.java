package com.contextualmocker.handlers;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class MethodCaptureInvocationHandlerTest {

    interface DummyInterface {
        String doSomething(String input);
        int getInt();
        long getLong();
        float getFloat();
        double getDouble();
        char getChar();
        boolean getBoolean();
        byte getByte();
        short getShort();
        Object getObject();
    }

    @Test
    void testProxyReturnsDefaultValues() {
        DummyInterface dummy = (DummyInterface) Proxy.newProxyInstance(
                DummyInterface.class.getClassLoader(),
                new Class[]{DummyInterface.class},
                new MethodCaptureInvocationHandler<>(null)
        );

        assertNull(dummy.doSomething("test"));
        assertEquals(0, dummy.getInt());
        assertEquals(0L, dummy.getLong());
        assertEquals(0.0f, dummy.getFloat());
        assertEquals(0.0d, dummy.getDouble());
        assertEquals('\0', dummy.getChar());
        assertFalse(dummy.getBoolean());
        assertEquals((byte) 0, dummy.getByte());
        assertEquals((short) 0, dummy.getShort());
        assertNull(dummy.getObject());
    }

    @Test
    void testInvokeWithNullArguments() throws Throwable {
        MethodCaptureInvocationHandler<?, ?> handler = new MethodCaptureInvocationHandler<>(null);
        Object result = handler.invoke(null, Object.class.getMethod("toString"), null);
        assertNull(result);
    }
}