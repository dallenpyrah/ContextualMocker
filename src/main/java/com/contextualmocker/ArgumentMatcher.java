package com.contextualmocker;

public interface ArgumentMatcher<T> {
    boolean matches(Object argument);
}