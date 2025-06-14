package io.github.dallenpyrah.contextualmocker.core;

import io.github.dallenpyrah.contextualmocker.matchers.ArgumentMatcher;
import java.lang.reflect.Method;

/**
 * Strategy interface for matching method invocations in stubbing rules.
 * Following Open/Closed Principle - open for extension, closed for modification.
 */
public interface MatchingStrategy {
    /**
     * Determines if the given invocation matches this strategy's criteria.
     * 
     * @param invokedMethod The method being invoked
     * @param invokedArguments The arguments passed to the method
     * @param currentState The current state of the mock
     * @param expectedMethod The expected method
     * @param expectedArguments The expected arguments
     * @param argumentMatchers The argument matchers (if any)
     * @param requiredState The required state (if any)
     * @return true if the invocation matches, false otherwise
     */
    boolean matches(Method invokedMethod, Object[] invokedArguments, Object currentState,
                   Method expectedMethod, Object[] expectedArguments, 
                   ArgumentMatcher<?>[] argumentMatchers, Object requiredState);
}
