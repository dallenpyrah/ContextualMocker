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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerificationMethodCaptureHandler<T> implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(VerificationMethodCaptureHandler.class);
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
            logger.debug("Found {} invocation records for mock in context {}", invocations.size(), contextId);
            logger.debug("Verifying method: {}", method.getName());
            logger.debug("Args: {}", java.util.Arrays.toString(args));
            logger.debug("Verification Matchers: {}", verificationMatchers);
            
            int matchCount = 0;
            
            // Match against each recorded invocation
            for (InvocationRecord record : invocations) {
                if (record.getMethod().equals(method)) {
                    Object[] recordArgs = record.getArguments();
                    logger.debug("Checking record args: {}", java.util.Arrays.toString(recordArgs));
                    
                    boolean matches = true;
                    
                    // Use verification matchers if we have them
                    if (hasVerificationMatchers) {
                        if (recordArgs.length != args.length) {
                            continue;
                        }
                        
                        for (int i = 0; i < args.length; i++) {
                            // Check if we have a matcher for this argument position
                            ArgumentMatcher<?> matcher = (i < verificationMatchers.size()) ? verificationMatchers.get(i) : null;
                            
                            if (matcher != null) {
                                logger.debug("Using matcher at position {}: {}", i, matcher);
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
                        logger.debug("Found matching invocation #{}", matchCount);
                    }
                }
            }
            
            logger.debug("Total matching invocations: {}", matchCount);
            
            // Apply the appropriate verification mode with full context
            mode.verifyCountWithContext(matchCount, method, args, mock, contextId, invocations);
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
