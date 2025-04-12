package com.contextualmocker.core;

import java.util.List;

public interface SimpleService {
    String greet(String name);
    void doSomething();
    List<String> getList(int size);
}