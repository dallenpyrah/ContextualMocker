package io.github.dallenpyrah.contextualmocker.core;

import java.util.List;

public interface SimpleService {
    String greet(String name);
    void doSomething();
    List<String> getList(int size);
}