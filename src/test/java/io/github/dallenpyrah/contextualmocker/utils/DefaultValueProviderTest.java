package io.github.dallenpyrah.contextualmocker.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;
import java.util.concurrent.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DefaultValueProvider}.
 */
class DefaultValueProviderTest {
    
    @Test
    @DisplayName("Should have private constructor that throws AssertionError")
    void shouldHavePrivateConstructor() throws Exception {
        Constructor<DefaultValueProvider> constructor = DefaultValueProvider.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        
        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, constructor::newInstance);
        
        try {
            constructor.newInstance();
        } catch (InvocationTargetException e) {
            assertInstanceOf(AssertionError.class, e.getCause());
            assertEquals("DefaultValueProvider should not be instantiated", e.getCause().getMessage());
        }
    }
    
    @Test
    @DisplayName("Should throw NullPointerException for null type")
    void shouldThrowNullPointerExceptionForNullType() {
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> DefaultValueProvider.getDefaultValue(null)
        );
        assertEquals("Type cannot be null", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should return false for boolean primitive")
    void shouldReturnFalseForBooleanPrimitive() {
        assertEquals(false, DefaultValueProvider.getDefaultValue(boolean.class));
    }
    
    @Test
    @DisplayName("Should return false for Boolean wrapper")
    void shouldReturnFalseForBooleanWrapper() {
        assertEquals(Boolean.FALSE, DefaultValueProvider.getDefaultValue(Boolean.class));
    }
    
    @Test
    @DisplayName("Should return zero for numeric primitives")
    void shouldReturnZeroForNumericPrimitives() {
        // Test byte
        assertEquals((byte) 0, DefaultValueProvider.getDefaultValue(byte.class));
        
        // Test short
        assertEquals((short) 0, DefaultValueProvider.getDefaultValue(short.class));
        
        // Test int
        assertEquals(0, DefaultValueProvider.getDefaultValue(int.class));
        
        // Test long
        assertEquals(0L, DefaultValueProvider.getDefaultValue(long.class));
    }
    
    @Test
    @DisplayName("Should return zero for numeric wrappers")
    void shouldReturnZeroForNumericWrappers() {
        // Test Byte
        assertEquals(Byte.valueOf((byte) 0), DefaultValueProvider.getDefaultValue(Byte.class));
        
        // Test Short
        assertEquals(Short.valueOf((short) 0), DefaultValueProvider.getDefaultValue(Short.class));
        
        // Test Integer
        assertEquals(Integer.valueOf(0), DefaultValueProvider.getDefaultValue(Integer.class));
        
        // Test Long
        assertEquals(Long.valueOf(0L), DefaultValueProvider.getDefaultValue(Long.class));
    }
    
    @Test
    @DisplayName("Should return 0.0 for float primitive")
    void shouldReturnZeroForFloatPrimitive() {
        assertEquals(0.0f, DefaultValueProvider.getDefaultValue(float.class));
    }
    
    @Test
    @DisplayName("Should return 0.0 for double primitive")
    void shouldReturnZeroForDoublePrimitive() {
        assertEquals(0.0d, DefaultValueProvider.getDefaultValue(double.class));
    }
    
    @Test
    @DisplayName("Should return 0.0 for Float wrapper")
    void shouldReturnZeroForFloatWrapper() {
        assertEquals(Float.valueOf(0.0f), DefaultValueProvider.getDefaultValue(Float.class));
    }
    
    @Test
    @DisplayName("Should return 0.0 for Double wrapper")
    void shouldReturnZeroForDoubleWrapper() {
        assertEquals(Double.valueOf(0.0d), DefaultValueProvider.getDefaultValue(Double.class));
    }
    
    @Test
    @DisplayName("Should return null character for char primitive")
    void shouldReturnNullCharForCharPrimitive() {
        assertEquals('\0', DefaultValueProvider.getDefaultValue(char.class));
    }
    
    @Test
    @DisplayName("Should return null character for Character wrapper")
    void shouldReturnNullCharForCharacterWrapper() {
        assertEquals(Character.valueOf('\0'), DefaultValueProvider.getDefaultValue(Character.class));
    }
    
    @Test
    @DisplayName("Should return empty string for String type")
    void shouldReturnEmptyStringForStringType() {
        assertEquals("", DefaultValueProvider.getDefaultValue(String.class));
    }
    
    @Test
    @DisplayName("Should return null for arbitrary Object types")
    void shouldReturnNullForObjectTypes() {
        assertNull(DefaultValueProvider.getDefaultValue(Object.class));
        assertNull(DefaultValueProvider.getDefaultValue(Date.class));
        assertNull(DefaultValueProvider.getDefaultValue(StringBuilder.class));
        assertNull(DefaultValueProvider.getDefaultValue(Thread.class));
    }
    
    @Test
    @DisplayName("Should return empty arrays for array types")
    void shouldReturnEmptyArraysForArrayTypes() {
        // Primitive arrays
        assertArrayEquals(new int[0], (int[]) DefaultValueProvider.getDefaultValue(int[].class));
        assertArrayEquals(new double[0], (double[]) DefaultValueProvider.getDefaultValue(double[].class));
        assertArrayEquals(new boolean[0], (boolean[]) DefaultValueProvider.getDefaultValue(boolean[].class));
        
        // Object arrays
        assertArrayEquals(new String[0], (String[]) DefaultValueProvider.getDefaultValue(String[].class));
        assertArrayEquals(new Object[0], (Object[]) DefaultValueProvider.getDefaultValue(Object[].class));
        
        // Multi-dimensional arrays
        assertArrayEquals(new int[0][], (int[][]) DefaultValueProvider.getDefaultValue(int[][].class));
    }
    
    @Test
    @DisplayName("Should return empty List implementations")
    void shouldReturnEmptyListImplementations() {
        // ArrayList
        Object arrayList = DefaultValueProvider.getDefaultValue(ArrayList.class);
        assertInstanceOf(ArrayList.class, arrayList);
        assertTrue(((ArrayList<?>) arrayList).isEmpty());
        
        // LinkedList
        Object linkedList = DefaultValueProvider.getDefaultValue(LinkedList.class);
        assertInstanceOf(LinkedList.class, linkedList);
        assertTrue(((LinkedList<?>) linkedList).isEmpty());
        
        // List interface
        Object list = DefaultValueProvider.getDefaultValue(List.class);
        assertInstanceOf(List.class, list);
        assertTrue(((List<?>) list).isEmpty());
        
        // CopyOnWriteArrayList
        Object cowList = DefaultValueProvider.getDefaultValue(CopyOnWriteArrayList.class);
        assertInstanceOf(CopyOnWriteArrayList.class, cowList);
        assertTrue(((CopyOnWriteArrayList<?>) cowList).isEmpty());
        
        // Vector
        Object vector = DefaultValueProvider.getDefaultValue(Vector.class);
        assertInstanceOf(Vector.class, vector);
        assertTrue(((Vector<?>) vector).isEmpty());
        
        // Stack
        Object stack = DefaultValueProvider.getDefaultValue(Stack.class);
        assertInstanceOf(Stack.class, stack);
        assertTrue(((Stack<?>) stack).isEmpty());
    }
    
    @Test
    @DisplayName("Should return empty Set implementations")
    void shouldReturnEmptySetImplementations() {
        // HashSet
        Object hashSet = DefaultValueProvider.getDefaultValue(HashSet.class);
        assertInstanceOf(HashSet.class, hashSet);
        assertTrue(((HashSet<?>) hashSet).isEmpty());
        
        // LinkedHashSet
        Object linkedHashSet = DefaultValueProvider.getDefaultValue(LinkedHashSet.class);
        assertInstanceOf(LinkedHashSet.class, linkedHashSet);
        assertTrue(((LinkedHashSet<?>) linkedHashSet).isEmpty());
        
        // TreeSet
        Object treeSet = DefaultValueProvider.getDefaultValue(TreeSet.class);
        assertInstanceOf(TreeSet.class, treeSet);
        assertTrue(((TreeSet<?>) treeSet).isEmpty());
        
        // Set interface
        Object set = DefaultValueProvider.getDefaultValue(Set.class);
        assertInstanceOf(Set.class, set);
        assertTrue(((Set<?>) set).isEmpty());
        
        // SortedSet interface
        Object sortedSet = DefaultValueProvider.getDefaultValue(SortedSet.class);
        assertInstanceOf(SortedSet.class, sortedSet);
        assertTrue(((SortedSet<?>) sortedSet).isEmpty());
        
        // NavigableSet interface
        Object navigableSet = DefaultValueProvider.getDefaultValue(NavigableSet.class);
        assertInstanceOf(NavigableSet.class, navigableSet);
        assertTrue(((NavigableSet<?>) navigableSet).isEmpty());
        
        // CopyOnWriteArraySet
        Object cowSet = DefaultValueProvider.getDefaultValue(CopyOnWriteArraySet.class);
        assertInstanceOf(CopyOnWriteArraySet.class, cowSet);
        assertTrue(((CopyOnWriteArraySet<?>) cowSet).isEmpty());
        
        // ConcurrentSkipListSet
        Object skipListSet = DefaultValueProvider.getDefaultValue(ConcurrentSkipListSet.class);
        assertInstanceOf(ConcurrentSkipListSet.class, skipListSet);
        assertTrue(((ConcurrentSkipListSet<?>) skipListSet).isEmpty());
    }
    
    @Test
    @DisplayName("Should return empty Queue and Deque implementations")
    void shouldReturnEmptyQueueAndDequeImplementations() {
        // ArrayDeque
        Object arrayDeque = DefaultValueProvider.getDefaultValue(ArrayDeque.class);
        assertInstanceOf(ArrayDeque.class, arrayDeque);
        assertTrue(((ArrayDeque<?>) arrayDeque).isEmpty());
        
        // Deque interface
        Object deque = DefaultValueProvider.getDefaultValue(Deque.class);
        assertInstanceOf(Deque.class, deque);
        assertTrue(((Deque<?>) deque).isEmpty());
        
        // Queue interface
        Object queue = DefaultValueProvider.getDefaultValue(Queue.class);
        assertInstanceOf(Queue.class, queue);
        assertTrue(((Queue<?>) queue).isEmpty());
        
        // PriorityQueue
        Object priorityQueue = DefaultValueProvider.getDefaultValue(PriorityQueue.class);
        assertInstanceOf(PriorityQueue.class, priorityQueue);
        assertTrue(((PriorityQueue<?>) priorityQueue).isEmpty());
    }
    
    @Test
    @DisplayName("Should return empty Map implementations")
    void shouldReturnEmptyMapImplementations() {
        // HashMap
        Object hashMap = DefaultValueProvider.getDefaultValue(HashMap.class);
        assertInstanceOf(HashMap.class, hashMap);
        assertTrue(((HashMap<?, ?>) hashMap).isEmpty());
        
        // LinkedHashMap
        Object linkedHashMap = DefaultValueProvider.getDefaultValue(LinkedHashMap.class);
        assertInstanceOf(LinkedHashMap.class, linkedHashMap);
        assertTrue(((LinkedHashMap<?, ?>) linkedHashMap).isEmpty());
        
        // TreeMap
        Object treeMap = DefaultValueProvider.getDefaultValue(TreeMap.class);
        assertInstanceOf(TreeMap.class, treeMap);
        assertTrue(((TreeMap<?, ?>) treeMap).isEmpty());
        
        // Map interface
        Object map = DefaultValueProvider.getDefaultValue(Map.class);
        assertInstanceOf(Map.class, map);
        assertTrue(((Map<?, ?>) map).isEmpty());
        
        // SortedMap interface
        Object sortedMap = DefaultValueProvider.getDefaultValue(SortedMap.class);
        assertInstanceOf(SortedMap.class, sortedMap);
        assertTrue(((SortedMap<?, ?>) sortedMap).isEmpty());
        
        // NavigableMap interface
        Object navigableMap = DefaultValueProvider.getDefaultValue(NavigableMap.class);
        assertInstanceOf(NavigableMap.class, navigableMap);
        assertTrue(((NavigableMap<?, ?>) navigableMap).isEmpty());
        
        // ConcurrentHashMap
        Object concurrentHashMap = DefaultValueProvider.getDefaultValue(ConcurrentHashMap.class);
        assertInstanceOf(ConcurrentHashMap.class, concurrentHashMap);
        assertTrue(((ConcurrentHashMap<?, ?>) concurrentHashMap).isEmpty());
        
        // ConcurrentMap interface
        Object concurrentMap = DefaultValueProvider.getDefaultValue(ConcurrentMap.class);
        assertInstanceOf(ConcurrentMap.class, concurrentMap);
        assertTrue(((ConcurrentMap<?, ?>) concurrentMap).isEmpty());
        
        // ConcurrentSkipListMap
        Object skipListMap = DefaultValueProvider.getDefaultValue(ConcurrentSkipListMap.class);
        assertInstanceOf(ConcurrentSkipListMap.class, skipListMap);
        assertTrue(((ConcurrentSkipListMap<?, ?>) skipListMap).isEmpty());
        
        // Hashtable
        Object hashtable = DefaultValueProvider.getDefaultValue(Hashtable.class);
        assertInstanceOf(Hashtable.class, hashtable);
        assertTrue(((Hashtable<?, ?>) hashtable).isEmpty());
        
        // IdentityHashMap
        Object identityHashMap = DefaultValueProvider.getDefaultValue(IdentityHashMap.class);
        assertInstanceOf(IdentityHashMap.class, identityHashMap);
        assertTrue(((IdentityHashMap<?, ?>) identityHashMap).isEmpty());
        
        // WeakHashMap
        Object weakHashMap = DefaultValueProvider.getDefaultValue(WeakHashMap.class);
        assertInstanceOf(WeakHashMap.class, weakHashMap);
        assertTrue(((WeakHashMap<?, ?>) weakHashMap).isEmpty());
        
        // Properties
        Object properties = DefaultValueProvider.getDefaultValue(Properties.class);
        assertInstanceOf(Properties.class, properties);
        assertTrue(((Properties) properties).isEmpty());
    }
    
    @Test
    @DisplayName("Should return empty Collection for unknown collection types")
    void shouldReturnEmptyCollectionForUnknownCollectionTypes() {
        // Test with Collection interface directly
        Object collection = DefaultValueProvider.getDefaultValue(Collection.class);
        assertInstanceOf(Collection.class, collection);
        assertTrue(((Collection<?>) collection).isEmpty());
    }
    
    @Test
    @DisplayName("Should return empty Map for unknown map types")
    void shouldReturnEmptyMapForUnknownMapTypes() {
        // Test with Map interface directly
        Object map = DefaultValueProvider.getDefaultValue(Map.class);
        assertInstanceOf(Map.class, map);
        assertTrue(((Map<?, ?>) map).isEmpty());
    }
    
    @Test
    @DisplayName("Should use cached values for performance")
    void shouldUseCachedValuesForPerformance() {
        // Call multiple times and ensure same instance is returned for non-null values
        Object first = DefaultValueProvider.getDefaultValue(String.class);
        Object second = DefaultValueProvider.getDefaultValue(String.class);
        assertSame(first, second, "Should return the same cached instance");
        
        // Test with wrapper types
        Object intFirst = DefaultValueProvider.getDefaultValue(Integer.class);
        Object intSecond = DefaultValueProvider.getDefaultValue(Integer.class);
        assertSame(intFirst, intSecond, "Should return the same cached instance for Integer");
        
        // Test that null returns are consistent (though not cached)
        Object nullFirst = DefaultValueProvider.getDefaultValue(Object.class);
        Object nullSecond = DefaultValueProvider.getDefaultValue(Object.class);
        assertNull(nullFirst);
        assertNull(nullSecond);
    }
    
    @Test
    @DisplayName("Should be thread-safe")
    void shouldBeThreadSafe() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Thread> threads = new ArrayList<>();
        Set<Object> results = Collections.synchronizedSet(new HashSet<>());
        
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    // Access various types concurrently
                    results.add(DefaultValueProvider.getDefaultValue(String.class));
                    results.add(DefaultValueProvider.getDefaultValue(ArrayList.class));
                    results.add(DefaultValueProvider.getDefaultValue(HashMap.class));
                    results.add(DefaultValueProvider.getDefaultValue(int[].class));
                } finally {
                    latch.countDown();
                }
            });
            threads.add(thread);
            thread.start();
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "All threads should complete");
        
        // Verify results are consistent
        assertTrue(results.contains(""));
        assertTrue(results.stream().anyMatch(o -> o instanceof ArrayList));
        assertTrue(results.stream().anyMatch(o -> o instanceof HashMap));
        assertTrue(results.stream().anyMatch(o -> o != null && o.getClass().isArray()));
    }
}