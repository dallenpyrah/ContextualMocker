package io.github.dallenpyrah.contextualmocker.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;
import static io.github.dallenpyrah.contextualmocker.core.ContextualMocker.*;

/**
 * Tests for mock validation in given() and verify() methods.
 */
class MockValidationTest {
    
    public interface TestInterface {
        String doSomething();
    }
    
    public static class TestClass {
        public String doSomething() {
            return "real";
        }
    }
    
    private ContextID context;
    
    @BeforeEach
    void setUp() {
        context = new StringContextId("test-context");
        MockRegistry.clearAllData();
    }
    
    @Test
    @DisplayName("given() should accept valid mocks")
    void testGivenAcceptsValidMocks() {
        TestInterface mock = mock(TestInterface.class);
        
        assertDoesNotThrow(() -> {
            given(mock).forContext(context).when(() -> mock.doSomething()).thenReturn("mocked");
        });
    }
    
    @Test
    @DisplayName("given() should reject non-mock objects")
    void testGivenRejectsNonMockObjects() {
        TestClass realObject = new TestClass();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> given(realObject)
        );
        
        assertTrue(exception.getMessage().contains("Object is not a mock created by ContextualMocker"));
        assertTrue(exception.getMessage().contains(TestClass.class.getName()));
    }
    
    @Test
    @DisplayName("given() should reject null")
    void testGivenRejectsNull() {
        assertThrows(
            NullPointerException.class,
            () -> given(null),
            "Mock object cannot be null"
        );
    }
    
    @Test
    @DisplayName("verify() should accept valid mocks")
    void testVerifyAcceptsValidMocks() {
        TestInterface mock = mock(TestInterface.class);
        
        // Set up context and invoke method
        ContextHolder.setContext(context);
        mock.doSomething();
        ContextHolder.clearContext();
        
        assertDoesNotThrow(() -> {
            verify(mock).forContext(context).verify(times(1));
            mock.doSomething();
        });
    }
    
    @Test
    @DisplayName("verify() should reject non-mock objects")
    void testVerifyRejectsNonMockObjects() {
        TestClass realObject = new TestClass();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> verify(realObject)
        );
        
        assertTrue(exception.getMessage().contains("Object is not a mock created by ContextualMocker"));
        assertTrue(exception.getMessage().contains(TestClass.class.getName()));
    }
    
    @Test
    @DisplayName("verify() should reject null")
    void testVerifyRejectsNull() {
        assertThrows(
            NullPointerException.class,
            () -> verify(null),
            "Mock object cannot be null"
        );
    }
    
    @Test
    @DisplayName("given() should work with class mocks")
    void testGivenWorksWithClassMocks() {
        TestClass mock = mock(TestClass.class);
        
        assertDoesNotThrow(() -> {
            given(mock).forContext(context).when(() -> mock.doSomething()).thenReturn("mocked");
        });
        
        ContextHolder.setContext(context);
        assertEquals("mocked", mock.doSomething());
        ContextHolder.clearContext();
    }
    
    @Test
    @DisplayName("verify() should work with class mocks")
    void testVerifyWorksWithClassMocks() {
        TestClass mock = mock(TestClass.class);
        
        ContextHolder.setContext(context);
        mock.doSomething();
        ContextHolder.clearContext();
        
        assertDoesNotThrow(() -> {
            verify(mock).forContext(context).verify(times(1));
            mock.doSomething();
        });
    }
    
    @Test
    @DisplayName("given() should work with spy objects")
    void testGivenWorksWithSpyObjects() {
        TestClass realObject = new TestClass();
        TestClass spy = spy(realObject);
        
        assertDoesNotThrow(() -> {
            given(spy).forContext(context).when(() -> spy.doSomething()).thenReturn("spy-mocked");
        });
        
        ContextHolder.setContext(context);
        assertEquals("spy-mocked", spy.doSomething());
        ContextHolder.clearContext();
    }
    
    @Test
    @DisplayName("verify() should work with spy objects")
    void testVerifyWorksWithSpyObjects() {
        TestClass realObject = new TestClass();
        TestClass spy = spy(realObject);
        
        ContextHolder.setContext(context);
        spy.doSomething();
        ContextHolder.clearContext();
        
        assertDoesNotThrow(() -> {
            verify(spy).forContext(context).verify(times(1));
            spy.doSomething();
        });
    }
    
    @Test
    @DisplayName("Direct API methods should also validate mocks")
    void testDirectApiMethodsValidateMocks() {
        TestClass realObject = new TestClass();
        TestInterface mock = mock(TestInterface.class);
        
        // Test when() method with non-mock
        IllegalArgumentException whenException = assertThrows(
            IllegalArgumentException.class,
            () -> when(realObject, context, () -> realObject.doSomething())
        );
        assertTrue(whenException.getMessage().contains("Object is not a mock created by ContextualMocker"));
        
        // Test verify() method with non-mock
        IllegalArgumentException verifyException = assertThrows(
            IllegalArgumentException.class,
            () -> verify(realObject, context, times(1), () -> realObject.doSomething())
        );
        assertTrue(verifyException.getMessage().contains("Object is not a mock created by ContextualMocker"));
        
        // Test that they work with valid mocks
        assertDoesNotThrow(() -> {
            when(mock, context, () -> mock.doSomething()).thenReturn("direct-mocked");
        });
    }
    
    @Test
    @DisplayName("isMock() should correctly identify mocks, spies, and non-mocks")
    void testIsMockMethod() {
        // Test with interface mock
        TestInterface interfaceMock = mock(TestInterface.class);
        assertTrue(ContextualMocker.isMock(interfaceMock), "Interface mock should be identified as mock");
        
        // Test with class mock
        TestClass classMock = mock(TestClass.class);
        assertTrue(ContextualMocker.isMock(classMock), "Class mock should be identified as mock");
        
        // Test with spy
        TestClass realObject = new TestClass();
        TestClass spy = spy(realObject);
        assertTrue(ContextualMocker.isMock(spy), "Spy should be identified as mock");
        
        // Test with non-mock object
        TestClass normalObject = new TestClass();
        assertFalse(ContextualMocker.isMock(normalObject), "Normal object should not be identified as mock");
        
        // Test with null
        assertFalse(ContextualMocker.isMock(null), "null should not be identified as mock");
        
        // Test with String (common object)
        assertFalse(ContextualMocker.isMock("test"), "String should not be identified as mock");
    }
}