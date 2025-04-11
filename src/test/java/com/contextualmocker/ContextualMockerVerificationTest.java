package com.contextualmocker;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
            .when(mock.greet(ArgumentMatchers.any(), ArgumentMatchers.anyInt()))
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

    @Test
    void testMultipleStubbingDoesNotAffectInvocationCount() {
        SampleService mock = ContextualMocker.mock(SampleService.class);
        ContextID ctx = new StringContextId("multi");

        ContextualMocker.given(mock)
            .forContext(ctx)
            .when(mock.greet(ArgumentMatchers.eq("A"), ArgumentMatchers.eq(1)))
            .thenReturn("A1");

        ContextualMocker.given(mock)
            .forContext(ctx)
            .when(mock.greet(ArgumentMatchers.eq("A"), ArgumentMatchers.eq(1)))
            .thenReturn("A1-again");

        mock.greet("A", 1);

        ContextualMocker.verify(mock)
            .forContext(ctx)
            .verify(ContextualMocker.times(1))
            .greet("A", 1);
    }

    @Test
    void testStubbingWithDifferentMatchers() {
        SampleService mock = ContextualMocker.mock(SampleService.class);
        ContextID ctx = new StringContextId("matcher");

        ContextualMocker.given(mock)
            .forContext(ctx)
            .when(mock.greet(ArgumentMatchers.any(), ArgumentMatchers.eq(2)))
            .thenReturn("Any2");

        mock.greet("B", 2);
        mock.greet("C", 2);

        ContextualMocker.verify(mock)
            .forContext(ctx)
            .verify(ContextualMocker.times(1))
            .greet("B", 2);

        ContextualMocker.verify(mock)
            .forContext(ctx)
            .verify(ContextualMocker.times(1))
            .greet("C", 2);
    }

    @Test
    void testInterleavedStubbingAndInvocation() {
        SampleService mock = ContextualMocker.mock(SampleService.class);
        ContextID ctx = new StringContextId("interleaved");

        ContextualMocker.given(mock)
            .forContext(ctx)
            .when(mock.greet(ArgumentMatchers.eq("X"), ArgumentMatchers.eq(10)))
            .thenReturn("X10");

        mock.greet("X", 10);

        ContextualMocker.given(mock)
            .forContext(ctx)
            .when(mock.greet(ArgumentMatchers.eq("X"), ArgumentMatchers.eq(10)))
            .thenReturn("X10-again");

        mock.greet("X", 10);

        ContextualMocker.verify(mock)
            .forContext(ctx)
            .verify(ContextualMocker.times(2))
            .greet("X", 10);
    }

    @Test
    void testStubbingWithoutInvocation() {
        SampleService mock = ContextualMocker.mock(SampleService.class);
        ContextID ctx = new StringContextId("noinvocation");

        ContextualMocker.given(mock)
            .forContext(ctx)
            .when(mock.greet(ArgumentMatchers.eq("Z"), ArgumentMatchers.eq(99)))
            .thenReturn("Z99");

        ContextualMocker.verify(mock)
            .forContext(ctx)
            .verify(ContextualMocker.times(0))
            .greet("Z", 99);
    }

    @Test
    void testStubbingInOneContextInvocationInAnother() {
        SampleService mock = ContextualMocker.mock(SampleService.class);
        ContextID ctx1 = new StringContextId("ctx1");
        ContextID ctx2 = new StringContextId("ctx2");

        ContextualMocker.given(mock)
            .forContext(ctx1)
            .when(mock.greet(ArgumentMatchers.eq("Y"), ArgumentMatchers.eq(5)))
            .thenReturn("Y5");

        mock.greet("Y", 5);

        ContextualMocker.verify(mock)
            .forContext(ctx2)
            .verify(ContextualMocker.times(0))
            .greet("Y", 5);

        ContextualMocker.verify(mock)
            .forContext(ctx1)
            .verify(ContextualMocker.times(1))
            .greet("Y", 5);
    }
}