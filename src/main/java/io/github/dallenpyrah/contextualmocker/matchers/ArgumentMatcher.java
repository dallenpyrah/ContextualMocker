package io.github.dallenpyrah.contextualmocker.matchers;

public interface ArgumentMatcher<T> {
    boolean matches(Object argument);
}