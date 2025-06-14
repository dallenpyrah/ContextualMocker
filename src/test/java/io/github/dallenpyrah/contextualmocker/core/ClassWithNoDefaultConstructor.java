package io.github.dallenpyrah.contextualmocker.core;

public class ClassWithNoDefaultConstructor {
    private final String value;

    public ClassWithNoDefaultConstructor(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String greet(String name) {
        return "Hi, " + name + " (" + value + ")";
    }
}