package com.contextualmocker.handlers;
import com.contextualmocker.core.ContextualMocker;
import com.contextualmocker.core.ContextID;
import com.contextualmocker.matchers.ArgumentMatcher;
import com.contextualmocker.matchers.MatcherContext;
import com.contextualmocker.core.InvocationRecord;
import com.contextualmocker.core.MockRegistry;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

public class VerificationMethodCaptureHandler<T> implements InvocationHandler {
    private final T mock;
    private final ContextualMocker.ContextualVerificationMode mode;
    private final ContextID contextId;
    private Method method;
    private Object[] args;

    VerificationMethodCaptureHandler(T mock, ContextualMocker.ContextualVerificationMode mode, ContextID contextId) {
        this.mock = mock;
        this.mode = mode;
        this.contextId = contextId;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] methodArgs) {
        if (this.method == null) {
            this.method = method;
            this.args = methodArgs != null ? methodArgs.clone() : new Object[0];
            
            // Get the matchers that were specified during verification
            List<ArgumentMatcher<?>> verificationMatchers = MatcherContext.consumeMatchers();
            boolean hasVerificationMatchers = verificationMatchers != null && !verificationMatchers.isEmpty();
            
            List<InvocationRecord> invocations = MockRegistry.getInvocationRecords(mock, contextId);
            System.out.println("[DEBUG] Found " + invocations.size() + " invocation records for mock in context " + contextId);
            System.out.println("[DEBUG] Verifying method: " + method.getName());
            System.out.println("[DEBUG] Args: " + java.util.Arrays.toString(args));
            System.out.println("[DEBUG] Verification Matchers: " + verificationMatchers);
            
            int matchCount = 0;
            
            // Match against each recorded invocation
            for (InvocationRecord record : invocations) {
                if (record.getMethod().equals(method)) {
                    Object[] recordArgs = record.getArguments();
                    System.out.println("[DEBUG] Checking record args: " + java.util.Arrays.toString(recordArgs));
                    
                    boolean matches = true;
                    
                    // Use verification matchers if we have them
                    if (hasVerificationMatchers) {
                        if (recordArgs.length != args.length) {
                            continue; // Different arg count, can't match
                        }
                        
                        for (int i = 0; i < args.length; i++) {
                            // Check if we have a matcher for this argument position
                            ArgumentMatcher<?> matcher = (i < verificationMatchers.size()) ? verificationMatchers.get(i) : null;
                            
                            if (matcher != null) {
                                System.out.println("[DEBUG] Using matcher at position " + i + ": " + matcher);
                                // Use the matcher for this argument
                                if (!matcher.matches(recordArgs[i])) {
                                    matches = false;
                                    break;
                                }
                            } else {
                                // No matcher for this position, use equals
                                if (!Objects.deepEquals(args[i], recordArgs[i])) {
                                    matches = false;
                                    break;
                                }
                            }
                        }
                    } else {
                        // No matchers, use strict equals
                        matches = Objects.deepEquals(recordArgs, args);
                    }
                    
                    if (matches) {
                        matchCount++;
                        System.out.println("[DEBUG] Found matching invocation #" + matchCount);
                    }
                }
            }
            
            System.out.println("[DEBUG] Total matching invocations: " + matchCount);
            
            // Apply the appropriate verification mode
            if (mode instanceof ContextualMocker.TimesVerificationMode) {
                ((ContextualMocker.TimesVerificationMode) mode).verifyCount(matchCount, method, args);
            } else if (mode instanceof ContextualMocker.AtLeastVerificationMode) {
                ((ContextualMocker.AtLeastVerificationMode) mode).verifyCount(matchCount, method, args);
            } else if (mode instanceof ContextualMocker.AtMostVerificationMode) {
                ((ContextualMocker.AtMostVerificationMode) mode).verifyCount(matchCount, method, args);
            }
        }
        return getDefaultValue(method.getReturnType());
    }

    private Object getDefaultValue(Class<?> type) {
        if (!type.isPrimitive())
            return null;
        if (type == boolean.class)
            return false;
        if (type == byte.class)
            return (byte) 0;
        if (type == short.class)
            return (short) 0;
        if (type == int.class)
            return 0;
        if (type == long.class)
            return 0L;
        if (type == float.class)
            return 0.0f;
        if (type == double.class)
            return 0.0d;
        if (type == char.class)
            return '\u0000';
        return null;
    }
}
