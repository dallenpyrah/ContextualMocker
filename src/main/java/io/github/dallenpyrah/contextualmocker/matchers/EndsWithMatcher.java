package io.github.dallenpyrah.contextualmocker.matchers;

/**
 * Matcher that matches strings ending with a specific suffix.
 */
public class EndsWithMatcher implements ArgumentMatcher {
    private final String suffix;

    public EndsWithMatcher(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public boolean matches(Object argument) {
        if (!(argument instanceof String)) {
            return false;
        }
        String str = (String) argument;
        return str.endsWith(suffix);
    }

    @Override
    public String toString() {
        return "endsWith(\"" + suffix + "\")";
    }
}