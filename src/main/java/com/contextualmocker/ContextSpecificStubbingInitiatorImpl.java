package com.contextualmocker;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

class ContextSpecificStubbingInitiatorImpl<T> implements ContextualMocker.ContextSpecificStubbingInitiator<T> {
    private final T mock;
    private Object requiredState;
    private Object nextState;

    ContextSpecificStubbingInitiatorImpl(T mock) {
        this.mock = mock;
    }


    @Override
    @SuppressWarnings("unchecked")
    public <R> ContextualMocker.OngoingContextualStubbing<T, R> when(R methodCall) {
        Objects.requireNonNull(ContextHolder.getContext(), "ContextID must be set before stubbing");
        com.contextualmocker.ContextualInvocationHandler.setStubbingInProgress(true);
        // The method call (methodCall) has already happened and should have been intercepted
        // by ContextualInvocationHandler if stubbingInProgress was true.
        // However, the flag wasn't set *before* the call.

        // Correct approach: Set flag, evaluate call, unset flag, consume.
        // No need to set a stubbing flag; the invocation handler always records the last invocation
        // The method call (methodCall) has already been evaluated and should have been recorded
        // by ContextualInvocationHandler in ThreadLocals

            // Let's reconsider the proxy approach, but fix the implementation.
            // when(R methodCall) should not exist.
            // when() should return a proxy.
            // The user calls mockProxy.method().
            // thenReturn() etc. retrieve the captured info.


            //    return new OngoingStubbing(handler.method, handler.args);
            //    ...

        Method method = ContextualInvocationHandler.consumeLastInvokedMethod();
        Object[] args = ContextualInvocationHandler.consumeLastInvokedArgs();
        if (method == null) {
            throw new IllegalStateException("Failed to capture stubbed method via ThreadLocal. " +
                                            "Ensure the method call uses the mock object directly.");
        }
        com.contextualmocker.ContextualInvocationHandler.setStubbingInProgress(false);
        MockRegistry.removeLastInvocation(mock, ContextHolder.getContext());
        return new OngoingContextualStubbingImpl<>(mock, ContextHolder.getContext(), method, args);
    }
    }


    class OngoingContextualStubbingImpl<T, R> implements ContextualMocker.OngoingContextualStubbing<T, R> {
        private final T mock;
        private final ContextID contextId;
        private final Method method;
        private final Object[] args;
        private Object requiredState;
        private Object nextState;

        OngoingContextualStubbingImpl(T mock, ContextID contextId, Method method, Object[] args) {
            this.mock = mock;
            this.contextId = contextId;
            this.method = Objects.requireNonNull(method, "Method cannot be null in OngoingStubbing");
            this.args = args;
            System.out.println("[DEBUG] Creating OngoingContextualStubbingImpl for method: " + method.getName() + " with args: " + java.util.Arrays.toString(args));
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
        public ContextualMocker.ContextSpecificStubbingInitiator<T> thenReturn(R value) {
            var matchers = MatcherContext.consumeMatchers();
            System.out.println("[DEBUG] thenReturn matchers: " + matchers + " for " + method.getName());
            
            StubbingRule.Builder builder = new StubbingRule.Builder(method)
                .expectedArguments(args);
                
            // Only set argument matchers if we have any
            if (!matchers.isEmpty()) {
                System.out.println("[DEBUG] Using argument matchers: " + matchers);
                builder.argumentMatchers(matchers.toArray(new ArgumentMatcher<?>[0]));
            }
            
            builder.returnValue(value)
                .requiredState(requiredState)
                .nextState(nextState);
                
            StubbingRule rule = builder.build();
            System.out.println("[DEBUG] Created stubbing rule: " + rule);
            registerRule(rule);
            return new ContextSpecificStubbingInitiatorImpl<>(mock);
        }

        @Override
        public ContextualMocker.ContextSpecificStubbingInitiator<T> thenThrow(Throwable throwable) {
            var matchers = MatcherContext.consumeMatchers();
            System.out.println("[DEBUG] thenThrow matchers: " + matchers + " for " + method.getName());
            
            StubbingRule.Builder builder = new StubbingRule.Builder(method)
                .expectedArguments(args);
                
            // Only set argument matchers if we have any
            if (!matchers.isEmpty()) {
                System.out.println("[DEBUG] Using argument matchers: " + matchers);
                builder.argumentMatchers(matchers.toArray(new ArgumentMatcher<?>[0]));
            }
            
            builder.throwable(throwable)
                .requiredState(requiredState)
                .nextState(nextState);
                
            StubbingRule rule = builder.build();
            System.out.println("[DEBUG] Created stubbing rule: " + rule);
            registerRule(rule);
            return new ContextSpecificStubbingInitiatorImpl<>(mock);
        }

        @Override
        public ContextualMocker.ContextSpecificStubbingInitiator<T> thenAnswer(ContextualAnswer<R> answer) {
            var matchers = MatcherContext.consumeMatchers();
            System.out.println("[DEBUG] thenAnswer matchers: " + matchers + " for " + method.getName());
            
            StubbingRule.Builder builder = new StubbingRule.Builder(method)
                .expectedArguments(args);
                
            // Only set argument matchers if we have any
            if (!matchers.isEmpty()) {
                System.out.println("[DEBUG] Using argument matchers: " + matchers);
                builder.argumentMatchers(matchers.toArray(new ArgumentMatcher<?>[0]));
            }
            
            builder.answer(answer)
                .requiredState(requiredState)
                .nextState(nextState);
                
            StubbingRule rule = builder.build();
            System.out.println("[DEBUG] Created stubbing rule: " + rule);
            registerRule(rule);
            return new ContextSpecificStubbingInitiatorImpl<>(mock);
        }

        private void registerRule(StubbingRule rule) {
            MockRegistry.addStubbingRule(mock, contextId, rule);
        }
    }