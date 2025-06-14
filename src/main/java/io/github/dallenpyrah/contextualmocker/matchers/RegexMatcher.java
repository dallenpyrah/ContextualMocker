package io.github.dallenpyrah.contextualmocker.matchers;

import java.util.regex.Pattern;

/**
 * Matcher that matches strings against a regular expression pattern.
 */
public class RegexMatcher implements ArgumentMatcher {
    private final Pattern pattern;

    public RegexMatcher(String regex) {
        this.pattern = Pattern.compile(regex);
    }

    public RegexMatcher(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean matches(Object argument) {
        if (!(argument instanceof String)) {
            return false;
        }
        String str = (String) argument;
        return pattern.matcher(str).matches();
    }

    @Override
    public String toString() {
        return "matches(\"" + pattern.pattern() + "\")";
    }
}