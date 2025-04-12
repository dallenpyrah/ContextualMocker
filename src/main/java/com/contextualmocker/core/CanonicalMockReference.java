package com.contextualmocker.core;

import java.lang.ref.WeakReference;

/**
 * Canonical identity wrapper for a mock instance, used as a key in registry maps.
 * Equality and hashCode are based on System.identityHashCode and ==, not .equals().
 */
public final class CanonicalMockReference extends WeakReference<Object> {
    private final int identityHash;

    public CanonicalMockReference(Object referent) {
        super(referent);
        this.identityHash = System.identityHashCode(referent);
    }

    @Override
    public int hashCode() {
        return identityHash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CanonicalMockReference)) return false;
        Object a = this.get();
        Object b = ((CanonicalMockReference) obj).get();
        return a != null && b != null && a == b;
    }
}
