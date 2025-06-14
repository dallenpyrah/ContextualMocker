package io.github.dallenpyrah.contextualmocker.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.dallenpyrah.contextualmocker.core.ContextualMocker.*;

import java.util.UUID;

/**
 * Test class demonstrating the improved API usability features.
 */
public class ImprovedApiTest {

    private SimpleService mockService;
    private ContextID context1;
    private ContextID context2;

    @BeforeEach
    void setUp() {
        mockService = mock(SimpleService.class);
        context1 = new StringContextId(UUID.randomUUID().toString());
        context2 = new StringContextId(UUID.randomUUID().toString());
        ContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        ContextHolder.clearContext();
    }

    @Test
    void testDirectStubbingApi() {
        // Direct stubbing without fluent chain
        when(mockService, context1, () -> mockService.greet("World")).thenReturn("Hello");
        
        ContextHolder.setContext(context1);
        assertEquals("Hello", mockService.greet("World"));
        ContextHolder.clearContext();
    }

    @Test
    void testDirectVerificationApi() {
        ContextHolder.setContext(context1);
        mockService.greet("Test");
        ContextHolder.clearContext();
        
        // Direct verification without fluent chain
        verify(mockService, context1, times(1), () -> mockService.greet("Test"));
    }

    @Test
    void testContextualMockBuilder() {
        // Builder pattern for multiple operations in same context
        withContext(context1)
            .stub(mockService, () -> mockService.greet("Builder")).thenReturn("Builder Hello");
        
        ContextHolder.setContext(context1);
        assertEquals("Builder Hello", mockService.greet("Builder"));
        ContextHolder.clearContext();
        
        withContext(context1)
            .verify(mockService, times(1), () -> mockService.greet("Builder"))
            .verifyNoInteractions(mockService);
    }

    @Test
    void testContextScope() {
        // Automatic context management with try-with-resources
        try (ContextScope scope = scopedContext(context1)) {
            scope.when(mockService, () -> mockService.greet("Scope")).thenReturn("Scoped Hello");
            
            // Context is automatically active within the scope
            assertEquals("Scoped Hello", mockService.greet("Scope"));
            
            scope.verify(mockService, times(1), () -> mockService.greet("Scope"));
        }
        // Context is automatically restored here
        
        // Verify context was properly cleaned up
        assertNull(ContextHolder.getCurrentContextIfSet());
    }

    @Test
    void testStreamlinedVerificationApi() {
        ContextHolder.setContext(context1);
        mockService.greet("Stream");
        ContextHolder.clearContext();
        
        // New streamlined verification without double verify() calls
        verify(mockService).forContext(context1).that(times(1), () -> mockService.greet("Stream"));
    }

    @Test
    void testConvenienceMethods() {
        ContextHolder.setContext(context1);
        mockService.greet("Conv");
        mockService.doSomething();
        ContextHolder.clearContext();
        
        // Convenience methods for common patterns
        verifyOnce(mockService, context1, () -> mockService.greet("Conv"));
        verifyOnce(mockService, context1, () -> { mockService.doSomething(); return null; });
    }

    @Test
    void testNestedContextScopes() {
        try (ContextScope outerScope = scopedContext(context1)) {
            outerScope.when(mockService, () -> mockService.greet("Outer")).thenReturn("Outer");
            
            try (ContextScope innerScope = scopedContext(context2)) {
                innerScope.when(mockService, () -> mockService.greet("Inner")).thenReturn("Inner");
                assertEquals("Inner", mockService.greet("Inner"));
            }
            
            // Back to outer context
            assertEquals("Outer", mockService.greet("Outer"));
        }
        
        assertNull(ContextHolder.getCurrentContextIfSet());
    }

    @Test
    void testContextScopeException() {
        ContextScope scope = scopedContext(context1);
        scope.close();
        
        assertThrows(IllegalStateException.class, () -> {
            scope.when(mockService, () -> mockService.greet("Fail")).thenReturn("Should fail");
        });
    }
}