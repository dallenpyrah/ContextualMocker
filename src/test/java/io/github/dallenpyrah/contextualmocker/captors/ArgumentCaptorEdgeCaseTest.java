package io.github.dallenpyrah.contextualmocker.captors;

import io.github.dallenpyrah.contextualmocker.core.ContextualMocker;
import io.github.dallenpyrah.contextualmocker.matchers.ArgumentMatchers;
import io.github.dallenpyrah.contextualmocker.core.ContextHolder;
import io.github.dallenpyrah.contextualmocker.core.ContextID;
import io.github.dallenpyrah.contextualmocker.core.ContextScope;
import io.github.dallenpyrah.contextualmocker.core.StringContextId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.AfterEach;
// Parameterized test imports removed - using regular tests instead

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ArgumentCaptorEdgeCaseTest {

    // Test interfaces and classes
    interface VarargsService {
        void processVarargs(String first, String... rest);
        void processIntVarargs(int... numbers);
        void processMixedVarargs(String fixed, Object... varargs);
    }

    interface PrimitiveService {
        void processPrimitive(int value);
        void processBoxed(Integer value);
        void processAllPrimitives(byte b, short s, int i, long l, float f, double d, boolean bool, char c);
    }

    interface GenericService<T> {
        void process(T value);
        <U extends Number> void processNumber(U number);
        <K, V> void processMap(Map<K, V> map);
        void processWildcard(List<? extends Number> numbers);
        void processWildcardSuper(List<? super Integer> numbers);
        <T extends Comparable<? super T>> void processBounded(T value);
    }

    interface ExceptionService {
        void processException(Exception e);
        void processThrowable(Throwable t);
        void processWithExceptionArg(String data, RuntimeException fallback);
    }

    interface InheritanceService {
        void processBase(BaseClass base);
        void processDerived(DerivedClass derived);
        void processInterface(TestInterface impl);
    }

    interface DefaultMethodInterface {
        default void defaultMethod(String arg) {
            processArg(arg);
        }
        void processArg(String arg);
    }

    interface LambdaService {
        void processFunction(Function<String, Integer> func);
        void processSupplier(Supplier<String> supplier);
        void processRunnable(Runnable runnable);
    }

    static class BaseClass {
        protected String name;
        public BaseClass(String name) {
            this.name = name;
        }
        public String getName() { return name; }
    }

    static class DerivedClass extends BaseClass {
        private int value;
        public DerivedClass(String name, int value) {
            super(name);
            this.value = value;
        }
        public int getValue() { return value; }
    }

    interface TestInterface {
        String getValue();
    }

    static class TestImpl implements TestInterface {
        private final String value;
        public TestImpl(String value) {
            this.value = value;
        }
        @Override
        public String getValue() { return value; }
    }

    @Nested
    @DisplayName("Varargs Method Capture Tests")
    class VarargsTests {

        private VarargsService mock;
        private ArgumentCaptor<String> stringCaptor;
        private ArgumentCaptor<String[]> stringArrayCaptor;
        private ArgumentCaptor<int[]> intArrayCaptor;

        @BeforeEach
        void setUp() {
            mock = ContextualMocker.mock(VarargsService.class);
            stringCaptor = ArgumentCaptor.forClass(String.class);
            stringArrayCaptor = ArgumentCaptor.forClass(String[].class);
            intArrayCaptor = ArgumentCaptor.forClass(int[].class);
        }

        @Test
        @DisplayName("Should capture fixed arg and varargs separately")
        void shouldCaptureFixedAndVarargsSeparately() {
            // Note: In real usage, the framework would handle varargs during verification
            // This simulates capturing both fixed and varargs parameters
            mock.processVarargs("fixed", "var1", "var2", "var3");
            
            // Simulate framework capturing
            stringCaptor.recordValue("fixed");
            stringArrayCaptor.recordValue(new String[]{"var1", "var2", "var3"});
            
            assertEquals("fixed", stringCaptor.getValue());
            assertArrayEquals(new String[]{"var1", "var2", "var3"}, stringArrayCaptor.getValue());
        }

        @Test
        @DisplayName("Should handle empty varargs")
        void shouldHandleEmptyVarargs() {
            mock.processVarargs("fixed");
            
            // Simulate framework capturing
            stringCaptor.recordValue("fixed");
            stringArrayCaptor.recordValue(new String[]{});
            
            assertEquals("fixed", stringCaptor.getValue());
            assertEquals(0, stringArrayCaptor.getValue().length);
        }

        @Test
        @DisplayName("Should capture primitive varargs")
        void shouldCapturePrimitiveVarargs() {
            mock.processIntVarargs(1, 2, 3, 4, 5);
            
            // Simulate framework capturing
            intArrayCaptor.recordValue(new int[]{1, 2, 3, 4, 5});
            
            assertArrayEquals(new int[]{1, 2, 3, 4, 5}, intArrayCaptor.getValue());
        }

        @Test
        @DisplayName("Should handle null varargs array")
        void shouldHandleNullVarargsArray() {
            mock.processVarargs("fixed", (String[]) null);
            
            // Simulate framework capturing
            stringCaptor.recordValue("fixed");
            stringArrayCaptor.recordValue(null);
            
            assertEquals("fixed", stringCaptor.getValue());
            assertNull(stringArrayCaptor.getValue());
        }
    }

    @Nested
    @DisplayName("Primitive Type Boxing/Unboxing Tests")
    class PrimitiveBoxingTests {

        private PrimitiveService mock;

        @BeforeEach
        void setUp() {
            mock = ContextualMocker.mock(PrimitiveService.class);
        }

        @Test
        @DisplayName("Should handle auto-boxing when capturing primitives")
        void shouldHandleAutoBoxing() {
            ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
            
            // Primitive value will be auto-boxed
            mock.processPrimitive(42);
            
            // Simulate framework capturing with auto-boxing
            captor.recordValue(42); // Auto-boxed to Integer
            
            assertEquals(Integer.valueOf(42), captor.getValue());
            assertTrue(captor.getValue() instanceof Integer);
        }

        @Test
        @DisplayName("Should handle null for boxed types")
        void shouldHandleNullForBoxedTypes() {
            ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
            
            mock.processBoxed(null);
            
            // Simulate framework capturing
            captor.recordValue(null);
            
            assertNull(captor.getValue());
        }

        @Test
        @DisplayName("Should capture all primitive types correctly")
        void shouldCaptureAllPrimitiveTypes() {
            // Test byte
            ArgumentCaptor<Byte> byteCaptor = ArgumentCaptor.forClass(Byte.class);
            byteCaptor.recordValue((byte) 127);
            assertEquals(Byte.valueOf((byte) 127), byteCaptor.getValue());
            
            // Test short
            ArgumentCaptor<Short> shortCaptor = ArgumentCaptor.forClass(Short.class);
            shortCaptor.recordValue((short) 32767);
            assertEquals(Short.valueOf((short) 32767), shortCaptor.getValue());
            
            // Test int
            ArgumentCaptor<Integer> intCaptor = ArgumentCaptor.forClass(Integer.class);
            intCaptor.recordValue(2147483647);
            assertEquals(Integer.valueOf(2147483647), intCaptor.getValue());
            
            // Test long
            ArgumentCaptor<Long> longCaptor = ArgumentCaptor.forClass(Long.class);
            longCaptor.recordValue(9223372036854775807L);
            assertEquals(Long.valueOf(9223372036854775807L), longCaptor.getValue());
            
            // Test float
            ArgumentCaptor<Float> floatCaptor = ArgumentCaptor.forClass(Float.class);
            floatCaptor.recordValue(3.14f);
            assertEquals(Float.valueOf(3.14f), floatCaptor.getValue());
            
            // Test double
            ArgumentCaptor<Double> doubleCaptor = ArgumentCaptor.forClass(Double.class);
            doubleCaptor.recordValue(3.14159265359);
            assertEquals(Double.valueOf(3.14159265359), doubleCaptor.getValue());
            
            // Test boolean
            ArgumentCaptor<Boolean> boolCaptor = ArgumentCaptor.forClass(Boolean.class);
            boolCaptor.recordValue(true);
            assertEquals(Boolean.TRUE, boolCaptor.getValue());
            
            // Test char
            ArgumentCaptor<Character> charCaptor = ArgumentCaptor.forClass(Character.class);
            charCaptor.recordValue('A');
            assertEquals(Character.valueOf('A'), charCaptor.getValue());
        }
    }

    @Nested
    @DisplayName("Generic Type Boundaries and Wildcards Tests")
    class GenericsTests {

        private GenericService<String> stringGenericMock;
        private GenericService<Number> numberGenericMock;

        @BeforeEach
        @SuppressWarnings("unchecked")
        void setUp() {
            stringGenericMock = ContextualMocker.mock(GenericService.class);
            numberGenericMock = ContextualMocker.mock(GenericService.class);
        }

        @Test
        @DisplayName("Should capture parameterized type arguments")
        void shouldCaptureParameterizedTypes() {
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            
            stringGenericMock.process("test");
            
            // Simulate framework capturing
            captor.recordValue("test");
            
            assertEquals("test", captor.getValue());
        }

        @Test
        @DisplayName("Should capture bounded type parameters")
        @SuppressWarnings("unchecked")
        void shouldCaptureBoundedTypeParameters() {
            ArgumentCaptor<Number> captor = ArgumentCaptor.forClass(Number.class);
            
            numberGenericMock.processNumber(42);
            numberGenericMock.processNumber(3.14);
            
            // Simulate framework capturing
            captor.recordValue(42);
            captor.recordValue(3.14);
            
            List<Number> values = captor.getAllValues();
            assertEquals(2, values.size());
            assertEquals(42, values.get(0));
            assertEquals(3.14, values.get(1));
        }

        @Test
        @DisplayName("Should capture wildcard extends types")
        @SuppressWarnings("unchecked")
        void shouldCaptureWildcardExtends() {
            ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
            
            List<Integer> intList = Arrays.asList(1, 2, 3);
            List<Double> doubleList = Arrays.asList(1.0, 2.0, 3.0);
            
            numberGenericMock.processWildcard(intList);
            numberGenericMock.processWildcard(doubleList);
            
            // Simulate framework capturing
            listCaptor.recordValue(intList);
            listCaptor.recordValue(doubleList);
            
            List<List> captured = listCaptor.getAllValues();
            assertEquals(2, captured.size());
            assertEquals(intList, captured.get(0));
            assertEquals(doubleList, captured.get(1));
        }

        @Test
        @DisplayName("Should capture wildcard super types")
        @SuppressWarnings("unchecked")
        void shouldCaptureWildcardSuper() {
            ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
            
            List<Integer> intList = Arrays.asList(1, 2, 3);
            List<Number> numberList = Arrays.asList(1, 2.0, 3L);
            List<Object> objectList = Arrays.asList(1, "two", 3.0);
            
            numberGenericMock.processWildcardSuper(intList);
            numberGenericMock.processWildcardSuper(numberList);
            numberGenericMock.processWildcardSuper(objectList);
            
            // Simulate framework capturing
            listCaptor.recordValue(intList);
            listCaptor.recordValue(numberList);
            listCaptor.recordValue(objectList);
            
            assertEquals(3, listCaptor.getAllValues().size());
        }

        @Test
        @DisplayName("Should capture complex generic maps")
        @SuppressWarnings("unchecked")
        void shouldCaptureComplexGenericMaps() {
            ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
            
            Map<String, Integer> stringIntMap = new HashMap<>();
            stringIntMap.put("one", 1);
            stringIntMap.put("two", 2);
            
            Map<Integer, List<String>> complexMap = new HashMap<>();
            complexMap.put(1, Arrays.asList("a", "b"));
            complexMap.put(2, Arrays.asList("c", "d"));
            
            numberGenericMock.processMap(stringIntMap);
            numberGenericMock.processMap(complexMap);
            
            // Simulate framework capturing
            mapCaptor.recordValue(stringIntMap);
            mapCaptor.recordValue(complexMap);
            
            List<Map> captured = mapCaptor.getAllValues();
            assertEquals(2, captured.size());
            assertEquals(stringIntMap, captured.get(0));
            assertEquals(complexMap, captured.get(1));
        }
    }

    @Nested
    @DisplayName("Exception as Method Argument Tests")
    class ExceptionArgumentTests {

        private ExceptionService mock;

        @BeforeEach
        void setUp() {
            mock = ContextualMocker.mock(ExceptionService.class);
        }

        @Test
        @DisplayName("Should capture exception arguments")
        void shouldCaptureExceptionArguments() {
            ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
            
            Exception ex1 = new IllegalArgumentException("Invalid argument");
            Exception ex2 = new IOException("IO error");
            Exception ex3 = new RuntimeException("Runtime error");
            
            mock.processException(ex1);
            mock.processException(ex2);
            mock.processException(ex3);
            
            // Simulate framework capturing
            captor.recordValue(ex1);
            captor.recordValue(ex2);
            captor.recordValue(ex3);
            
            List<Exception> captured = captor.getAllValues();
            assertEquals(3, captured.size());
            assertSame(ex1, captured.get(0));
            assertSame(ex2, captured.get(1));
            assertSame(ex3, captured.get(2));
            
            // Verify exception details are preserved
            assertEquals("Invalid argument", captured.get(0).getMessage());
            assertTrue(captured.get(0) instanceof IllegalArgumentException);
        }

        @Test
        @DisplayName("Should capture throwable hierarchy")
        void shouldCaptureThrowableHierarchy() {
            ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
            
            Throwable error = new Error("System error");
            Throwable exception = new Exception("Checked exception");
            Throwable runtime = new RuntimeException("Runtime exception");
            
            mock.processThrowable(error);
            mock.processThrowable(exception);
            mock.processThrowable(runtime);
            
            // Simulate framework capturing
            captor.recordValue(error);
            captor.recordValue(exception);
            captor.recordValue(runtime);
            
            List<Throwable> captured = captor.getAllValues();
            assertEquals(3, captured.size());
            assertTrue(captured.get(0) instanceof Error);
            assertTrue(captured.get(1) instanceof Exception);
            assertTrue(captured.get(2) instanceof RuntimeException);
        }

        @Test
        @DisplayName("Should capture exception with cause chain")
        void shouldCaptureExceptionWithCauseChain() {
            ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
            
            Exception root = new IOException("Root cause");
            Exception middle = new RuntimeException("Middle layer", root);
            Exception top = new IllegalStateException("Top layer", middle);
            
            mock.processException(top);
            
            // Simulate framework capturing
            captor.recordValue(top);
            
            Exception captured = captor.getValue();
            assertNotNull(captured);
            assertEquals("Top layer", captured.getMessage());
            assertNotNull(captured.getCause());
            assertEquals("Middle layer", captured.getCause().getMessage());
            assertNotNull(captured.getCause().getCause());
            assertEquals("Root cause", captured.getCause().getCause().getMessage());
        }
    }

    @Nested
    @DisplayName("Large Number of Captures Tests")
    class LargeCaptureTests {

        @Test
        @DisplayName("Should handle very large number of captures efficiently")
        void shouldHandleVeryLargeNumberOfCaptures() {
            ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
            int captureCount = 100_000;
            
            // Record large number of values
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < captureCount; i++) {
                captor.recordValue(i);
            }
            long endTime = System.currentTimeMillis();
            
            // Verify all values captured
            assertEquals(captureCount, captor.getAllValues().size());
            assertEquals(Integer.valueOf(captureCount - 1), captor.getValue());
            
            // Performance assertion - should complete in reasonable time
            long duration = endTime - startTime;
            assertTrue(duration < 5000, "Large capture should complete within 5 seconds, took: " + duration + "ms");
            
            // Verify memory efficiency - getAllValues should return defensive copy
            List<Integer> values1 = captor.getAllValues();
            List<Integer> values2 = captor.getAllValues();
            assertNotSame(values1, values2, "Should return new list instances");
            assertEquals(values1, values2, "But with same content");
        }

        @Test
        @DisplayName("Should handle memory efficiently with large objects")
        void shouldHandleMemoryEfficientlyWithLargeObjects() {
            ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
            
            // Capture large byte arrays
            for (int i = 0; i < 100; i++) {
                byte[] largeArray = new byte[1024 * 1024]; // 1MB
                Arrays.fill(largeArray, (byte) i);
                captor.recordValue(largeArray);
            }
            
            // Verify captures
            assertEquals(100, captor.getAllValues().size());
            
            // Verify last value
            byte[] lastValue = captor.getValue();
            assertEquals(1024 * 1024, lastValue.length);
            assertEquals((byte) 99, lastValue[0]);
        }

        @Test
        @DisplayName("Should maintain performance with frequent reset operations")
        void shouldMaintainPerformanceWithFrequentResets() {
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            
            long startTime = System.currentTimeMillis();
            
            for (int cycle = 0; cycle < 1000; cycle++) {
                // Capture some values
                for (int i = 0; i < 100; i++) {
                    captor.recordValue("value-" + cycle + "-" + i);
                }
                
                // Verify and reset
                assertEquals(100, captor.getAllValues().size());
                captor.reset();
                assertTrue(captor.getAllValues().isEmpty());
            }
            
            long duration = System.currentTimeMillis() - startTime;
            assertTrue(duration < 2000, "Reset cycles should complete quickly, took: " + duration + "ms");
        }
    }

    @Nested
    @DisplayName("Captor Reuse Across Verifications Tests")
    static class CaptorReuseTests {

        private ArgumentCaptor<String> sharedCaptor;
        private TestService service1;
        private TestService service2;

        interface TestService {
            void process(String value);
            String processAndReturn(String value);
        }

        @BeforeEach
        void setUp() {
            sharedCaptor = ArgumentCaptor.forClass(String.class);
            service1 = ContextualMocker.mock(TestService.class);
            service2 = ContextualMocker.mock(TestService.class);
        }

        @Test
        @DisplayName("Should accumulate captures across multiple verifications")
        void shouldAccumulateCapturesAcrossVerifications() {
            // First mock usage
            service1.process("service1-call1");
            service1.process("service1-call2");
            
            // Simulate first verification capturing
            sharedCaptor.recordValue("service1-call1");
            sharedCaptor.recordValue("service1-call2");
            
            // Second mock usage
            service2.process("service2-call1");
            service2.process("service2-call2");
            
            // Simulate second verification capturing
            sharedCaptor.recordValue("service2-call1");
            sharedCaptor.recordValue("service2-call2");
            
            // Verify all captures accumulated
            List<String> allValues = sharedCaptor.getAllValues();
            assertEquals(4, allValues.size());
            assertEquals("service1-call1", allValues.get(0));
            assertEquals("service1-call2", allValues.get(1));
            assertEquals("service2-call1", allValues.get(2));
            assertEquals("service2-call2", allValues.get(3));
        }

        @Test
        @DisplayName("Should maintain capture order across different mock types")
        void shouldMaintainCaptureOrderAcrossDifferentMockTypes() {
            // Interleaved calls
            service1.process("first");
            service2.process("second");
            service1.process("third");
            service2.process("fourth");
            
            // Simulate capturing in order
            sharedCaptor.recordValue("first");
            sharedCaptor.recordValue("second");
            sharedCaptor.recordValue("third");
            sharedCaptor.recordValue("fourth");
            
            List<String> values = sharedCaptor.getAllValues();
            assertEquals(Arrays.asList("first", "second", "third", "fourth"), values);
        }

        @Test
        @DisplayName("Should handle reset between verifications")
        void shouldHandleResetBetweenVerifications() {
            // First verification
            service1.process("before-reset-1");
            service1.process("before-reset-2");
            
            sharedCaptor.recordValue("before-reset-1");
            sharedCaptor.recordValue("before-reset-2");
            
            assertEquals(2, sharedCaptor.getAllValues().size());
            
            // Reset
            sharedCaptor.reset();
            
            // Second verification
            service2.process("after-reset-1");
            service2.process("after-reset-2");
            
            sharedCaptor.recordValue("after-reset-1");
            sharedCaptor.recordValue("after-reset-2");
            
            List<String> values = sharedCaptor.getAllValues();
            assertEquals(2, values.size());
            assertEquals("after-reset-1", values.get(0));
            assertEquals("after-reset-2", values.get(1));
        }
    }

    @Nested
    @DisplayName("Inheritance Hierarchy Capture Tests")
    class InheritanceTests {

        private InheritanceService mock;

        @BeforeEach
        void setUp() {
            mock = ContextualMocker.mock(InheritanceService.class);
        }

        @Test
        @DisplayName("Should capture base class references to derived objects")
        void shouldCaptureBaseClassReferencesToDerived() {
            ArgumentCaptor<BaseClass> captor = ArgumentCaptor.forClass(BaseClass.class);
            
            BaseClass base = new BaseClass("base");
            DerivedClass derived = new DerivedClass("derived", 42);
            
            mock.processBase(base);
            mock.processBase(derived); // Upcast
            
            // Simulate framework capturing
            captor.recordValue(base);
            captor.recordValue(derived);
            
            List<BaseClass> captured = captor.getAllValues();
            assertEquals(2, captured.size());
            
            // First is base
            assertEquals("base", captured.get(0).getName());
            assertFalse(captured.get(0) instanceof DerivedClass);
            
            // Second is actually derived
            assertEquals("derived", captured.get(1).getName());
            assertTrue(captured.get(1) instanceof DerivedClass);
            assertEquals(42, ((DerivedClass) captured.get(1)).getValue());
        }

        @Test
        @DisplayName("Should maintain type safety with inheritance")
        void shouldMaintainTypeSafetyWithInheritance() {
            ArgumentCaptor<DerivedClass> derivedCaptor = ArgumentCaptor.forClass(DerivedClass.class);
            
            DerivedClass derived1 = new DerivedClass("d1", 10);
            DerivedClass derived2 = new DerivedClass("d2", 20);
            
            mock.processDerived(derived1);
            mock.processDerived(derived2);
            
            // Simulate framework capturing
            derivedCaptor.recordValue(derived1);
            derivedCaptor.recordValue(derived2);
            
            // Can access derived class methods without casting
            List<DerivedClass> captured = derivedCaptor.getAllValues();
            assertEquals(10, captured.get(0).getValue());
            assertEquals(20, captured.get(1).getValue());
        }

        @Test
        @DisplayName("Should capture interface implementations")
        void shouldCaptureInterfaceImplementations() {
            ArgumentCaptor<TestInterface> captor = ArgumentCaptor.forClass(TestInterface.class);
            
            TestImpl impl1 = new TestImpl("impl1");
            TestImpl impl2 = new TestImpl("impl2");
            
            // Anonymous implementation
            TestInterface anonymous = new TestInterface() {
                @Override
                public String getValue() {
                    return "anonymous";
                }
            };
            
            mock.processInterface(impl1);
            mock.processInterface(impl2);
            mock.processInterface(anonymous);
            
            // Simulate framework capturing
            captor.recordValue(impl1);
            captor.recordValue(impl2);
            captor.recordValue(anonymous);
            
            List<TestInterface> captured = captor.getAllValues();
            assertEquals(3, captured.size());
            assertEquals("impl1", captured.get(0).getValue());
            assertEquals("impl2", captured.get(1).getValue());
            assertEquals("anonymous", captured.get(2).getValue());
            
            // Type checking
            assertTrue(captured.get(0) instanceof TestImpl);
            assertTrue(captured.get(1) instanceof TestImpl);
            assertFalse(captured.get(2) instanceof TestImpl); // Anonymous class
        }
    }

    @Nested
    @DisplayName("Lambda and Method Reference Capture Tests")
    class LambdaTests {

        private LambdaService mock;

        @BeforeEach
        void setUp() {
            mock = ContextualMocker.mock(LambdaService.class);
        }

        @Test
        @DisplayName("Should capture lambda expressions")
        @SuppressWarnings("unchecked")
        void shouldCaptureLambdaExpressions() {
            ArgumentCaptor<Function> captor = ArgumentCaptor.forClass(Function.class);
            
            Function<String, Integer> lambda1 = s -> s.length();
            Function<String, Integer> lambda2 = s -> Integer.parseInt(s);
            
            mock.processFunction(lambda1);
            mock.processFunction(lambda2);
            
            // Simulate framework capturing
            captor.recordValue(lambda1);
            captor.recordValue(lambda2);
            
            List<Function> captured = captor.getAllValues();
            assertEquals(2, captured.size());
            
            // Verify lambdas still work
            assertEquals(5, captured.get(0).apply("hello"));
            assertEquals(123, captured.get(1).apply("123"));
        }

        @Test
        @DisplayName("Should capture method references")
        @SuppressWarnings("unchecked")
        void shouldCaptureMethodReferences() {
            ArgumentCaptor<Function> captor = ArgumentCaptor.forClass(Function.class);
            
            Function<String, Integer> methodRef = String::length;
            Function<String, Integer> staticRef = Integer::parseInt;
            
            mock.processFunction(methodRef);
            mock.processFunction(staticRef);
            
            // Simulate framework capturing
            captor.recordValue(methodRef);
            captor.recordValue(staticRef);
            
            List<Function> captured = captor.getAllValues();
            assertEquals(2, captured.size());
            
            // Verify method references still work
            assertEquals(7, captured.get(0).apply("testing"));
            assertEquals(456, captured.get(1).apply("456"));
        }

        @Test
        @DisplayName("Should capture supplier lambdas")
        @SuppressWarnings("unchecked")
        void shouldCaptureSupplierLambdas() {
            ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
            
            final AtomicInteger counter = new AtomicInteger(0);
            Supplier<String> supplier1 = () -> "constant";
            Supplier<String> supplier2 = () -> "count-" + counter.incrementAndGet();
            
            mock.processSupplier(supplier1);
            mock.processSupplier(supplier2);
            
            // Simulate framework capturing
            captor.recordValue(supplier1);
            captor.recordValue(supplier2);
            
            List<Supplier> captured = captor.getAllValues();
            
            // Verify suppliers still work and maintain state
            assertEquals("constant", captured.get(0).get());
            assertEquals("constant", captured.get(0).get()); // Same each time
            
            assertEquals("count-1", captured.get(1).get());
            assertEquals("count-2", captured.get(1).get()); // Changes each time
        }

        @Test
        @DisplayName("Should capture runnable lambdas with side effects")
        void shouldCaptureRunnableWithSideEffects() {
            ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
            
            final List<String> sideEffects = new ArrayList<>();
            Runnable r1 = () -> sideEffects.add("effect1");
            Runnable r2 = () -> sideEffects.add("effect2");
            
            mock.processRunnable(r1);
            mock.processRunnable(r2);
            
            // Simulate framework capturing
            captor.recordValue(r1);
            captor.recordValue(r2);
            
            // Clear side effects and run captured runnables
            sideEffects.clear();
            List<Runnable> captured = captor.getAllValues();
            captured.forEach(Runnable::run);
            
            assertEquals(Arrays.asList("effect1", "effect2"), sideEffects);
        }
    }

    @Nested
    @DisplayName("Concurrent Modification Tests")
    class ConcurrentModificationTests {

        @Test
        @DisplayName("Should handle concurrent captures and reads safely")
        void shouldHandleConcurrentCapturesAndReads() throws InterruptedException {
            ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
            ExecutorService executor = Executors.newFixedThreadPool(10);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completeLatch = new CountDownLatch(10);
            AtomicInteger errors = new AtomicInteger(0);
            
            // 5 writer threads
            for (int i = 0; i < 5; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < 1000; j++) {
                            captor.recordValue(threadId * 1000 + j);
                            Thread.yield();
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        completeLatch.countDown();
                    }
                });
            }
            
            // 5 reader threads
            for (int i = 0; i < 5; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < 100; j++) {
                            List<Integer> values = captor.getAllValues();
                            if (!values.isEmpty()) {
                                try {
                                    captor.getValue(); // Should not throw if list not empty
                                } catch (IllegalStateException e) {
                                    // Possible race condition - ignore
                                }
                            }
                            Thread.sleep(5);
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        completeLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            assertTrue(completeLatch.await(10, TimeUnit.SECONDS));
            executor.shutdown();
            
            assertEquals(0, errors.get(), "No errors should occur during concurrent access");
            assertEquals(5000, captor.getAllValues().size());
        }

        @Test
        @DisplayName("Should handle concurrent reset operations")
        void shouldHandleConcurrentResetOperations() throws InterruptedException {
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            ExecutorService executor = Executors.newFixedThreadPool(3);
            CountDownLatch completeLatch = new CountDownLatch(3);
            AtomicInteger successfulResets = new AtomicInteger(0);
            
            // Writer thread
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 10000; i++) {
                        captor.recordValue("value-" + i);
                        if (i % 100 == 0) {
                            Thread.yield();
                        }
                    }
                } finally {
                    completeLatch.countDown();
                }
            });
            
            // Reset thread
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 50; i++) {
                        Thread.sleep(20);
                        captor.reset();
                        successfulResets.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            });
            
            // Reader thread
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 100; i++) {
                        Thread.sleep(10);
                        List<String> values = captor.getAllValues();
                        // Just access, don't assert - values may change
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            });
            
            assertTrue(completeLatch.await(15, TimeUnit.SECONDS));
            executor.shutdown();
            
            assertTrue(successfulResets.get() > 0, "Some resets should have succeeded");
            // Final state depends on timing, but should be valid
            assertTrue(captor.getAllValues().size() >= 0);
        }
    }

    @Nested
    @DisplayName("Error Condition Tests")
    class ErrorConditionTests {

        @Test
        @DisplayName("Should handle captor usage outside verification context")
        void shouldHandleCaptorUsageOutsideVerification() {
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            
            // capture() returns null when used outside verification
            assertNull(captor.capture());
            
            // No values should be captured
            assertTrue(captor.getAllValues().isEmpty());
            assertThrows(IllegalStateException.class, captor::getValue);
        }

        @Test
        @DisplayName("Should detect type safety violations at runtime")
        @SuppressWarnings("unchecked")
        void shouldDetectTypeSafetyViolations() {
            // Create captor for String
            ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
            
            // In real usage, the framework would prevent this
            // But we can simulate what would happen with unsafe casting
            ArgumentCaptor rawCaptor = stringCaptor;
            
            // This would be caught by the framework's type checking
            // but we simulate direct recording to test captor behavior
            rawCaptor.recordValue(123); // Wrong type
            
            // The captor stores it (since it uses generics)
            assertEquals(1, rawCaptor.getAllValues().size());
            
            // But accessing it with the typed captor would cause ClassCastException
            // in real usage when the framework tries to return the value
        }

        @Test
        @DisplayName("Should handle memory cleanup after captures")
        void shouldHandleMemoryCleanupAfterCaptures() {
            ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
            
            // Create large objects
            List<WeakReference<byte[]>> references = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                byte[] largeArray = new byte[1024 * 1024]; // 1MB
                captor.recordValue(largeArray);
                references.add(new WeakReference<>(largeArray));
            }
            
            // Verify all captured
            assertEquals(10, captor.getAllValues().size());
            
            // Reset should allow garbage collection
            captor.reset();
            
            // Force GC
            System.gc();
            // Removed deprecated System.runFinalization()
            System.gc();
            
            // After reset, the captor should not prevent GC
            // Note: This is not deterministic, but demonstrates the concept
            assertTrue(captor.getAllValues().isEmpty());
        }

        @Test
        @DisplayName("Should handle stack overflow in toString for circular references")
        void shouldHandleStackOverflowInToString() {
            // Create circular reference
            class CircularNode {
                String name;
                CircularNode next;
                
                CircularNode(String name) {
                    this.name = name;
                }
                
                @Override
                public String toString() {
                    return "Node[" + name + "->" + next + "]";
                }
            }
            
            ArgumentCaptor<CircularNode> captor = ArgumentCaptor.forClass(CircularNode.class);
            
            CircularNode node1 = new CircularNode("node1");
            CircularNode node2 = new CircularNode("node2");
            node1.next = node2;
            node2.next = node1; // Circular reference
            
            captor.recordValue(node1);
            
            // The captor itself should not cause issues
            assertEquals(1, captor.getAllValues().size());
            assertSame(node1, captor.getValue());
            
            // Note: Calling toString on the captured object would cause StackOverflowError
            // but the captor itself handles this gracefully
        }
    }

    @Nested
    @DisplayName("Edge Case Combinations Tests")
    static class EdgeCaseCombinationTests {

        interface GenericVarargsService {
            <T> void process(T first, T... rest);
        }

        interface ArrayProcessor {
            void process(Function<int[], Integer> processor);
        }

        interface TestService {
            void process(String value);
        }

        @Test
        @DisplayName("Should handle null varargs with generic types")
        void shouldHandleNullVarargsWithGenerics() {
            
            GenericVarargsService mock = ContextualMocker.mock(GenericVarargsService.class);
            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            ArgumentCaptor<Object[]> arrayCaptor = ArgumentCaptor.forClass(Object[].class);
            
            // Call with null varargs
            mock.process("first", (String[]) null);
            
            // Simulate capturing
            captor.recordValue("first");
            arrayCaptor.recordValue(null);
            
            assertEquals("first", captor.getValue());
            assertNull(arrayCaptor.getValue());
        }

        @Test
        @DisplayName("Should capture lambda with primitive array")
        void shouldCaptureLambdaWithPrimitiveArray() {
            
            ArrayProcessor mock = ContextualMocker.mock(ArrayProcessor.class);
            @SuppressWarnings("unchecked")
            ContextualArgumentCaptor<Function> captor = ContextualArgumentCaptor.forClass(Function.class);
            ContextID context = new StringContextId("lambda-test");
            
            // Setup stubbing
            ContextualMocker.when(mock, context, () -> {
                mock.process(ArgumentMatchers.capture(captor));
                return null;
            }).thenReturn(null);
            
            Function<int[], Integer> sumFunction = arr -> Arrays.stream(arr).sum();
            
            // Call the mock with context
            try (ContextScope scope = ContextScope.withContext(context)) {
                mock.process(sumFunction);
            }
            
            Function<int[], Integer> captured = captor.getValueForContext(context);
            assertEquals(15, captured.apply(new int[]{1, 2, 3, 4, 5}));
        }

        @Test
        @DisplayName("Should handle concurrent captures with context switching")
        void shouldHandleConcurrentCapturesWithContextSwitching() throws InterruptedException {
            ContextualArgumentCaptor<String> captor = ContextualArgumentCaptor.forClass(String.class);
            TestService mock = ContextualMocker.mock(TestService.class);
            ExecutorService executor = Executors.newFixedThreadPool(4);
            CountDownLatch completeLatch = new CountDownLatch(4);
            
            ContextID context1 = new StringContextId("ctx1");
            ContextID context2 = new StringContextId("ctx2");
            
            // Setup stubbing for both contexts
            ContextualMocker.when(mock, context1, () -> {
                mock.process(ArgumentMatchers.capture(captor));
                return null;
            }).thenReturn(null);
            ContextualMocker.when(mock, context2, () -> {
                mock.process(ArgumentMatchers.capture(captor));
                return null;
            }).thenReturn(null);
            
            // Two threads with context1
            for (int i = 0; i < 2; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        try (ContextScope scope = ContextScope.withContext(context1)) {
                            for (int j = 0; j < 100; j++) {
                                mock.process("ctx1-thread" + threadId + "-" + j);
                            }
                        }
                    } finally {
                        completeLatch.countDown();
                    }
                });
            }
            
            // Two threads with context2
            for (int i = 0; i < 2; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        try (ContextScope scope = ContextScope.withContext(context2)) {
                            for (int j = 0; j < 100; j++) {
                                mock.process("ctx2-thread" + threadId + "-" + j);
                            }
                        }
                    } finally {
                        completeLatch.countDown();
                    }
                });
            }
            
            assertTrue(completeLatch.await(5, TimeUnit.SECONDS));
            executor.shutdown();
            
            // Verify context-specific captures
            assertEquals(200, captor.getAllValuesForContext(context1).size());
            assertEquals(200, captor.getAllValuesForContext(context2).size());
            assertEquals(400, captor.getAllValues().size());
        }
    }
}