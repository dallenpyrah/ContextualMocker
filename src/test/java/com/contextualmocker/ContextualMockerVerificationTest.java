package com.contextualmocker;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

interface SampleService {
    String greet(String name, int code);
}

public class ContextualMockerVerificationTest {

    @Test
    void testTimesVerificationWithEqMatcher() {
        SampleService mock = ContextualMocker.mock(SampleService.class);
        ContextID ctx = new StringContextId("A");

        ContextualMocker.given(mock)
            .forContext(ctx)
            .when(mock.greet(ArgumentMatchers.eq("Alice"), ArgumentMatchers.eq(42)))
            .thenReturn("Hello Alice");

        mock.greet("Alice", 42);
        mock.greet("Alice", 42);

        ContextualMocker.verify(mock)
            .forContext(ctx)
            .verify(ContextualMocker.times(2))
            .greet("Alice", 42);
    }

    @Test
    void testNeverVerificationWithAnyMatcher() {
        SampleService mock = ContextualMocker.mock(SampleService.class);
        ContextID ctx = new StringContextId("B");

        ContextualMocker.given(mock)
            .forContext(ctx)
            .when(mock.greet(ArgumentMatchers.any(), ArgumentMatchers.eq(99)))
            .thenReturn("Hi");

        // No invocation

        ContextualMocker.verify(mock)
            .forContext(ctx)
            .verify(ContextualMocker.never())
            .greet("Bob", 99);
    }

    @Test
    void testAtLeastOnceVerificationWithContext() {
        SampleService mock = ContextualMocker.mock(SampleService.class);
        ContextID ctx1 = new StringContextId("C1");
        ContextID ctx2 = new StringContextId("C2");

        ContextualMocker.given(mock)
            .forContext(ctx1)
            .when(mock.greet(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn("Hi");

        mock.greet("X", 1);
        mock.greet("Y", 2);

        ContextualMocker.verify(mock)
            .forContext(ctx1)
            .verify(ContextualMocker.atLeastOnce())
            .greet("X", 1);

        // Should not match in a different context
        assertThrows(AssertionError.class, () -> {
            ContextualMocker.verify(mock)
                .forContext(ctx2)
                .verify(ContextualMocker.atLeastOnce())
                .greet("X", 1);
        });
    }
}