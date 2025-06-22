package io.github.dallenpyrah.contextualmocker.matchers;

import io.github.dallenpyrah.contextualmocker.captors.ArgumentCaptor;

/**
 * A matcher that captures argument values during method invocation verification.
 * This class bridges the ArgumentCaptor functionality with the ArgumentMatcher interface,
 * allowing argument capture to work seamlessly within the mocking framework.
 * 
 * <p>This matcher always returns true, as its primary purpose is to capture arguments
 * rather than perform matching logic. The captured values can be retrieved through
 * the associated ArgumentCaptor instance.</p>
 * 
 * <p>This class is thread-safe as it delegates to the thread-safe ArgumentCaptor
 * and maintains no mutable state of its own.</p>
 * 
 * @param <T> The type of argument to capture
 */
class CapturingMatcher<T> implements ArgumentMatcher<T> {
    
    private final ArgumentCaptor<T> captor;
    
    /**
     * Creates a new CapturingMatcher with the specified ArgumentCaptor.
     * 
     * @param captor The ArgumentCaptor to use for recording captured values
     * @throws IllegalArgumentException if captor is null
     */
    CapturingMatcher(ArgumentCaptor<T> captor) {
        if (captor == null) {
            throw new IllegalArgumentException("ArgumentCaptor cannot be null");
        }
        this.captor = captor;
    }
    
    /**
     * Captures the argument value and always returns true.
     * This method records the argument value using the associated ArgumentCaptor
     * and returns true to indicate that any argument matches.
     * 
     * @param argument The argument to capture
     * @return always returns true
     */
    @Override
    public boolean matches(Object argument) {
        @SuppressWarnings("unchecked")
        T value = (T) argument;
        captor.recordValue(value);
        return true;
    }
}