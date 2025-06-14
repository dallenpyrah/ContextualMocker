package io.github.dallenpyrah.contextualmocker.core;

import java.util.List;

public interface GenericService<T> {
    T process(T input);
    List<T> getItems();
}