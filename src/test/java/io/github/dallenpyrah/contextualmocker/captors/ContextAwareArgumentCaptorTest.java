package io.github.dallenpyrah.contextualmocker.captors;

import io.github.dallenpyrah.contextualmocker.core.ContextID;
import io.github.dallenpyrah.contextualmocker.core.ContextScope;
import io.github.dallenpyrah.contextualmocker.core.ContextualMocker;
import io.github.dallenpyrah.contextualmocker.core.StringContextId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.dallenpyrah.contextualmocker.core.ContextualMocker.*;
import static io.github.dallenpyrah.contextualmocker.matchers.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Context-Aware ArgumentCaptor Tests")
class ContextAwareArgumentCaptorTest {

    public static interface TestService {
        void processString(String data);
        void processInteger(Integer number);
        void processMultiple(String str, Integer num);
        String getValue(String key);
        Integer calculate(Integer a, Integer b);
    }

    public static interface SecondService {
        void handleRequest(String request);
        void logMessage(String level, String message);
    }

    private TestService mockService;
    private SecondService secondMockService;
    private ContextID context1;
    private ContextID context2;
    private ContextID context3;

    @BeforeEach
    void setUp() {
        mockService = mock(TestService.class);
        secondMockService = mock(SecondService.class);
        context1 = new StringContextId("test-context-1");
        context2 = new StringContextId("test-context-2");
        context3 = new StringContextId("test-context-3");
    }

    @Nested
    @DisplayName("Basic Context-Aware Capture Tests")
    class BasicContextCaptureTests {

        @Test
        @DisplayName("Should capture values in specific context")
        void shouldCaptureValuesInContext() {
            ContextualArgumentCaptor<String> captor = ContextualArgumentCaptor.forClass(String.class);

            try (ContextScope scope = ContextScope.withContext(context1)) {
                mockService.processString("context1-value");
                
                scope.verify(mockService, times(1), () -> { 
                    mockService.processString(captor.capture()); 
                    return null; 
                });

                assertEquals("context1-value", captor.getValueForContext(context1));
                assertEquals(1, captor.getCaptureCountForContext(context1));
            }
        }

        @Test
        @DisplayName("Should capture multiple values in same context")
        void shouldCaptureMultipleValuesInSameContext() {
            ContextualArgumentCaptor<String> captor = ContextualArgumentCaptor.forClass(String.class);

            try (ContextScope scope = ContextScope.withContext(context1)) {
                mockService.processString("first");
                mockService.processString("second");
                mockService.processString("third");
                
                scope.verify(mockService, times(3), () -> { 
                    mockService.processString(captor.capture()); 
                    return null; 
                });

                List<String> captured = captor.getAllValuesForContext(context1);
                assertEquals(3, captured.size());
                assertEquals("first", captured.get(0));
                assertEquals("second", captured.get(1));
                assertEquals("third", captured.get(2));
                assertEquals("third", captor.getValueForContext(context1)); // Last value
            }
        }

        @Test
        @DisplayName("Should retrieve empty list for context with no captures")
        void shouldReturnEmptyListForContextWithNoCaptures() {
            ContextualArgumentCaptor<String> captor = ContextualArgumentCaptor.forClass(String.class);

            List<String> values = captor.getAllValuesForContext(context1);
            assertTrue(values.isEmpty());
            assertEquals(0, captor.getCaptureCountForContext(context1));
        }

        @Test
        @DisplayName("Should throw exception when getting value from context with no captures")
        void shouldThrowExceptionForContextWithNoCaptures() {
            ContextualArgumentCaptor<String> captor = ContextualArgumentCaptor.forClass(String.class);

            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> captor.getValueForContext(context1)
            );
            assertTrue(exception.getMessage().contains("No argument value captured for context"));
        }
    }

    @Nested
    @DisplayName("Context Isolation Tests")
    class ContextIsolationTests {

        @Test
        @DisplayName("Should isolate captures between different contexts")
        void shouldIsolateCapturesBetweenContexts() {
            ContextualArgumentCaptor<String> captor = ContextualArgumentCaptor.forClass(String.class);

            // Capture in context1
            try (ContextScope scope = ContextScope.withContext(context1)) {
                mockService.processString("context1-data");
                scope.verify(mockService, times(1), () -> { 
                    mockService.processString(captor.capture()); 
                    return null; 
                });
            }

            // Capture in context2
            try (ContextScope scope = ContextScope.withContext(context2)) {
                mockService.processString("context2-data");
                scope.verify(mockService, times(1), () -> { 
                    mockService.processString(captor.capture()); 
                    return null; 
                });
            }

            // Verify isolation
            assertEquals("context1-data", captor.getValueForContext(context1));
            assertEquals("context2-data", captor.getValueForContext(context2));
            assertEquals(1, captor.getCaptureCountForContext(context1));
            assertEquals(1, captor.getCaptureCountForContext(context2));

            // Verify global captures include both
            List<String> allValues = captor.getAllValues();
            assertEquals(2, allValues.size());
            assertTrue(allValues.contains("context1-data"));
            assertTrue(allValues.contains("context2-data"));
        }

        @Test
        @DisplayName("Should maintain separate capture lists per context")
        void shouldMaintainSeparateCaptureListsPerContext() {
            ContextualArgumentCaptor<Integer> captor = ContextualArgumentCaptor.forClass(Integer.class);

            // Context 1 captures
            try (ContextScope scope = ContextScope.withContext(context1)) {
                mockService.processInteger(10);
                mockService.processInteger(20);
                
                scope.verify(mockService, times(2), () -> { 
                    mockService.processInteger(captor.capture()); 
                    return null; 
                });
            }

            // Context 2 captures
            try (ContextScope scope = ContextScope.withContext(context2)) {
                mockService.processInteger(30);
                mockService.processInteger(40);
                mockService.processInteger(50);
                
                scope.verify(mockService, times(3), () -> { 
                    mockService.processInteger(captor.capture()); 
                    return null; 
                });
            }

            // Verify context-specific captures
            List<Integer> context1Values = captor.getAllValuesForContext(context1);
            assertEquals(2, context1Values.size());
            assertEquals(10, context1Values.get(0));
            assertEquals(20, context1Values.get(1));

            List<Integer> context2Values = captor.getAllValuesForContext(context2);
            assertEquals(3, context2Values.size());
            assertEquals(30, context2Values.get(0));
            assertEquals(40, context2Values.get(1));
            assertEquals(50, context2Values.get(2));
        }
    }

    @Nested
    @DisplayName("Multiple Mock Objects Tests")
    class MultipleMocksTests {

        @Test
        @DisplayName("Should capture from multiple mocks in same context")
        void shouldCaptureFromMultipleMocksInSameContext() {
            ContextualArgumentCaptor<String> captor = ContextualArgumentCaptor.forClass(String.class);

            try (ContextScope scope = ContextScope.withContext(context1)) {
                mockService.processString("from-first-mock");
                secondMockService.handleRequest("from-second-mock");
                
                scope.verify(mockService, times(1), () -> { 
                    mockService.processString(captor.capture()); 
                    return null; 
                });
                scope.verify(secondMockService, times(1), () -> { 
                    secondMockService.handleRequest(captor.capture()); 
                    return null; 
                });

                List<String> capturedValues = captor.getAllValuesForContext(context1);
                assertEquals(2, capturedValues.size());
                assertTrue(capturedValues.contains("from-first-mock"));
                assertTrue(capturedValues.contains("from-second-mock"));
            }
        }

        @Test
        @DisplayName("Should capture from multiple mocks across different contexts")
        void shouldCaptureFromMultipleMocksAcrossContexts() {
            ContextualArgumentCaptor<String> captor = ContextualArgumentCaptor.forClass(String.class);

            // Context 1 - first mock
            try (ContextScope scope = ContextScope.withContext(context1)) {
                mockService.processString("mock1-context1");
                scope.verify(mockService, times(1), () -> { 
                    mockService.processString(captor.capture()); 
                    return null; 
                });
            }

            // Context 2 - second mock
            try (ContextScope scope = ContextScope.withContext(context2)) {
                secondMockService.handleRequest("mock2-context2");
                scope.verify(secondMockService, times(1), () -> { 
                    secondMockService.handleRequest(captor.capture()); 
                    return null; 
                });
            }

            assertEquals("mock1-context1", captor.getValueForContext(context1));
            assertEquals("mock2-context2", captor.getValueForContext(context2));
        }
    }

    @Nested
    @DisplayName("Nested Context Tests")
    class NestedContextTests {

        @Test
        @DisplayName("Should handle nested context scopes")
        void shouldHandleNestedContextScopes() {
            ContextualArgumentCaptor<String> captor = ContextualArgumentCaptor.forClass(String.class);

            try (ContextScope outerScope = ContextScope.withContext(context1)) {
                mockService.processString("outer-context");
                
                try (ContextScope innerScope = ContextScope.withContext(context2)) {
                    mockService.processString("inner-context");
                    
                    innerScope.verify(mockService, times(1), () -> { 
                        mockService.processString(captor.capture()); 
                        return null; 
                    });
                }
                
                // Back to outer context
                mockService.processString("outer-context-again");
                
                outerScope.verify(mockService, times(2), () -> { 
                    mockService.processString(captor.capture()); 
                    return null; 
                });
            }

            // Verify captures
            List<String> context1Values = captor.getAllValuesForContext(context1);
            assertEquals(2, context1Values.size());
            assertEquals("outer-context", context1Values.get(0));
            assertEquals("outer-context-again", context1Values.get(1));

            List<String> context2Values = captor.getAllValuesForContext(context2);
            assertEquals(1, context2Values.size());
            assertEquals("inner-context", context2Values.get(0));
        }

        @Test
        @DisplayName("Should restore parent context after nested scope closes")
        void shouldRestoreParentContextAfterNestedScope() {
            ContextualArgumentCaptor<Integer> captor = ContextualArgumentCaptor.forClass(Integer.class);

            try (ContextScope outerScope = ContextScope.withContext(context1)) {
                mockService.processInteger(100);

                try (ContextScope innerScope = ContextScope.withContext(context2)) {
                    mockService.processInteger(200);
                    
                    innerScope.verify(mockService, times(1), () -> { 
                        mockService.processInteger(captor.capture()); 
                        return null; 
                    });
                    
                    assertEquals(200, captor.getValueForContext(context2).intValue());
                }

                // After inner scope closes, we're back in context1
                mockService.processInteger(300);
                
                outerScope.verify(mockService, times(2), () -> { 
                    mockService.processInteger(captor.capture()); 
                    return null; 
                });
            }

            List<Integer> context1Values = captor.getAllValuesForContext(context1);
            assertEquals(2, context1Values.size());
            assertEquals(100, context1Values.get(0).intValue());
            assertEquals(300, context1Values.get(1).intValue());
        }
    }

    @Nested
    @DisplayName("Concurrent Context Capture Tests")
    class ConcurrentCaptureTests {

        @Test
        @DisplayName("Should handle concurrent captures in different contexts")
        void shouldHandleConcurrentCapturesInDifferentContexts() throws InterruptedException {
            ContextualArgumentCaptor<String> captor = ContextualArgumentCaptor.forClass(String.class);
            int threadCount = 5;
            int capturesPerThread = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completeLatch = new CountDownLatch(threadCount * 2);
            AtomicInteger errorCount = new AtomicInteger(0);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount * 2);

            try {
                // Create threads for context1
                for (int t = 0; t < threadCount; t++) {
                    final int threadNum = t;
                    executor.submit(() -> {
                        try {
                            startLatch.await();
                            try (ContextScope scope = ContextScope.withContext(context1)) {
                                for (int i = 0; i < capturesPerThread; i++) {
                                    String value = "context1-thread" + threadNum + "-value" + i;
                                    mockService.processString(value);
                                }
                                
                                scope.verify(mockService, times(capturesPerThread), () -> { 
                                    mockService.processString(captor.capture()); 
                                    return null; 
                                });
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            e.printStackTrace();
                        } finally {
                            completeLatch.countDown();
                        }
                    });
                }

                // Create threads for context2
                for (int t = 0; t < threadCount; t++) {
                    final int threadNum = t;
                    executor.submit(() -> {
                        try {
                            startLatch.await();
                            try (ContextScope scope = ContextScope.withContext(context2)) {
                                for (int i = 0; i < capturesPerThread; i++) {
                                    String value = "context2-thread" + threadNum + "-value" + i;
                                    mockService.processString(value);
                                }
                                
                                scope.verify(mockService, times(capturesPerThread), () -> { 
                                    mockService.processString(captor.capture()); 
                                    return null; 
                                });
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            e.printStackTrace();
                        } finally {
                            completeLatch.countDown();
                        }
                    });
                }

                // Start all threads
                startLatch.countDown();

                // Wait for completion
                assertTrue(completeLatch.await(10, TimeUnit.SECONDS));
                assertEquals(0, errorCount.get());

                // Verify captures
                List<String> context1Values = captor.getAllValuesForContext(context1);
                List<String> context2Values = captor.getAllValuesForContext(context2);

                assertEquals(threadCount * capturesPerThread, context1Values.size());
                assertEquals(threadCount * capturesPerThread, context2Values.size());

                // Verify all context1 values contain "context1"
                assertTrue(context1Values.stream().allMatch(v -> v.startsWith("context1")));
                
                // Verify all context2 values contain "context2"
                assertTrue(context2Values.stream().allMatch(v -> v.startsWith("context2")));

                // Verify total captures
                assertEquals(threadCount * capturesPerThread * 2, captor.getCaptureCount());
            } finally {
                executor.shutdown();
            }
        }

        @Test
        @DisplayName("Should handle rapid context switching")
        void shouldHandleRapidContextSwitching() throws InterruptedException {
            ContextualArgumentCaptor<Integer> captor = ContextualArgumentCaptor.forClass(Integer.class);
            int iterations = 100;
            CountDownLatch latch = new CountDownLatch(3);
            AtomicReference<Exception> error = new AtomicReference<>();
            ExecutorService executor = Executors.newFixedThreadPool(3);

            try {
                // Thread rapidly switching between context1 and context2
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < iterations; i++) {
                            ContextID ctx = (i % 2 == 0) ? context1 : context2;
                            try (ContextScope scope = ContextScope.withContext(ctx)) {
                                mockService.processInteger(i);
                                scope.verify(mockService, atLeastOnce(), () -> { 
                                    mockService.processInteger(captor.capture()); 
                                    return null; 
                                });
                            }
                        }
                    } catch (Exception e) {
                        error.set(e);
                    } finally {
                        latch.countDown();
                    }
                });

                // Thread using only context3
                executor.submit(() -> {
                    try {
                        try (ContextScope scope = ContextScope.withContext(context3)) {
                            for (int i = 1000; i < 1000 + iterations; i++) {
                                mockService.processInteger(i);
                            }
                            scope.verify(mockService, times(iterations), () -> { 
                                mockService.processInteger(captor.capture()); 
                                return null; 
                            });
                        }
                    } catch (Exception e) {
                        error.set(e);
                    } finally {
                        latch.countDown();
                    }
                });

                // Thread without context
                executor.submit(() -> {
                    try {
                        ContextID noContext = new StringContextId("no-context-thread");
                        for (int i = 2000; i < 2000 + iterations; i++) {
                            mockService.processInteger(i);
                        }
                        verify(mockService, noContext, atLeast(iterations), () -> { 
                            mockService.processInteger(captor.capture()); 
                            return null; 
                        });
                    } catch (Exception e) {
                        error.set(e);
                    } finally {
                        latch.countDown();
                    }
                });

                assertTrue(latch.await(10, TimeUnit.SECONDS));
                assertNull(error.get());

                // Verify context3 has exactly the expected values
                List<Integer> context3Values = captor.getAllValuesForContext(context3);
                assertEquals(iterations, context3Values.size());
                assertTrue(context3Values.stream().allMatch(v -> v >= 1000 && v < 1000 + iterations));
            } finally {
                executor.shutdown();
            }
        }
    }

    @Nested
    @DisplayName("Advanced Usage Tests")
    class AdvancedUsageTests {

        @Test
        @DisplayName("Should capture with matchers in context")
        void shouldCaptureWithMatchersInContext() {
            ContextualArgumentCaptor<String> keyCaptor = ContextualArgumentCaptor.forClass(String.class);

            try (ContextScope scope = ContextScope.withContext(context1)) {
                scope.when(mockService, () -> mockService.getValue(keyCaptor.capture()))
                     .thenReturn("captured-value");

                assertEquals("captured-value", mockService.getValue("test-key"));
                assertEquals("test-key", keyCaptor.getValueForContext(context1));
            }
        }

        @Test
        @DisplayName("Should capture multiple arguments in method call")
        void shouldCaptureMultipleArgumentsInMethodCall() {
            ContextualArgumentCaptor<String> stringCaptor = ContextualArgumentCaptor.forClass(String.class);
            ContextualArgumentCaptor<Integer> intCaptor = ContextualArgumentCaptor.forClass(Integer.class);

            try (ContextScope scope = ContextScope.withContext(context1)) {
                mockService.processMultiple("test-string", 42);
                
                scope.verify(mockService, times(1), () -> { 
                    mockService.processMultiple(stringCaptor.capture(), intCaptor.capture()); 
                    return null; 
                });

                assertEquals("test-string", stringCaptor.getValueForContext(context1));
                assertEquals(42, intCaptor.getValueForContext(context1).intValue());
            }
        }

        @Test
        @DisplayName("Should reset captures across contexts")
        void shouldResetCapturesAcrossContexts() {
            ContextualArgumentCaptor<String> captor = ContextualArgumentCaptor.forClass(String.class);

            // Capture in multiple contexts
            try (ContextScope scope = ContextScope.withContext(context1)) {
                mockService.processString("value1");
                scope.verify(mockService, times(1), () -> { 
                    mockService.processString(captor.capture()); 
                    return null; 
                });
            }

            try (ContextScope scope = ContextScope.withContext(context2)) {
                mockService.processString("value2");
                scope.verify(mockService, times(1), () -> { 
                    mockService.processString(captor.capture()); 
                    return null; 
                });
            }

            assertEquals(2, captor.getCaptureCount());
            assertEquals(1, captor.getCaptureCountForContext(context1));
            assertEquals(1, captor.getCaptureCountForContext(context2));

            // Reset
            captor.reset();

            assertEquals(0, captor.getCaptureCount());
            assertEquals(0, captor.getCaptureCountForContext(context1));
            assertEquals(0, captor.getCaptureCountForContext(context2));
            assertTrue(captor.getAllValues().isEmpty());
            assertTrue(captor.getAllValuesForContext(context1).isEmpty());
            assertTrue(captor.getAllValuesForContext(context2).isEmpty());
        }

        @Test
        @DisplayName("Should work with stubbing and verification in same context")
        void shouldWorkWithStubbingAndVerificationInSameContext() {
            ContextualArgumentCaptor<Integer> aCaptor = ContextualArgumentCaptor.forClass(Integer.class);
            ContextualArgumentCaptor<Integer> bCaptor = ContextualArgumentCaptor.forClass(Integer.class);

            try (ContextScope scope = ContextScope.withContext(context1)) {
                // Stub with captors
                scope.when(mockService, () -> mockService.calculate(aCaptor.capture(), bCaptor.capture()))
                     .thenReturn(999);

                // Call the method
                Integer result = mockService.calculate(10, 20);
                assertEquals(999, result);

                // Verify captures
                assertEquals(10, aCaptor.getValueForContext(context1).intValue());
                assertEquals(20, bCaptor.getValueForContext(context1).intValue());

                // Call again with different values
                result = mockService.calculate(30, 40);
                assertEquals(999, result);

                // Verify latest captures
                assertEquals(30, aCaptor.getValueForContext(context1).intValue());
                assertEquals(40, bCaptor.getValueForContext(context1).intValue());

                // Verify all captures
                List<Integer> allA = aCaptor.getAllValuesForContext(context1);
                List<Integer> allB = bCaptor.getAllValuesForContext(context1);
                assertEquals(2, allA.size());
                assertEquals(2, allB.size());
                assertEquals(10, allA.get(0).intValue());
                assertEquals(30, allA.get(1).intValue());
                assertEquals(20, allB.get(0).intValue());
                assertEquals(40, allB.get(1).intValue());
            }
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw NPE for null context ID")
        void shouldThrowNPEForNullContextId() {
            ContextualArgumentCaptor<String> captor = ContextualArgumentCaptor.forClass(String.class);

            assertThrows(NullPointerException.class, 
                () -> captor.getValueForContext(null));
            assertThrows(NullPointerException.class, 
                () -> captor.getAllValuesForContext(null));
            assertThrows(NullPointerException.class, 
                () -> captor.getCaptureCountForContext(null));
        }

        @Test
        @DisplayName("Should provide meaningful toString representation")
        void shouldProvideMeaningfulToString() {
            ContextualArgumentCaptor<String> captor = ContextualArgumentCaptor.forClass(String.class);

            // Initial state
            String toString = captor.toString();
            assertTrue(toString.contains("ContextualArgumentCaptor"));
            assertTrue(toString.contains("String"));
            assertTrue(toString.contains("globalCaptures=0"));
            assertTrue(toString.contains("contexts=0"));

            // After some captures
            try (ContextScope scope1 = ContextScope.withContext(context1)) {
                mockService.processString("test1");
                scope1.verify(mockService, times(1), () -> { 
                    mockService.processString(captor.capture()); 
                    return null; 
                });
            }

            try (ContextScope scope2 = ContextScope.withContext(context2)) {
                mockService.processString("test2");
                scope2.verify(mockService, times(1), () -> { 
                    mockService.processString(captor.capture()); 
                    return null; 
                });
            }

            toString = captor.toString();
            assertTrue(toString.contains("globalCaptures=2"));
            assertTrue(toString.contains("contexts=2"));
        }
    }
}