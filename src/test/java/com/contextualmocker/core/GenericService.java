package com.contextualmocker.core;

import java.util.List;

public interface GenericService<T> {
    T process(T input);
    List<T> getItems();
}