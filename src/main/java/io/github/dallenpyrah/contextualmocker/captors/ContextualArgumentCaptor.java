package io.github.dallenpyrah.contextualmocker.captors;

import io.github.dallenpyrah.contextualmocker.core.ContextID;
import io.github.dallenpyrah.contextualmocker.matchers.ArgumentMatchers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Captures argument values passed to mocked methods for later assertions.
 * This class provides both global and context-specific capture capabilities,
 * allowing tests to verify arguments in different execution contexts.
 *
 * <p>Example usage:</p>
 * <pre>
 * // Create a captor for String arguments
 * ContextualArgumentCaptor&lt;String&gt; captor = ContextualArgumentCaptor.forClass(String.class);
 * 
 * // Use in stubbing or verification
 * when(mock.someMethod(captor.capture())).thenReturn("result");
 * 
 * // Retrieve captured values
 * String capturedValue = captor.getValue();
 * List&lt;String&gt; allValues = captor.getAllValues();
 * 
 * // Context-specific retrieval
 * String contextValue = captor.getValueForContext(contextId);
 * List&lt;String&gt; contextValues = captor.getAllValuesForContext(contextId);
 * </pre>
 *
 * @param <T> The type of argument to capture
 */
public class ContextualArgumentCaptor<T> {
    
    private final Class<T> clazz;
    private final List<T> globalCaptures;
    private final Map<ContextID, List<T>> contextCaptures;
    
    /**
     * Private constructor. Use {@link #forClass(Class)} to create instances.
     *
     * @param clazz The class of the argument type to capture
     */
    private ContextualArgumentCaptor(Class<T> clazz) {
        this.clazz = Objects.requireNonNull(clazz, "Class cannot be null");
        this.globalCaptures = new CopyOnWriteArrayList<>();
        this.contextCaptures = new ConcurrentHashMap<>();
    }
    
    /**
     * Creates a new ArgumentCaptor for the specified class.
     *
     * @param <T> The type of argument to capture
     * @param clazz The class of the argument type to capture
     * @return A new ContextualArgumentCaptor instance
     * @throws NullPointerException if clazz is null
     */
    public static <T> ContextualArgumentCaptor<T> forClass(Class<T> clazz) {
        return new ContextualArgumentCaptor<>(clazz);
    }
    
    /**
     * Captures an argument value. This method should be used in place of actual arguments
     * when stubbing or verifying mock interactions.
     *
     * @return null (the actual capturing is handled by the registered matcher)
     */
    public T capture() {
        return ArgumentMatchers.captureWith(clazz, globalCaptures, contextCaptures);
    }
    
    /**
     * Returns the last captured value globally (across all contexts).
     *
     * @return The last captured value
     * @throws IllegalStateException if no values have been captured
     */
    public T getValue() {
        if (globalCaptures.isEmpty()) {
            throw new IllegalStateException("No argument value captured");
        }
        return globalCaptures.get(globalCaptures.size() - 1);
    }
    
    /**
     * Returns all captured values globally (across all contexts).
     *
     * @return A list of all captured values in the order they were captured
     */
    public List<T> getAllValues() {
        return new CopyOnWriteArrayList<>(globalCaptures);
    }
    
    /**
     * Returns the last captured value for a specific context.
     *
     * @param contextId The context ID to retrieve values for
     * @return The last captured value in the specified context
     * @throws IllegalStateException if no values have been captured for the context
     * @throws NullPointerException if contextId is null
     */
    public T getValueForContext(ContextID contextId) {
        Objects.requireNonNull(contextId, "ContextID cannot be null");
        
        List<T> contextValues = contextCaptures.get(contextId);
        if (contextValues == null || contextValues.isEmpty()) {
            throw new IllegalStateException("No argument value captured for context: " + contextId);
        }
        return contextValues.get(contextValues.size() - 1);
    }
    
    /**
     * Returns all captured values for a specific context.
     *
     * @param contextId The context ID to retrieve values for
     * @return A list of all captured values in the specified context
     * @throws NullPointerException if contextId is null
     */
    public List<T> getAllValuesForContext(ContextID contextId) {
        Objects.requireNonNull(contextId, "ContextID cannot be null");
        
        List<T> contextValues = contextCaptures.get(contextId);
        if (contextValues == null) {
            return new CopyOnWriteArrayList<>();
        }
        return new CopyOnWriteArrayList<>(contextValues);
    }
    
    /**
     * Clears all captured values (both global and context-specific).
     * This method is useful for resetting the captor between test cases.
     */
    public void reset() {
        globalCaptures.clear();
        contextCaptures.clear();
    }
    
    /**
     * Returns the number of values captured globally.
     *
     * @return The total number of captured values across all contexts
     */
    public int getCaptureCount() {
        return globalCaptures.size();
    }
    
    /**
     * Returns the number of values captured for a specific context.
     *
     * @param contextId The context ID to check
     * @return The number of captured values in the specified context
     * @throws NullPointerException if contextId is null
     */
    public int getCaptureCountForContext(ContextID contextId) {
        Objects.requireNonNull(contextId, "ContextID cannot be null");
        
        List<T> contextValues = contextCaptures.get(contextId);
        return contextValues == null ? 0 : contextValues.size();
    }
    
    @Override
    public String toString() {
        return "ContextualArgumentCaptor<" + clazz.getSimpleName() + "> [" +
               "globalCaptures=" + globalCaptures.size() + ", " +
               "contexts=" + contextCaptures.size() + "]";
    }
}