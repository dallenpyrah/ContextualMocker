package io.github.dallenpyrah.contextualmocker.captors;

import io.github.dallenpyrah.contextualmocker.utils.DefaultValueProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ArgumentCaptor is used to capture argument values for further assertions.
 * 
 * @param <T> The type of the argument to capture
 */
public class ArgumentCaptor<T> {
    
    private final List<T> capturedValues;
    private final Class<T> clazz;
    
    /**
     * Private constructor - use forClass() factory method
     */
    private ArgumentCaptor(Class<T> clazz) {
        this.clazz = clazz;
        this.capturedValues = new CopyOnWriteArrayList<>(); // Thread-safe
    }
    
    /**
     * Creates an ArgumentCaptor for the given class
     */
    public static <T> ArgumentCaptor<T> forClass(Class<T> clazz) {
        return new ArgumentCaptor<>(clazz);
    }
    
    /**
     * Use this method in verify() calls to capture arguments
     */
    @SuppressWarnings("unchecked")
    public T capture() {
        // This will be replaced by the framework during verification
        // Return appropriate default value based on the type
        return (T) DefaultValueProvider.getDefaultValue(clazz);
    }
    
    /**
     * Returns the captured value. Use only after verification.
     * 
     * @throws IllegalStateException if no value was captured
     */
    public T getValue() {
        if (capturedValues.isEmpty()) {
            throw new IllegalStateException("No argument value captured");
        }
        return capturedValues.get(capturedValues.size() - 1);
    }
    
    /**
     * Returns all captured values
     */
    public List<T> getAllValues() {
        return Collections.unmodifiableList(new ArrayList<>(capturedValues));
    }
    
    /**
     * Internal method used by the framework to record captured values
     */
    public void recordValue(T value) {
        capturedValues.add(value);
    }
    
    /**
     * Clears all captured values
     */
    public void reset() {
        capturedValues.clear();
    }
    
    /**
     * Returns the class type this captor captures
     */
    public Class<T> getType() {
        return clazz;
    }
}