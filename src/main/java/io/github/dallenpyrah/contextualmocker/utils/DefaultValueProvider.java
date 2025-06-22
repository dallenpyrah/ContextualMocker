package io.github.dallenpyrah.contextualmocker.utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.lang.reflect.Array;

/**
 * Utility class that provides default values for various Java types.
 * <p>
 * This class is used internally by ContextMock to return appropriate default values
 * when methods are called on mocks without explicit stubbing, particularly useful
 * for ArgumentCaptor.capture() to return placeholder values.
 * </p>
 * <p>
 * The following default values are provided:
 * <ul>
 *   <li>Primitives: false for boolean, 0 for numeric types, '\0' for char</li>
 *   <li>Object types (including String): null</li>
 *   <li>Collections: empty instances of the appropriate collection type</li>
 *   <li>Maps: empty instances of the appropriate map type</li>
 *   <li>Arrays: empty arrays of the appropriate type</li>
 * </ul>
 * </p>
 * <p>
 * This class is thread-safe and uses a cache to improve performance for
 * frequently requested default values.
 * </p>
 * 
 * @since 1.0
 */
public final class DefaultValueProvider {
    
    /**
     * Cache for storing default values to avoid repeated instantiation.
     * Thread-safe via ConcurrentHashMap.
     */
    private static final ConcurrentMap<Class<?>, Object> DEFAULT_VALUE_CACHE = new ConcurrentHashMap<>();
    
    /**
     * Static initializer to pre-populate cache with common default values.
     */
    static {
        // Pre-populate cache for common types
        // Note: We don't cache null values since ConcurrentHashMap doesn't support null values
    }
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private DefaultValueProvider() {
        throw new AssertionError("DefaultValueProvider should not be instantiated");
    }
    
    /**
     * Returns the default value for the given type.
     * <p>
     * For primitive types, returns their Java language defaults:
     * <ul>
     *   <li>boolean: false</li>
     *   <li>byte: 0</li>
     *   <li>short: 0</li>
     *   <li>int: 0</li>
     *   <li>long: 0L</li>
     *   <li>float: 0.0f</li>
     *   <li>double: 0.0d</li>
     *   <li>char: '\0'</li>
     * </ul>
     * </p>
     * <p>
     * For reference types:
     * <ul>
     *   <li>Collection types: empty collection instance</li>
     *   <li>Map types: empty map instance</li>
     *   <li>Array types: empty array</li>
     *   <li>All Object types (including String): null</li>
     * </ul>
     * </p>
     * 
     * @param type the class type to get default value for (must not be null)
     * @return the default value for the specified type
     * @throws NullPointerException if type is null
     */
    public static Object getDefaultValue(Class<?> type) {
        Objects.requireNonNull(type, "Type cannot be null");
        
        // Check cache first
        Object cachedValue = DEFAULT_VALUE_CACHE.get(type);
        if (cachedValue != null) {
            return cachedValue;
        }
        
        // Handle primitives
        if (type.isPrimitive()) {
            return getPrimitiveDefault(type);
        }
        
        // Handle arrays
        if (type.isArray()) {
            return Array.newInstance(type.getComponentType(), 0);
        }
        
        // Handle collections and maps
        Object defaultValue = getCollectionOrMapDefault(type);
        if (defaultValue != null) {
            // Cache the created instance for future use
            DEFAULT_VALUE_CACHE.putIfAbsent(type, defaultValue);
            return DEFAULT_VALUE_CACHE.get(type);
        }
        
        // For all other object types, return null
        // Note: ConcurrentHashMap doesn't support null values, so we don't cache null returns
        return null;
    }
    
    /**
     * Returns the default value for primitive types.
     * 
     * @param type the primitive type
     * @return the default value for the primitive type
     */
    private static Object getPrimitiveDefault(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0d;
        if (type == char.class) return '\0';
        
        // Should never reach here for primitives
        return null;
    }
    
    /**
     * Returns the default value for Collection or Map types.
     * Creates appropriate empty instances based on the type.
     * 
     * @param type the collection or map type
     * @return an empty instance of the collection/map type, or null if not a collection/map
     */
    private static Object getCollectionOrMapDefault(Class<?> type) {
        // Handle Collection types
        if (Collection.class.isAssignableFrom(type)) {
            return createEmptyCollection(type);
        }
        
        // Handle Map types
        if (Map.class.isAssignableFrom(type)) {
            return createEmptyMap(type);
        }
        
        return null;
    }
    
    /**
     * Creates an empty Collection instance based on the type.
     * 
     * @param type the collection type
     * @return an empty collection instance
     */
    private static Collection<?> createEmptyCollection(Class<?> type) {
        // Handle concrete collection types
        if (type == ArrayList.class || type == List.class) {
            return new ArrayList<>();
        }
        if (type == LinkedList.class) {
            return new LinkedList<>();
        }
        if (type == HashSet.class || type == Set.class) {
            return new HashSet<>();
        }
        if (type == LinkedHashSet.class) {
            return new LinkedHashSet<>();
        }
        if (type == TreeSet.class || type == SortedSet.class || type == NavigableSet.class) {
            return new TreeSet<>();
        }
        if (type == CopyOnWriteArrayList.class) {
            return new CopyOnWriteArrayList<>();
        }
        if (type == CopyOnWriteArraySet.class) {
            return new CopyOnWriteArraySet<>();
        }
        if (type == ConcurrentSkipListSet.class) {
            return new ConcurrentSkipListSet<>();
        }
        if (type == ArrayDeque.class || type == Deque.class || type == Queue.class) {
            return new ArrayDeque<>();
        }
        if (type == PriorityQueue.class) {
            return new PriorityQueue<>();
        }
        if (type == Vector.class) {
            return new Vector<>();
        }
        if (type == Stack.class) {
            return new Stack<>();
        }
        
        // Default to ArrayList for unknown Collection types
        if (Collection.class.isAssignableFrom(type)) {
            return new ArrayList<>();
        }
        
        return null;
    }
    
    /**
     * Creates an empty Map instance based on the type.
     * 
     * @param type the map type
     * @return an empty map instance
     */
    private static Map<?, ?> createEmptyMap(Class<?> type) {
        // Handle concrete map types
        if (type == HashMap.class || type == Map.class) {
            return new HashMap<>();
        }
        if (type == LinkedHashMap.class) {
            return new LinkedHashMap<>();
        }
        if (type == TreeMap.class || type == SortedMap.class || type == NavigableMap.class) {
            return new TreeMap<>();
        }
        if (type == ConcurrentHashMap.class || type == ConcurrentMap.class) {
            return new ConcurrentHashMap<>();
        }
        if (type == ConcurrentSkipListMap.class) {
            return new ConcurrentSkipListMap<>();
        }
        if (type == Hashtable.class) {
            return new Hashtable<>();
        }
        if (type == IdentityHashMap.class) {
            return new IdentityHashMap<>();
        }
        if (type == WeakHashMap.class) {
            return new WeakHashMap<>();
        }
        if (type == Properties.class) {
            return new Properties();
        }
        
        // Default to HashMap for unknown Map types
        if (Map.class.isAssignableFrom(type)) {
            return new HashMap<>();
        }
        
        return null;
    }
}