package io.github.dallenpyrah.contextualmocker.captors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BasicArgumentCaptorTest {

    // Test interface and classes for custom type testing
    interface TestService {
        void process(String data);
        void processNumber(Integer number);
        void processCustom(CustomObject obj);
        void processMultiple(String s1, Integer i1, CustomObject obj);
    }

    static class CustomObject {
        private final String name;
        private final int value;

        CustomObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CustomObject that = (CustomObject) o;
            return value == that.value && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + value;
        }
    }

    @Nested
    @DisplayName("ArgumentCaptor Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("Should create ArgumentCaptor for String type")
        void shouldCreateStringCaptor() {
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            
            assertNotNull(captor);
            assertEquals(String.class, captor.getType());
            assertTrue(captor.getAllValues().isEmpty());
        }

        @Test
        @DisplayName("Should create ArgumentCaptor for Integer type")
        void shouldCreateIntegerCaptor() {
            ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
            
            assertNotNull(captor);
            assertEquals(Integer.class, captor.getType());
            assertTrue(captor.getAllValues().isEmpty());
        }

        @Test
        @DisplayName("Should create ArgumentCaptor for custom object type")
        void shouldCreateCustomObjectCaptor() {
            ArgumentCaptor<CustomObject> captor = ArgumentCaptor.forClass(CustomObject.class);
            
            assertNotNull(captor);
            assertEquals(CustomObject.class, captor.getType());
            assertTrue(captor.getAllValues().isEmpty());
        }

        @Test
        @DisplayName("Should create ArgumentCaptor for List type with generics")
        void shouldCreateListCaptor() {
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass((Class<List<String>>) (Class<?>) List.class);
            
            assertNotNull(captor);
            assertEquals(List.class, captor.getType());
            assertTrue(captor.getAllValues().isEmpty());
        }
    }

    @Nested
    @DisplayName("Basic Capture and getValue Tests")
    class BasicCaptureTests {

        private ArgumentCaptor<String> stringCaptor;
        private ArgumentCaptor<Integer> integerCaptor;
        private ArgumentCaptor<CustomObject> customCaptor;

        @BeforeEach
        void setUp() {
            stringCaptor = ArgumentCaptor.forClass(String.class);
            integerCaptor = ArgumentCaptor.forClass(Integer.class);
            customCaptor = ArgumentCaptor.forClass(CustomObject.class);
        }

        @Test
        @DisplayName("Should capture and retrieve single String value")
        void shouldCaptureStringValue() {
            // Simulate framework recording a value
            stringCaptor.recordValue("test value");
            
            assertEquals("test value", stringCaptor.getValue());
            assertEquals(1, stringCaptor.getAllValues().size());
        }

        @Test
        @DisplayName("Should capture and retrieve single Integer value")
        void shouldCaptureIntegerValue() {
            integerCaptor.recordValue(42);
            
            assertEquals(42, integerCaptor.getValue());
            assertEquals(1, integerCaptor.getAllValues().size());
        }

        @Test
        @DisplayName("Should capture and retrieve custom object")
        void shouldCaptureCustomObject() {
            CustomObject obj = new CustomObject("test", 100);
            customCaptor.recordValue(obj);
            
            assertSame(obj, customCaptor.getValue());
            assertEquals("test", customCaptor.getValue().getName());
            assertEquals(100, customCaptor.getValue().getValue());
        }

        @Test
        @DisplayName("Should return null from capture() method")
        void shouldReturnNullFromCapture() {
            // The capture() method returns null as it's replaced by framework during verification
            // Using the basic ArgumentCaptor (not ContextualArgumentCaptor)
            ArgumentCaptor<String> basicStringCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Integer> basicIntCaptor = ArgumentCaptor.forClass(Integer.class);
            ArgumentCaptor<CustomObject> basicCustomCaptor = ArgumentCaptor.forClass(CustomObject.class);
            
            assertNull(basicStringCaptor.capture());
            assertNull(basicIntCaptor.capture());
            assertNull(basicCustomCaptor.capture());
        }
    }

    @Nested
    @DisplayName("Multiple Captures Tests")
    class MultipleCapturesTests {

        private ArgumentCaptor<String> captor;

        @BeforeEach
        void setUp() {
            captor = ArgumentCaptor.forClass(String.class);
        }

        @Test
        @DisplayName("Should capture multiple values and retrieve all")
        void shouldCaptureMultipleValues() {
            captor.recordValue("first");
            captor.recordValue("second");
            captor.recordValue("third");
            
            List<String> allValues = captor.getAllValues();
            assertEquals(3, allValues.size());
            assertEquals("first", allValues.get(0));
            assertEquals("second", allValues.get(1));
            assertEquals("third", allValues.get(2));
        }

        @Test
        @DisplayName("Should return last captured value with getValue()")
        void shouldReturnLastCapturedValue() {
            captor.recordValue("first");
            captor.recordValue("second");
            captor.recordValue("last");
            
            assertEquals("last", captor.getValue());
        }

        @Test
        @DisplayName("Should return unmodifiable list from getAllValues()")
        void shouldReturnUnmodifiableList() {
            captor.recordValue("value1");
            captor.recordValue("value2");
            
            List<String> values = captor.getAllValues();
            assertThrows(UnsupportedOperationException.class, () -> values.add("new"));
            assertThrows(UnsupportedOperationException.class, () -> values.remove(0));
            assertThrows(UnsupportedOperationException.class, values::clear);
        }

        @Test
        @DisplayName("Should clear all values on reset")
        void shouldClearValuesOnReset() {
            captor.recordValue("value1");
            captor.recordValue("value2");
            assertEquals(2, captor.getAllValues().size());
            
            captor.reset();
            
            assertTrue(captor.getAllValues().isEmpty());
            assertThrows(IllegalStateException.class, captor::getValue);
        }
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionTests {

        private ArgumentCaptor<String> captor;

        @BeforeEach
        void setUp() {
            captor = ArgumentCaptor.forClass(String.class);
        }

        @Test
        @DisplayName("Should throw exception when getValue() called without captures")
        void shouldThrowExceptionWhenNoCaptures() {
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                captor::getValue
            );
            assertEquals("No argument value captured", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception after reset")
        void shouldThrowExceptionAfterReset() {
            captor.recordValue("value");
            assertEquals("value", captor.getValue());
            
            captor.reset();
            
            assertThrows(IllegalStateException.class, captor::getValue);
        }
    }

    @Nested
    @DisplayName("Null Value Tests")
    class NullValueTests {

        private ArgumentCaptor<String> captor;

        @BeforeEach
        void setUp() {
            captor = ArgumentCaptor.forClass(String.class);
        }

        @Test
        @DisplayName("Should capture null values")
        void shouldCaptureNullValues() {
            captor.recordValue(null);
            
            assertNull(captor.getValue());
            assertEquals(1, captor.getAllValues().size());
            assertNull(captor.getAllValues().get(0));
        }

        @Test
        @DisplayName("Should handle mix of null and non-null values")
        void shouldHandleMixedNullValues() {
            captor.recordValue("first");
            captor.recordValue(null);
            captor.recordValue("third");
            captor.recordValue(null);
            
            List<String> values = captor.getAllValues();
            assertEquals(4, values.size());
            assertEquals("first", values.get(0));
            assertNull(values.get(1));
            assertEquals("third", values.get(2));
            assertNull(values.get(3));
            assertNull(captor.getValue()); // Last value
        }
    }

    @Nested
    @DisplayName("Type Safety Tests")
    class TypeSafetyTests {

        @Test
        @DisplayName("Should maintain type safety with generics")
        void shouldMaintainTypeSafety() {
            ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Integer> intCaptor = ArgumentCaptor.forClass(Integer.class);
            
            // Record values
            stringCaptor.recordValue("text");
            intCaptor.recordValue(123);
            
            // Retrieve with correct types
            String strValue = stringCaptor.getValue();
            Integer intValue = intCaptor.getValue();
            
            assertEquals("text", strValue);
            assertEquals(123, intValue);
            
            // Check compile-time type safety
            assertEquals(String.class, stringCaptor.getType());
            assertEquals(Integer.class, intCaptor.getType());
        }

        @Test
        @DisplayName("Should work with generic types")
        void shouldWorkWithGenericTypes() {
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String>> listCaptor = 
                ArgumentCaptor.forClass((Class<List<String>>) (Class<?>) List.class);
            
            List<String> list1 = List.of("a", "b", "c");
            List<String> list2 = List.of("x", "y", "z");
            
            listCaptor.recordValue(list1);
            listCaptor.recordValue(list2);
            
            assertEquals(list2, listCaptor.getValue());
            assertEquals(2, listCaptor.getAllValues().size());
            assertTrue(listCaptor.getAllValues().get(0).contains("a"));
            assertTrue(listCaptor.getAllValues().get(1).contains("x"));
        }

        @Test
        @DisplayName("Should work with array types")
        void shouldWorkWithArrayTypes() {
            ArgumentCaptor<String[]> arrayCaptor = ArgumentCaptor.forClass(String[].class);
            
            String[] array1 = {"one", "two", "three"};
            String[] array2 = {"four", "five"};
            
            arrayCaptor.recordValue(array1);
            arrayCaptor.recordValue(array2);
            
            assertArrayEquals(array2, arrayCaptor.getValue());
            assertEquals(2, arrayCaptor.getAllValues().size());
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent captures safely")
        void shouldHandleConcurrentCaptures() throws InterruptedException {
            ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
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
                        startLatch.await(); // Wait for all threads to be ready
                        for (int j = 0; j < capturesPerThread; j++) {
                            captor.recordValue(threadId * 1000 + j);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        completeLatch.countDown();
                    }
                });
            }

            // Start all threads simultaneously
            startLatch.countDown();
            
            // Wait for completion
            assertTrue(completeLatch.await(5, TimeUnit.SECONDS));
            executor.shutdown();
            
            // Verify all values were captured
            List<Integer> allValues = captor.getAllValues();
            assertEquals(threadCount * capturesPerThread, allValues.size());
            
            // Verify no values were lost
            int[] counts = new int[threadCount];
            for (Integer value : allValues) {
                int threadId = value / 1000;
                counts[threadId]++;
            }
            
            for (int count : counts) {
                assertEquals(capturesPerThread, count);
            }
        }

        @Test
        @DisplayName("Should safely read while writing concurrently")
        void shouldSafelyReadWhileWriting() throws InterruptedException {
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            AtomicInteger writeCount = new AtomicInteger(0);
            AtomicInteger readErrors = new AtomicInteger(0);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completeLatch = new CountDownLatch(2);
            
            // Writer thread
            Thread writer = new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < 1000; i++) {
                        captor.recordValue("value-" + i);
                        writeCount.incrementAndGet();
                        Thread.yield(); // Give reader chance to read
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            });
            
            // Reader thread
            Thread reader = new Thread(() -> {
                try {
                    startLatch.await();
                    Thread.sleep(10); // Let writer start first
                    for (int i = 0; i < 100; i++) {
                        try {
                            List<String> values = captor.getAllValues();
                            // Just accessing should not throw
                            if (!values.isEmpty()) {
                                captor.getValue(); // Should not throw if list not empty
                            }
                        } catch (Exception e) {
                            readErrors.incrementAndGet();
                        }
                        Thread.sleep(5);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            });
            
            writer.start();
            reader.start();
            startLatch.countDown();
            
            assertTrue(completeLatch.await(10, TimeUnit.SECONDS));
            
            assertEquals(0, readErrors.get(), "No read errors should occur");
            assertEquals(1000, captor.getAllValues().size());
        }
    }

    @Nested
    @DisplayName("Integration Simulation Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should simulate typical verification workflow")
        void shouldSimulateVerificationWorkflow() {
            // This simulates how the framework would use ArgumentCaptor
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            
            // Simulate method invocation capture
            String arg1 = "first call";
            String arg2 = "second call";
            String arg3 = "third call";
            
            // Framework would record these during verification
            captor.recordValue(arg1);
            captor.recordValue(arg2);
            captor.recordValue(arg3);
            
            // User assertions
            List<String> allCalls = captor.getAllValues();
            assertEquals(3, allCalls.size());
            assertEquals("first call", allCalls.get(0));
            assertEquals("second call", allCalls.get(1));
            assertEquals("third call", allCalls.get(2));
            assertEquals("third call", captor.getValue());
        }

        @Test
        @DisplayName("Should handle complex object capturing")
        void shouldHandleComplexObjectCapturing() {
            ArgumentCaptor<CustomObject> captor = ArgumentCaptor.forClass(CustomObject.class);
            
            // Simulate capturing different objects
            CustomObject obj1 = new CustomObject("obj1", 10);
            CustomObject obj2 = new CustomObject("obj2", 20);
            CustomObject obj3 = null; // null object
            CustomObject obj4 = new CustomObject("obj4", 40);
            
            captor.recordValue(obj1);
            captor.recordValue(obj2);
            captor.recordValue(obj3);
            captor.recordValue(obj4);
            
            List<CustomObject> captured = captor.getAllValues();
            assertEquals(4, captured.size());
            assertEquals("obj1", captured.get(0).getName());
            assertEquals("obj2", captured.get(1).getName());
            assertNull(captured.get(2));
            assertEquals("obj4", captured.get(3).getName());
            assertEquals(obj4, captor.getValue());
        }
    }
}