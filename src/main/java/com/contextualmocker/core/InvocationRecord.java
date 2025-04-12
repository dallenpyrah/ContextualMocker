package com.contextualmocker.core;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import com.contextualmocker.matchers.ArgumentMatcher;

public final class InvocationRecord {
    private final WeakReference<Object> mockRef;
    private final Method method;
    private final Object[] arguments;
    private final ContextID contextId;
    private final Instant timestamp;
    private final long threadId;
    private final boolean stubbing;
    private final List<ArgumentMatcher<?>> matchers;
    private final AtomicBoolean verified = new AtomicBoolean(false);

    public InvocationRecord(
            Object mock,
            Method method,
            Object[] arguments,
            ContextID contextId,
            boolean stubbing,
            List<ArgumentMatcher<?>> matchers) {
        this.mockRef = new WeakReference<>(Objects.requireNonNull(mock, "Mock cannot be null"));
        this.method = Objects.requireNonNull(method, "Method cannot be null");
        this.arguments = arguments == null ? new Object[0] : arguments.clone();
        this.contextId = Objects.requireNonNull(contextId, "ContextID cannot be null");
        this.timestamp = Instant.now();
        this.threadId = Thread.currentThread().getId();
        this.stubbing = stubbing;
        this.matchers = matchers;
    }

    public InvocationRecord(
            Object mock,
            Method method,
            Object[] arguments,
            ContextID contextId,
            boolean stubbing) {
        this(mock, method, arguments, contextId, stubbing, null);
    }

    public WeakReference<Object> getMockRef() {
        return mockRef;
    }

    public Object getMock() {
        return mockRef.get();
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArguments() {
        return arguments.clone();
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

    public List<ArgumentMatcher<?>> getMatchers() {
        return matchers;
    }

    public boolean isVerified() {
        return verified.get();
    }

    public void markVerified() {
        this.verified.set(true);
    }

    @Override
    public String toString() {
        Object mock = getMock();
        return "InvocationRecord{" +
                "mock="
                + (mock != null ? mock.getClass().getSimpleName() + "@" + System.identityHashCode(mock) : "null (GC'd)")
                +
                ", method=" + method.getName() +
                ", arguments=" + Arrays.toString(arguments) +
                ", contextId=" + contextId +
                ", timestamp=" + timestamp +
                ", threadId=" + threadId +
                ", matchers=" + matchers +
                '}';
    }
    
    public boolean isStubbing() {
        return stubbing;
    }
}