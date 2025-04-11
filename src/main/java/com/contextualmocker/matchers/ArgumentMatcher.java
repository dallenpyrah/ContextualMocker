package com.contextualmocker.matchers;

public interface ArgumentMatcher<T> {
    boolean matches(Object argument);
}