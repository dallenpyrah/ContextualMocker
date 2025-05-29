package com.contextualmocker.junit5;

import com.contextualmocker.core.ContextHolder;
import com.contextualmocker.core.ContextID;
import com.contextualmocker.core.ContextScope;
import com.contextualmocker.core.StringContextId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.contextualmocker.core.ContextualMocker.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ContextualMockerExtension.class)
class ContextualMockerExtensionTest {

    // Test services for mocking and spying
    public interface TestService {
        String getData(String input);
        void processData(String input);
    }

    public static class RealTestService implements TestService {
        @Override
        public String getData(String input) {
            return "real: " + input;
        }

        @Override
        public void processData(String input) {
            // Real implementation
        }
    }

    // Annotated fields for automatic injection
    @Mock
    TestService mockService;

    @Spy
    TestService spyService = new RealTestService();

    @ContextId("fixed-test-context")
    ContextID fixedContext;

    @ContextId(autoSet = true)
    ContextID autoSetContext;

    @ContextId
    ContextID dynamicContext;

    @Test
    void testMockInjection() {
        // Mock should be automatically injected
        assertNotNull(mockService);
        
        try (ContextScope scope = scopedContext(fixedContext)) {
            // Stub the mock
            scope.when(mockService, () -> mockService.getData("test"))
                 .thenReturn("mocked result");

            // Test stubbed behavior
            assertEquals("mocked result", mockService.getData("test"));
            
            // Verify interaction
            scope.verify(mockService, times(1), () -> mockService.getData("test"));
        }
    }

    @Test
    void testSpyInjection() {
        // Spy should be automatically injected
        assertNotNull(spyService);
        
        try (ContextScope scope = scopedContext(fixedContext)) {
            // Test real behavior (no stubbing) 
            assertEquals("real: input", spyService.getData("input"));
            assertEquals("real: normal", spyService.getData("normal"));
            
            // Verify interactions with spy (calls delegate to real implementation)
            scope.verify(spyService, times(1), () -> spyService.getData("input"));
            scope.verify(spyService, times(1), () -> spyService.getData("normal"));
        }
    }

    @Test
    void testContextIdInjection() {
        // All context IDs should be injected
        assertNotNull(fixedContext);
        assertNotNull(autoSetContext);
        assertNotNull(dynamicContext);
        
        // Fixed context should have expected value
        assertEquals("fixed-test-context", ((StringContextId) fixedContext).getId());
        
        // Auto-set context should be the current context
        assertEquals(autoSetContext, ContextHolder.getCurrentContextIfSet());
        
        // Dynamic context should be unique
        assertNotEquals(fixedContext, dynamicContext);
        assertNotEquals(autoSetContext, dynamicContext);
    }

    @Test
    void testParameterInjection(@Mock TestService paramMock, 
                                 @ContextId("param-context") ContextID paramContext) {
        // Parameters should be injected
        assertNotNull(paramMock);
        assertNotNull(paramContext);
        
        assertEquals("param-context", ((StringContextId) paramContext).getId());
        
        try (ContextScope scope = scopedContext(paramContext)) {
            scope.when(paramMock, () -> paramMock.getData("param"))
                 .thenReturn("param result");

            assertEquals("param result", paramMock.getData("param"));
            scope.verify(paramMock, times(1), () -> paramMock.getData("param"));
        }
    }

    @Test
    void testContextIsolationBetweenTests() {
        // Each test should have a clean context state
        // Previous test contexts should not interfere
        
        try (ContextScope scope = scopedContext(fixedContext)) {
            scope.when(mockService, () -> mockService.getData("isolation"))
                 .thenReturn("isolated result");

            assertEquals("isolated result", mockService.getData("isolation"));
            
            // This should not see stubbing from other tests
            assertNull(mockService.getData("other-test-data"));
        }
    }

    @Test
    void testMultipleContextsInSameTest() {
        try (ContextScope scope1 = scopedContext(fixedContext)) {
            scope1.when(mockService, () -> mockService.getData("context1"))
                  .thenReturn("result1");

            assertEquals("result1", mockService.getData("context1"));
        }

        try (ContextScope scope2 = scopedContext(dynamicContext)) {
            scope2.when(mockService, () -> mockService.getData("context2"))
                  .thenReturn("result2");

            assertEquals("result2", mockService.getData("context2"));
            
            // Should not see stubbing from other context
            assertNull(mockService.getData("context1"));
        }
    }

    @Test
    void testVoidMethodVerification() {
        try (ContextScope scope = scopedContext(fixedContext)) {
            mockService.processData("test");
            
            scope.verify(mockService, times(1), () -> { 
                mockService.processData("test"); 
                return null; 
            });
        }
    }
}