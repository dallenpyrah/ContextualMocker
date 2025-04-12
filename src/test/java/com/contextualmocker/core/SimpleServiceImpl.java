package com.contextualmocker.core;

import java.util.List;

public class SimpleServiceImpl implements SimpleService {
    @Override
    public String greet(String name) { return "Hello, " + name; }
    @Override
    public void doSomething() { }
    @Override
    public List<String> getList(int size) { return List.of(); }
}