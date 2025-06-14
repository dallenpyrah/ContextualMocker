package io.github.dallenpyrah.contextualmocker.initiators;
import io.github.dallenpyrah.contextualmocker.api.ContextSpecificStubbingInitiator;
import io.github.dallenpyrah.contextualmocker.api.OngoingContextualStubbing;
import io.github.dallenpyrah.contextualmocker.core.ContextHolder;
import io.github.dallenpyrah.contextualmocker.handlers.ContextualInvocationHandler;
import io.github.dallenpyrah.contextualmocker.handlers.SpyInvocationHandler;
import io.github.dallenpyrah.contextualmocker.core.MockRegistry;
import io.github.dallenpyrah.contextualmocker.core.StubbingRule;
import io.github.dallenpyrah.contextualmocker.matchers.MatcherContext;
import io.github.dallenpyrah.contextualmocker.matchers.ArgumentMatcher;
import io.github.dallenpyrah.contextualmocker.core.ContextID;
import io.github.dallenpyrah.contextualmocker.handlers.ContextualAnswer;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Collections;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextSpecificStubbingInitiatorImpl<T> implements ContextSpecificStubbingInitiator<T> {
   private static final Logger logger = LoggerFactory.getLogger(ContextSpecificStubbingInitiatorImpl.class);
    private final T mock;
    private Object requiredState;
    private Object nextState;

    public ContextSpecificStubbingInitiatorImpl(T mock) {
        this.mock = mock;
    }


    @Override
    @SuppressWarnings("unchecked")
    public <R> OngoingContextualStubbing<T, R> when(java.util.function.Supplier<R> methodCallSupplier) {
        Objects.requireNonNull(ContextHolder.getContext(), "ContextID must be set before stubbing");
        
        // Try to determine if this is a spy or regular mock by attempting to set stubbing progress on both handlers
        ContextualInvocationHandler.setStubbingInProgress(true);
        SpyInvocationHandler.setStubbingInProgress(true);
        
        try {
            methodCallSupplier.get();
        } finally {
            ContextualInvocationHandler.setStubbingInProgress(false);
            SpyInvocationHandler.setStubbingInProgress(false);
        }
        
        // Try to get method/args from regular mock handler first
        Method method = ContextualInvocationHandler.consumeLastInvokedMethod();
        Object[] args = ContextualInvocationHandler.consumeLastInvokedArgs();
        List<ArgumentMatcher<?>> matchers = ContextualInvocationHandler.consumeLastInvokedMatchers();
        
        // If not found, try spy handler
        if (method == null) {
            method = SpyInvocationHandler.consumeLastInvokedMethod();
            args = SpyInvocationHandler.consumeLastInvokedArgs();
            matchers = SpyInvocationHandler.consumeLastInvokedMatchers();
        }
        
        if (method == null) {
            throw new IllegalStateException("Failed to capture stubbed method via ThreadLocal. " +
                                            "Ensure the method call uses the mock object directly.");
        }
        MockRegistry.removeLastInvocation(mock, ContextHolder.getContext());
        return new OngoingContextualStubbingImpl<T, R>(mock, ContextHolder.getContext(), method, args, matchers);
    }
    }


    class OngoingContextualStubbingImpl<T, R> implements OngoingContextualStubbing<T, R> {
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
        public ContextSpecificStubbingInitiator<T> thenReturn(R value) {
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
        public ContextSpecificStubbingInitiator<T> thenThrow(Throwable throwable) {
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
        public ContextSpecificStubbingInitiator<T> thenAnswer(ContextualAnswer<R> answer) {
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
