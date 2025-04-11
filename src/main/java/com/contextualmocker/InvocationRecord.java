package com.contextualmocker;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

public final class InvocationRecord {
    private final WeakReference<Object> mockRef;
    private final Method method;
    private final Object[] arguments;
    private final ContextID contextId;
    private final Instant timestamp;
    private final long threadId;

    public InvocationRecord(
            Object mock,
            Method method,
            Object[] arguments,
            ContextID contextId) {
        this.mockRef = new WeakReference<>(Objects.requireNonNull(mock, "Mock cannot be null"));
        this.method = Objects.requireNonNull(method, "Method cannot be null");
        this.arguments = arguments == null ? new Object[0] : arguments.clone(); // Defensive copy
        this.contextId = Objects.requireNonNull(contextId, "ContextID cannot be null");
        this.timestamp = Instant.now();
        this.threadId = Thread.currentThread().getId();
    }

    public WeakReference<Object> getMockRef() {
        return mockRef;
    }

    public Object getMock() {
        return mockRef.get(); // May return null if GC'd
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArguments() {
        return arguments.clone(); // Defensive copy
    }

    public ContextID getContextId() {
        return contextId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public long getThreadId() {
        return threadId;
    }

    @Override
    public String toString() {
        Object mock = getMock();
        return "InvocationRecord{" +
               "mock=" + (mock != null ? mock.getClass().getSimpleName() + "@" + System.identityHashCode(mock) : "null (GC'd)") +
               ", method=" + method.getName() +
               ", arguments=" + Arrays.toString(arguments) +
               ", contextId=" + contextId +
               ", timestamp=" + timestamp +
               ", threadId=" + threadId +
               '}';
    }

    // equals/hashCode are not typically needed for simple records like this,
    // unless they are stored in sets or used as map keys directly.
    // The identity of the record is usually sufficient.
}