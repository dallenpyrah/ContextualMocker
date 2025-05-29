package com.contextualmocker.matchers;

/**
 * Matcher that matches strings starting with a specific prefix.
 */
public class StartsWithMatcher implements ArgumentMatcher {
    private final String prefix;

    public StartsWithMatcher(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public boolean matches(Object argument) {
        if (!(argument instanceof String)) {
            return false;
        }
        String str = (String) argument;
        return str.startsWith(prefix);
    }

    @Override
    public String toString() {
        return "startsWith(\"" + prefix + "\")";
    }
}