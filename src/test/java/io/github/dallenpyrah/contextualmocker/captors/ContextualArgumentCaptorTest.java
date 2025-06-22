package io.github.dallenpyrah.contextualmocker.captors;

import io.github.dallenpyrah.contextualmocker.core.ContextHolder;
import io.github.dallenpyrah.contextualmocker.core.ContextID;
import io.github.dallenpyrah.contextualmocker.core.ContextScope;
import io.github.dallenpyrah.contextualmocker.core.StringContextId;
import io.github.dallenpyrah.contextualmocker.core.ContextualMocker;
import io.github.dallenpyrah.contextualmocker.matchers.ArgumentMatchers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ContextualArgumentCaptorTest {

    // Test interface
    interface TestService {
        void process(String data);
        void processNumber(Integer number);
        void processMultiple(String s, Integer i);
    }

    @Nested
    @DisplayName("Basic Functionality Tests")
    class BasicFunctionalityTests {

        @Test
        @DisplayName("Should create captor for different types")
        void shouldCreateCaptorForDifferentTypes() {
            ContextualArgumentCaptor<String> stringCaptor = ContextualArgumentCaptor.forClass(String.class);
            ContextualArgumentCaptor<Integer> intCaptor = ContextualArgumentCaptor.forClass(Integer.class);
            ContextualArgumentCaptor<List> listCaptor = ContextualArgumentCaptor.forClass(List.class);
            
            assertNotNull(stringCaptor);
            assertNotNull(intCaptor);
            assertNotNull(listCaptor);
            
            // Initial state should be empty
            assertEquals(0, stringCaptor.getCaptureCount());
            assertEquals(0, intCaptor.getCaptureCount());
            assertEquals(0, listCaptor.getCaptureCount());
        }

        @Test
        @DisplayName("Should throw NPE when creating captor with null class")
        void shouldThrowNPEForNullClass() {
            NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> ContextualArgumentCaptor.forClass(null)
            );
            assertEquals("Class cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw when capture is called without context")
        void shouldThrowWhenCaptureCalledWithoutContext() {
            ContextualArgumentCaptor<String> captor = ContextualArgumentCaptor.forClass(String.class);
            assertThrows(IllegalStateException.class, () -> captor.capture());
        }
    }

    @Nested
    @DisplayName("Global Capture Tests")
    static class GlobalCaptureTests {

        private ContextualArgumentCaptor<String> captor;
        private TestService mockService;
        private ContextID globalContext;

        @BeforeEach
        void setUp() {
            captor = ContextualArgumentCaptor.forClass(String.class);
            mockService = ContextualMocker.mock(TestService.class);
            globalContext = new StringContextId("global-context");
        }

        @Test
        @DisplayName("Should capture values globally")
        void shouldCaptureValuesGlobally() {
            // Use captor in stubbing
            ContextualMocker.when(mockService, globalContext, () -> {
                mockService.process(ArgumentMatchers.capture(captor));
                return null;
            }).thenReturn(null);
            
            // Call the method multiple times
            try (ContextScope scope = ContextScope.withContext(globalContext)) {
                mockService.process("first");
                mockService.process("second");
                mockService.process("third");
            }
            
            // Verify captured values
            assertEquals("third", captor.getValueForContext(globalContext));
            List<String> allValues = captor.getAllValuesForContext(globalContext);
            assertEquals(3, allValues.size());
            assertEquals("first", allValues.get(0));
            assertEquals("second", allValues.get(1));
            assertEquals("third", allValues.get(2));
        }

        @Test
        @DisplayName("Should throw exception when no value captured")
        void shouldThrowExceptionWhenNoValueCaptured() {
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> captor.getValue()
            );
            assertEquals("No argument value captured", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle null values")
        void shouldHandleNullValues() {
            ContextualMocker.when(mockService, globalContext, () -> {
                mockService.process(ArgumentMatchers.capture(captor));
                return null;
            }).thenReturn(null);
            
            try (ContextScope scope = ContextScope.withContext(globalContext)) {
                mockService.process(null);
                mockService.process("not null");
                mockService.process(null);
            }
            
            assertNull(captor.getValueForContext(globalContext));
            List<String> values = captor.getAllValuesForContext(globalContext);
            assertEquals(3, values.size());
            assertNull(values.get(0));
            assertEquals("not null", values.get(1));
            assertNull(values.get(2));
        }
    }

    @Nested
    @DisplayName("Context-Specific Capture Tests")
    static class ContextSpecificCaptureTests {

        private ContextualArgumentCaptor<String> captor;
        private TestService mockService;
        private ContextID context1;
        private ContextID context2;

        @BeforeEach
        void setUp() {
            captor = ContextualArgumentCaptor.forClass(String.class);
            mockService = ContextualMocker.mock(TestService.class);
            context1 = new StringContextId("context1");
            context2 = new StringContextId("context2");
        }

        @AfterEach
        void tearDown() {
            ContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should capture values per context")
        void shouldCaptureValuesPerContext() {
            // Setup stubbing for both contexts
            ContextualMocker.when(mockService, context1, () -> {
                mockService.process(ArgumentMatchers.capture(captor));
                return null;
            }).thenReturn(null);
            ContextualMocker.when(mockService, context2, () -> {
                mockService.process(ArgumentMatchers.capture(captor));
                return null;
            }).thenReturn(null);
            
            // Capture in context 1
            try (ContextScope scope = ContextScope.withContext(context1)) {
                mockService.process("context1-value1");
                mockService.process("context1-value2");
            }
            
            // Capture in context 2
            try (ContextScope scope = ContextScope.withContext(context2)) {
                mockService.process("context2-value1");
                mockService.process("context2-value2");
                mockService.process("context2-value3");
            }
            
            // Verify total captures across contexts
            assertEquals(2, captor.getAllValuesForContext(context1).size());
            assertEquals(3, captor.getAllValuesForContext(context2).size());
            
            // Verify context-specific captures
            assertEquals("context1-value2", captor.getValueForContext(context1));
            assertEquals("context2-value3", captor.getValueForContext(context2));
            
            List<String> context1Values = captor.getAllValuesForContext(context1);
            assertEquals(2, context1Values.size());
            assertEquals("context1-value1", context1Values.get(0));
            assertEquals("context1-value2", context1Values.get(1));
            
            List<String> context2Values = captor.getAllValuesForContext(context2);
            assertEquals(3, context2Values.size());
            assertEquals("context2-value1", context2Values.get(0));
            assertEquals("context2-value2", context2Values.get(1));
            assertEquals("context2-value3", context2Values.get(2));
        }

        @Test
        @DisplayName("Should return empty list for context with no captures")
        void shouldReturnEmptyListForContextWithNoCaptures() {
            ContextID unusedContext = new StringContextId("unused");
            List<String> values = captor.getAllValuesForContext(unusedContext);
            assertTrue(values.isEmpty());
            assertEquals(0, captor.getCaptureCountForContext(unusedContext));
        }

        @Test
        @DisplayName("Should throw exception when no value captured for context")
        void shouldThrowExceptionWhenNoValueCapturedForContext() {
            ContextID unusedContext = new StringContextId("unused");
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> captor.getValueForContext(unusedContext)
            );
            assertEquals("No argument value captured for context: unused", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw NPE for null context")
        void shouldThrowNPEForNullContext() {
            assertThrows(NullPointerException.class, () -> captor.getValueForContext(null));
            assertThrows(NullPointerException.class, () -> captor.getAllValuesForContext(null));
            assertThrows(NullPointerException.class, () -> captor.getCaptureCountForContext(null));
        }
    }

    @Nested
    @DisplayName("Reset and State Management Tests")
    static class ResetTests {

        private ContextualArgumentCaptor<String> captor;
        private TestService mockService;
        private ContextID context;

        @BeforeEach
        void setUp() {
            captor = ContextualArgumentCaptor.forClass(String.class);
            mockService = ContextualMocker.mock(TestService.class);
            context = new StringContextId("test-context");
        }

        @AfterEach
        void tearDown() {
            ContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should reset all captured values")
        void shouldResetAllCapturedValues() {
            // Capture some values
            ContextualMocker.when(mockService, context, () -> {
                mockService.process(ArgumentMatchers.capture(captor));
                return null;
            }).thenReturn(null);
            
            try (ContextScope scope = ContextScope.withContext(context)) {
                mockService.process("global1");
            }
            
            try (ContextScope scope = ContextScope.withContext(context)) {
                mockService.process("context1");
            }
            
            // Verify values captured
            assertEquals(2, captor.getCaptureCountForContext(context));
            
            // Reset
            captor.reset();
            
            // Verify all cleared
            assertEquals(0, captor.getCaptureCountForContext(context));
            assertTrue(captor.getAllValuesForContext(context).isEmpty());
            
            // Should throw after reset
            assertThrows(IllegalStateException.class, () -> captor.getValue());
            assertThrows(IllegalStateException.class, () -> captor.getValueForContext(context));
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    static class ThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent captures from multiple threads")
        void shouldHandleConcurrentCaptures() throws InterruptedException {
            ContextualArgumentCaptor<Integer> captor = ContextualArgumentCaptor.forClass(Integer.class);
            TestService mockService = ContextualMocker.mock(TestService.class);
            ContextID testContext = new StringContextId("thread-safety-test");
            ContextualMocker.when(mockService, testContext, () -> {
                mockService.processNumber(ArgumentMatchers.capture(captor));
                return null;
            }).thenReturn(null);
            
            int threadCount = 10;
            int capturesPerThread = 100;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completeLatch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            
            // Launch threads
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        try (ContextScope scope = ContextScope.withContext(testContext)) {
                            for (int j = 0; j < capturesPerThread; j++) {
                                mockService.processNumber(threadId * 1000 + j);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        completeLatch.countDown();
                    }
                });
            }
            
            // Start all threads
            startLatch.countDown();
            
            // Wait for completion
            assertTrue(completeLatch.await(10, TimeUnit.SECONDS));
            executor.shutdown();
            
            // Verify all values captured
            List<Integer> allValues = captor.getAllValuesForContext(testContext);
            assertEquals(threadCount * capturesPerThread, allValues.size());
        }
    }

    @Nested
    @DisplayName("Integration with ArgumentMatchers Tests")
    static class IntegrationTests {

        private TestService mockService;

        @BeforeEach
        void setUp() {
            mockService = ContextualMocker.mock(TestService.class);
        }

        @Test
        @DisplayName("Should work with ArgumentMatchers.capture()")
        void shouldWorkWithArgumentMatchersCapture() {
            ContextualArgumentCaptor<String> captor = ContextualArgumentCaptor.forClass(String.class);
            ContextID context = new StringContextId("integration-test");
            
            // Use in stubbing
            ContextualMocker.when(mockService, context, () -> {
                mockService.process(ArgumentMatchers.capture(captor));
                return null;
            }).thenReturn(null);
            
            // Call method
            try (ContextScope scope = ContextScope.withContext(context)) {
                mockService.process("test-value");
            }
            
            // Verify capture
            assertEquals("test-value", captor.getValueForContext(context));
        }

        @Test
        @DisplayName("Should capture multiple arguments in same method")
        void shouldCaptureMultipleArguments() {
            ContextualArgumentCaptor<String> stringCaptor = ContextualArgumentCaptor.forClass(String.class);
            ContextualArgumentCaptor<Integer> intCaptor = ContextualArgumentCaptor.forClass(Integer.class);
            ContextID context = new StringContextId("multi-arg-test");
            
            // Use in stubbing
            ContextualMocker.when(mockService, context, () -> {
                mockService.processMultiple(
                    ArgumentMatchers.capture(stringCaptor),
                    ArgumentMatchers.capture(intCaptor)
                );
                return null;
            }).thenReturn(null);
            
            // Call method
            try (ContextScope scope = ContextScope.withContext(context)) {
                mockService.processMultiple("text", 42);
            }
            
            // Verify captures
            assertEquals("text", stringCaptor.getValueForContext(context));
            assertEquals(42, intCaptor.getValueForContext(context));
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    static class ToStringTests {

        @Test
        @DisplayName("Should provide meaningful toString")
        void shouldProvideMeaningfulToString() {
            ContextualArgumentCaptor<String> captor = ContextualArgumentCaptor.forClass(String.class);
            TestService mockService = ContextualMocker.mock(TestService.class);
            ContextID context = new StringContextId("test");
            
            // Initial state
            assertEquals("ContextualArgumentCaptor<String> [globalCaptures=0, contexts=0]", 
                        captor.toString());
            
            // After context capture
            ContextualMocker.when(mockService, context, () -> {
                mockService.process(ArgumentMatchers.capture(captor));
                return null;
            }).thenReturn(null);
            try (ContextScope scope = ContextScope.withContext(context)) {
                mockService.process("value1");
            }
            assertEquals("ContextualArgumentCaptor<String> [globalCaptures=0, contexts=1]", 
                        captor.toString());
            
            // After another capture in same context
            try (ContextScope scope = ContextScope.withContext(context)) {
                mockService.process("value2");
            }
            assertEquals("ContextualArgumentCaptor<String> [globalCaptures=0, contexts=1]", 
                        captor.toString());
        }
    }
}