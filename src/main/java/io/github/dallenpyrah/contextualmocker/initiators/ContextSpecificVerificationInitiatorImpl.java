package io.github.dallenpyrah.contextualmocker.initiators;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import io.github.dallenpyrah.contextualmocker.api.ContextSpecificVerificationInitiator;
import io.github.dallenpyrah.contextualmocker.core.ContextualVerificationMode;
import io.github.dallenpyrah.contextualmocker.core.ContextID;
import io.github.dallenpyrah.contextualmocker.core.ContextHolder;
import io.github.dallenpyrah.contextualmocker.core.DefaultValueProvider;
import io.github.dallenpyrah.contextualmocker.matchers.ArgumentMatcher;
import io.github.dallenpyrah.contextualmocker.matchers.MatcherContext;
import io.github.dallenpyrah.contextualmocker.core.InvocationRecord;
import io.github.dallenpyrah.contextualmocker.core.MockRegistry;
import io.github.dallenpyrah.contextualmocker.handlers.VerificationMethodCaptureHandler;

public class ContextSpecificVerificationInitiatorImpl<T> implements ContextSpecificVerificationInitiator<T> {
    private final T mock;

    public ContextSpecificVerificationInitiatorImpl(T mock) {
        this.mock = mock;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T verify(ContextualVerificationMode mode) {
        Objects.requireNonNull(ContextHolder.getContext(), "ContextID must be set before verification");
        VerificationMethodCaptureHandler<T> handler = new VerificationMethodCaptureHandler<>(mock, mode, ContextHolder.getContext());
        return (T) Proxy.newProxyInstance(
                mock.getClass().getClassLoader(),
                mock.getClass().getInterfaces(),
                handler
        );
    }

    @Override
    public void that(ContextualVerificationMode mode, java.util.function.Supplier<?> methodCall) {
        Objects.requireNonNull(ContextHolder.getContext(), "ContextID must be set before verification");
        Objects.requireNonNull(mode, "Verification mode cannot be null");
        Objects.requireNonNull(methodCall, "Method call supplier cannot be null");
        
        VerificationMethodCaptureHandler<T> handler = new VerificationMethodCaptureHandler<>(mock, mode, ContextHolder.getContext());
        @SuppressWarnings("unchecked")
        T verificationProxy = (T) Proxy.newProxyInstance(
                mock.getClass().getClassLoader(),
                mock.getClass().getInterfaces(),
                handler
        );
        methodCall.get();
    }

    public static class VerificationMethodCaptureHandler<T> implements InvocationHandler {
        private final T mock;
        private final ContextualVerificationMode mode;
        private final ContextID contextId;
        private Method method;
        private Object[] args;
        private List<ArgumentMatcher<?>> verificationMatchers;

        VerificationMethodCaptureHandler(T mock, ContextualVerificationMode mode, ContextID contextId) {
            this.mock = mock;
            this.mode = mode;
            this.contextId = contextId;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (this.method == null) {
                this.method = method;
                this.args = args != null ? args.clone() : new Object[0];
                this.verificationMatchers = MatcherContext.consumeMatchers();
                boolean useVerificationMatchers = this.verificationMatchers != null && !this.verificationMatchers.isEmpty();
                
                List<InvocationRecord> invocations = MockRegistry.getInvocationRecords(mock, contextId);
                List<InvocationRecord> matchingRecords = new ArrayList<>();
                
                for (InvocationRecord record : invocations) {
                    if (!record.getMethod().equals(method)) continue;

                    Object[] actualArgs = record.getArguments();
                    boolean argumentsMatch = false;

                    if (actualArgs.length != this.args.length) continue;

                    if (useVerificationMatchers) {
                        boolean allMatch = true;
                        for (int i = 0; i < this.args.length; i++) {
                            boolean hasMatcherForArg = i < this.verificationMatchers.size() && this.verificationMatchers.get(i) != null;
                            
                            if (hasMatcherForArg) {
                                @SuppressWarnings("unchecked")
                                ArgumentMatcher<Object> objectMatcher = (ArgumentMatcher<Object>) this.verificationMatchers.get(i);
                                if (!objectMatcher.matches(actualArgs[i])) {
                                    allMatch = false;
                                    break;
                                }
                            } else {
                                // No matcher for this argument, use direct comparison
                                if (!Objects.deepEquals(this.args[i], actualArgs[i])) {
                                    allMatch = false;
                                    break;
                                }
                            }
                        }
                        argumentsMatch = allMatch;
                    } else {
                        // No matchers, simply use deep equality for all arguments
                        argumentsMatch = Objects.deepEquals(this.args, actualArgs);
                    }

                    if (argumentsMatch) {
                        matchingRecords.add(record);
                    }
                }
                
                int matchCount = matchingRecords.size();

                // Use enhanced verification with full context
                mode.verifyCountWithContext(matchCount, method, args, mock, contextId, invocations);

                for (InvocationRecord record : matchingRecords) {
                    record.markVerified();
                }
            }
            return getDefaultValue(method.getReturnType());
        }

        private Object getDefaultValue(Class<?> type) {
            return DefaultValueProvider.getDefaultValue(type);
        }
    }
}