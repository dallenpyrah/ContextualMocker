package com.contextualmocker.matchers;

/**
 * Matcher that matches strings containing a specific substring.
 */
public class ContainsMatcher implements ArgumentMatcher {
    private final String substring;

    public ContainsMatcher(String substring) {
        this.substring = substring;
    }

    @Override
    public boolean matches(Object argument) {
        if (!(argument instanceof String)) {
            return false;
        }
        String str = (String) argument;
        return str.contains(substring);
    }

    @Override
    public String toString() {
        return "contains(\"" + substring + "\")";
    }
}