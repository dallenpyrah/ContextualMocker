package com.contextualmocker.core;

public class ClassWithFinalMethod {
    public String sayHello() {
        return "Hello";
    }

    public final String finalMethod() {
        return "This is final";
    }
}