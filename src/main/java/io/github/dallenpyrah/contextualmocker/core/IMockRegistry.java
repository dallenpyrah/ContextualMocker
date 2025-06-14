package io.github.dallenpyrah.contextualmocker.core;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Interface for Mock Registry operations.
 * Following Dependency Inversion Principle - depend on abstractions, not concretions.
 */
public interface IMockRegistry {
    /**
     * Adds a new stubbing rule for a specific mock and context.
     */
    void addStubbingRule(Object mock, ContextID contextId, StubbingRule rule);
    
    /**
     * Finds the first matching stubbing rule for a given invocation.
     */
    StubbingRule findStubbingRule(Object mock, ContextID contextId, Method method, Object[] arguments, Object currentState);
    
    /**
     * Records an invocation on a mock instance.
     */
    void recordInvocation(InvocationRecord record);
    
    /**
     * Retrieves all non-stubbing invocation records for a specific mock and context.
     */
    List<InvocationRecord> getInvocationRecords(Object mock, ContextID contextId);
    
    /**
     * Clears all invocation records associated with a specific context ID across all mocks.
     */
    void clearInvocationsForContext(ContextID contextId);
    
    /**
     * Removes the most recently recorded invocation for a specific mock and context.
     */
    void removeLastInvocation(Object mock, ContextID contextId);
    
    /**
     * Gets the current state for a specific mock and context.
     */
    Object getState(Object mock, ContextID contextId);
    
    /**
     * Sets the state for a specific mock and context.
     */
    void setState(Object mock, ContextID contextId, Object newState);
    
    /**
     * Gets current memory usage statistics.
     */
    MockRegistry.MemoryUsageStats getMemoryUsageStats();
    
    /**
     * Manually triggers a comprehensive cleanup of all stored data.
     */
    MockRegistry.CleanupStats performCleanup();
    
    /**
     * Clears all data associated with a specific mock instance.
     */
    boolean clearMockData(Object mock);
    
    /**
     * Clears all data for all mocks.
     */
    void clearAllData();
}
