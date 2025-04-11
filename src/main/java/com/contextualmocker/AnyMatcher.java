package com.contextualmocker;

class AnyMatcher<T> implements ArgumentMatcher<T> {
    @Override
    public boolean matches(Object argument) {
        return true;
    }
    
    @Override
    public String toString() {
        return "AnyMatcher";
    }
}