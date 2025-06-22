package io.github.dallenpyrah.contextualmocker.matchers;

import io.github.dallenpyrah.contextualmocker.core.ContextHolder;
import io.github.dallenpyrah.contextualmocker.core.ContextID;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Internal matcher implementation used by ContextualArgumentCaptor to capture argument values.
 * This class is package-private and should not be used directly.
 *
 * @param <T> The type of argument to capture
 */
class CapturingArgumentMatcher<T> implements ArgumentMatcher<T> {
    
    private final Class<T> clazz;
    private final List<T> globalCaptures;
    private final Map<ContextID, List<T>> contextCaptures;
    
    CapturingArgumentMatcher(Class<T> clazz, List<T> globalCaptures, Map<ContextID, List<T>> contextCaptures) {
        this.clazz = clazz;
        this.globalCaptures = globalCaptures;
        this.contextCaptures = contextCaptures;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public boolean matches(Object argument) {
        // Always return true to capture all values
        if (argument != null && !clazz.isInstance(argument)) {
            // Skip capturing if the type doesn't match
            return true;
        }
        
        T capturedValue = (T) argument;
        
        // Store globally
        globalCaptures.add(capturedValue);
        
        // Store per-context if a context is set
        ContextID currentContext = ContextHolder.getCurrentContextIfSet();
        if (currentContext != null) {
            contextCaptures.computeIfAbsent(currentContext, k -> new CopyOnWriteArrayList<>())
                          .add(capturedValue);
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        return "CapturingArgumentMatcher<" + clazz.getSimpleName() + ">";
    }
}