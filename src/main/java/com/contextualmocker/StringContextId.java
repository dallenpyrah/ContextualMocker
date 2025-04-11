package com.contextualmocker;

import java.util.Objects;

public final class StringContextId implements ContextID {
    private final String id;

    public StringContextId(String id) {
        this.id = Objects.requireNonNull(id, "Context ID string cannot be null");
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringContextId that = (StringContextId) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "StringContextId{" +
               "id='" + id + '\'' +
               '}';
    }
}