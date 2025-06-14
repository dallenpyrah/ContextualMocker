package io.github.dallenpyrah.contextualmocker.matchers;

import io.github.dallenpyrah.contextualmocker.core.ContextHolder;
import io.github.dallenpyrah.contextualmocker.core.ContextID;
import io.github.dallenpyrah.contextualmocker.core.StringContextId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static io.github.dallenpyrah.contextualmocker.core.ContextualMocker.*;
import static io.github.dallenpyrah.contextualmocker.matchers.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class ExtendedArgumentMatchersTest {

    public interface TestService {
        String processString(String input);
        int processNumber(int number);
        long processLong(long number);
        double processDouble(double number);
        void processObject(Object obj);
        String processCollection(List<String> items);
        String processMultiple(String str, int num, Object obj);
    }

    private TestService mockService;
    private ContextID testContext;

    @BeforeEach
    void setUp() {
        mockService = mock(TestService.class);
        testContext = new StringContextId("test-context");
        ContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        ContextHolder.clearContext();
    }

    @Test
    void testAnyStringMatcher() {
        when(mockService, testContext, () -> mockService.processString(anyString()))
            .thenReturn("matched");

        try (var scope = scopedContext(testContext)) {
            assertEquals("matched", mockService.processString("hello"));
            assertEquals("matched", mockService.processString(""));
            assertEquals("matched", mockService.processString("any string"));
            
            scope.verify(mockService, times(3), () -> mockService.processString(anyString()));
        }
    }

    @Test
    void testContainsMatcher() {
        when(mockService, testContext, () -> mockService.processString(contains("test")))
            .thenReturn("contains test");

        try (var scope = scopedContext(testContext)) {
            assertEquals("contains test", mockService.processString("this is a test"));
            assertEquals("contains test", mockService.processString("testing"));
            assertNull(mockService.processString("no match"));
            
            scope.verify(mockService, times(2), () -> mockService.processString(contains("test")));
        }
    }

    @Test
    void testStartsWithMatcher() {
        when(mockService, testContext, () -> mockService.processString(startsWith("prefix")))
            .thenReturn("starts with prefix");

        try (var scope = scopedContext(testContext)) {
            assertEquals("starts with prefix", mockService.processString("prefix123"));
            assertEquals("starts with prefix", mockService.processString("prefix"));
            assertNull(mockService.processString("not prefix"));
            
            scope.verify(mockService, times(2), () -> mockService.processString(startsWith("prefix")));
        }
    }

    @Test
    void testEndsWithMatcher() {
        when(mockService, testContext, () -> mockService.processString(endsWith("suffix")))
            .thenReturn("ends with suffix");

        try (var scope = scopedContext(testContext)) {
            assertEquals("ends with suffix", mockService.processString("123suffix"));
            assertEquals("ends with suffix", mockService.processString("suffix"));
            assertNull(mockService.processString("suffix123"));
            
            scope.verify(mockService, times(2), () -> mockService.processString(endsWith("suffix")));
        }
    }

    @Test
    void testRegexMatcher() {
        when(mockService, testContext, () -> mockService.processString(matches("\\d+")))
            .thenReturn("is number");

        try (var scope = scopedContext(testContext)) {
            assertEquals("is number", mockService.processString("123"));
            assertEquals("is number", mockService.processString("456"));
            assertNull(mockService.processString("abc"));
            
            scope.verify(mockService, times(2), () -> mockService.processString(matches("\\d+")));
        }
    }

    @Test
    void testRegexPatternMatcher() {
        Pattern emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
        when(mockService, testContext, () -> mockService.processString(matches(emailPattern)))
            .thenReturn("valid email");

        try (var scope = scopedContext(testContext)) {
            assertEquals("valid email", mockService.processString("test@example.com"));
            assertEquals("valid email", mockService.processString("user.name+tag@domain.co.uk"));
            assertNull(mockService.processString("invalid-email"));
            
            scope.verify(mockService, times(2), () -> mockService.processString(matches(emailPattern)));
        }
    }

    @Test
    void testNullMatcher() {
        try (var scope = scopedContext(testContext)) {
            mockService.processObject(null);
            mockService.processObject("not null");
            
            scope.verify(mockService, times(1), () -> { mockService.processObject(isNull()); return null; });
            scope.verify(mockService, times(1), () -> { mockService.processObject(notNull()); return null; });
        }
    }

    @Test
    void testNotNullMatcher() {
        try (var scope = scopedContext(testContext)) {
            mockService.processObject("something");
            mockService.processObject(123);
            mockService.processObject(null);
            
            scope.verify(mockService, times(2), () -> { mockService.processObject(notNull()); return null; });
            scope.verify(mockService, times(1), () -> { mockService.processObject(isNull()); return null; });
        }
    }

    @Test
    void testPredicateMatcher() {
        when(mockService, testContext, () -> mockService.processString(argThat(s -> s != null && s.length() > 5)))
            .thenReturn("long string");

        try (var scope = scopedContext(testContext)) {
            assertEquals("long string", mockService.processString("this is long"));
            assertNull(mockService.processString("short"));
            
            scope.verify(mockService, times(1), () -> mockService.processString(argThat(s -> s != null && s.length() > 5)));
        }
    }

    @Test
    void testRangeMatchers() {
        when(mockService, testContext, () -> mockService.processNumber(intThat(10, 20)))
            .thenReturn(999);

        when(mockService, testContext, () -> mockService.processLong(longThat(100L, 200L)))
            .thenReturn(888L);

        when(mockService, testContext, () -> mockService.processDouble(doubleThat(1.0, 2.0)))
            .thenReturn(777.0);

        try (var scope = scopedContext(testContext)) {
            assertEquals(999, mockService.processNumber(15));
            assertEquals(888L, mockService.processLong(150L));
            assertEquals(777.0, mockService.processDouble(1.5));
            
            // Out of range - should return defaults
            assertEquals(0, mockService.processNumber(5));
            assertEquals(0L, mockService.processLong(50L));
            assertEquals(0.0, mockService.processDouble(0.5));
            
            scope.verify(mockService, times(1), () -> mockService.processNumber(intThat(10, 20)));
            scope.verify(mockService, times(1), () -> mockService.processLong(longThat(100L, 200L)));
            scope.verify(mockService, times(1), () -> mockService.processDouble(doubleThat(1.0, 2.0)));
        }
    }

    @Test
    void testAnyCollectionMatcher() {
        when(mockService, testContext, () -> mockService.processCollection(anyCollection()))
            .thenReturn("got collection");

        try (var scope = scopedContext(testContext)) {
            assertEquals("got collection", mockService.processCollection(Arrays.asList("a", "b")));
            assertEquals("got collection", mockService.processCollection(new ArrayList<>()));
            
            scope.verify(mockService, times(2), () -> mockService.processCollection(anyCollection()));
        }
    }

    @Test
    void testMixedMatchers() {
        when(mockService, testContext, () -> 
            mockService.processMultiple(startsWith("test"), intThat(1, 10), notNull()))
            .thenReturn("mixed match");

        try (var scope = scopedContext(testContext)) {
            assertEquals("mixed match", mockService.processMultiple("testing", 5, "object"));
            assertNull(mockService.processMultiple("wrong", 5, "object"));
            assertNull(mockService.processMultiple("testing", 15, "object"));
            assertNull(mockService.processMultiple("testing", 5, null));
            
            scope.verify(mockService, times(1), () -> 
                mockService.processMultiple(startsWith("test"), intThat(1, 10), notNull()));
        }
    }
}