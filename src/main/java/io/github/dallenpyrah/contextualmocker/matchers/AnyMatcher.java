package io.github.dallenpyrah.contextualmocker.matchers;

public class AnyMatcher implements ArgumentMatcher<Object> {

    @Override
    public boolean matches(Object argument) {
        return true;
    }

    @Override
    public String toString() {
        return "AnyMatcher";
    }
}