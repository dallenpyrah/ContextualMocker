package com.contextualmocker.initiators;
import com.contextualmocker.core.ContextualMocker;
import com.contextualmocker.core.ContextHolder;
import com.contextualmocker.handlers.ContextualInvocationHandler;
import com.contextualmocker.core.MockRegistry;
import com.contextualmocker.core.StubbingRule;
import com.contextualmocker.matchers.MatcherContext;
import com.contextualmocker.matchers.ArgumentMatcher;
import com.contextualmocker.core.ContextID;
import com.contextualmocker.handlers.ContextualAnswer;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Collections;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextSpecificStubbingInitiatorImpl<T> implements ContextualMocker.ContextSpecificStubbingInitiator<T> {
   private static final Logger logger = LoggerFactory.getLogger(ContextSpecificStubbingInitiatorImpl.class);
    private final T mock;
    private Object requiredState;
    private Object nextState;

    public ContextSpecificStubbingInitiatorImpl(T mock) {
        this.mock = mock;
    }


    @Override
    @SuppressWarnings("unchecked")
    public <R> ContextualMocker.OngoingContextualStubbing<T, R> when(java.util.function.Supplier<R> methodCallSupplier) {
        Objects.requireNonNull(ContextHolder.getContext(), "ContextID must be set before stubbing");
        ContextualInvocationHandler.setStubbingInProgress(true);
        try {
            methodCallSupplier.get();
        } finally {
            ContextualInvocationHandler.setStubbingInProgress(false);
        }
        Method method = ContextualInvocationHandler.consumeLastInvokedMethod();
        Object[] args = ContextualInvocationHandler.consumeLastInvokedArgs();
        var matchers = ContextualInvocationHandler.consumeLastInvokedMatchers();
        if (method == null) {
            throw new IllegalStateException("Failed to capture stubbed method via ThreadLocal. " +
                                            "Ensure the method call uses the mock object directly.");
        }
        MockRegistry.removeLastInvocation(mock, ContextHolder.getContext());
        return new OngoingContextualStubbingImpl<T, R>(mock, ContextHolder.getContext(), method, args, matchers);
    }
    }


    class OngoingContextualStubbingImpl<T, R> implements ContextualMocker.OngoingContextualStubbing<T, R> {
        private static final Logger logger = LoggerFactory.getLogger(OngoingContextualStubbingImpl.class);
        private final T mock;
        private final ContextID contextId;
        private final Method method;
        private final Object[] args;
        private Object requiredState;
        private Object nextState;

        private final List<ArgumentMatcher<?>> capturedMatchers;
        private long ttlMillis = 0; // Added field for TTL

        OngoingContextualStubbingImpl(T mock, ContextID contextId, Method method, Object[] args, List<ArgumentMatcher<?>> matchers) {
            this.mock = mock;
            this.contextId = contextId;
            this.method = Objects.requireNonNull(method, "Method cannot be null in OngoingStubbing");
            this.args = args;
            this.capturedMatchers = matchers != null ? matchers : java.util.Collections.emptyList();
            logger.debug("Creating OngoingContextualStubbingImpl for method: {} with args: {}", method.getName(), java.util.Arrays.toString(args));
        }


        @Override
        public OngoingContextualStubbingImpl<T, R> whenStateIs(Object state) {
            this.requiredState = state;
            return this;
        }

        @Override
        public OngoingContextualStubbingImpl<T, R> willSetStateTo(Object newState) {
            this.nextState = newState;
            return this;
        }

        @Override
        public OngoingContextualStubbingImpl<T, R> ttlMillis(long ttlMillis) {
            this.ttlMillis = ttlMillis;
            return this;
        }

        @Override
        public ContextualMocker.ContextSpecificStubbingInitiator<T> thenReturn(R value) {
            var matchers = this.capturedMatchers;
            logger.debug("thenReturn matchers: {} for {}", matchers, method.getName());

            StubbingRule.Builder builder = new StubbingRule.Builder(method);

            // Set argument matchers ONLY if they were explicitly provided
            if (matchers != null && !matchers.isEmpty()) {
                builder.argumentMatchers(matchers.toArray(new ArgumentMatcher<?>[0]));
            }
            builder.expectedArguments(args); // Always set expected args

            builder.returnValue(value)
                .requiredState(requiredState)
                .nextState(nextState)
                .ttlMillis(this.ttlMillis); // Pass TTL to builder

            StubbingRule rule = builder.build();
            logger.debug("Created stubbing rule: {}", rule);
            registerRule(rule);
            return new ContextSpecificStubbingInitiatorImpl<>(mock);
        }

        @Override
        public ContextualMocker.ContextSpecificStubbingInitiator<T> thenThrow(Throwable throwable) {
            var matchers = this.capturedMatchers;
            logger.debug("thenThrow matchers: {} for {}", matchers, method.getName());

            StubbingRule.Builder builder = new StubbingRule.Builder(method);

            // Set argument matchers ONLY if they were explicitly provided
            if (matchers != null && !matchers.isEmpty()) {
                builder.argumentMatchers(matchers.toArray(new ArgumentMatcher<?>[0]));
            }
            builder.expectedArguments(args); // Always set expected args

            builder.throwable(throwable)
                .requiredState(requiredState)
                .nextState(nextState)
                .ttlMillis(this.ttlMillis); // Pass TTL to builder

            StubbingRule rule = builder.build();
            logger.debug("Created stubbing rule: {}", rule);
            registerRule(rule);
            return new ContextSpecificStubbingInitiatorImpl<>(mock);
        }

        @Override
        public ContextualMocker.ContextSpecificStubbingInitiator<T> thenAnswer(ContextualAnswer<R> answer) {
            var matchers = this.capturedMatchers;
            logger.debug("thenAnswer matchers: {} for {}", matchers, method.getName());

            StubbingRule.Builder builder = new StubbingRule.Builder(method);

            // Set argument matchers ONLY if they were explicitly provided
            if (matchers != null && !matchers.isEmpty()) {
                builder.argumentMatchers(matchers.toArray(new ArgumentMatcher<?>[0]));
            }
            builder.expectedArguments(args); // Always set expected args

            builder.answer(answer)
                .requiredState(requiredState)
                .nextState(nextState)
                .ttlMillis(this.ttlMillis); // Pass TTL to builder

            StubbingRule rule = builder.build();
            logger.debug("Created stubbing rule: {}", rule);
            registerRule(rule);
            return new ContextSpecificStubbingInitiatorImpl<>(mock);
        }

        private void registerRule(StubbingRule rule) {
            MockRegistry.addStubbingRule(mock, contextId, rule);
        }
    }
