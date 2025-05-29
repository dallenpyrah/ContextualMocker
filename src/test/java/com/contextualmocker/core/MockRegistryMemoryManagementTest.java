package com.contextualmocker.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.contextualmocker.core.ContextualMocker.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MockRegistry memory management features.
 */
class MockRegistryMemoryManagementTest {

    private MockRegistry.CleanupConfiguration originalConfig;

    @BeforeEach
    void setUp() {
        originalConfig = MockRegistry.getCleanupConfiguration();
        MockRegistry.disableAutoCleanup();
    }

    @AfterEach
    void tearDown() {
        MockRegistry.disableAutoCleanup();
        MockRegistry.setCleanupConfiguration(originalConfig);
    }

    @Test
    void testCleanupConfiguration() {
        MockRegistry.CleanupConfiguration config = new MockRegistry.CleanupConfiguration(
            1000, 2000, 5000, false
        );
        
        MockRegistry.setCleanupConfiguration(config);
        MockRegistry.CleanupConfiguration retrieved = MockRegistry.getCleanupConfiguration();
        
        assertEquals(1000, retrieved.getMaxInvocationsPerContext());
        assertEquals(2000, retrieved.getMaxAgeMillis());
        assertEquals(5000, retrieved.getCleanupIntervalMillis());
        assertFalse(retrieved.isAutoCleanupEnabled());
    }

    @Test
    void testDefaultConfiguration() {
        MockRegistry.CleanupConfiguration defaultConfig = MockRegistry.CleanupConfiguration.defaultConfig();
        
        assertEquals(10000, defaultConfig.getMaxInvocationsPerContext());
        assertEquals(300000, defaultConfig.getMaxAgeMillis());
        assertEquals(60000, defaultConfig.getCleanupIntervalMillis());
        assertTrue(defaultConfig.isAutoCleanupEnabled());
    }

    @Test
    void testMemoryUsageStats() {
        TestService mockService = mock(TestService.class);
        ContextID context1 = new StringContextId(UUID.randomUUID().toString());
        ContextID context2 = new StringContextId(UUID.randomUUID().toString());
        
        MockRegistry.MemoryUsageStats emptyStats = MockRegistry.getMemoryUsageStats();
        
        // Create some data and test stats while the context is active
        try (ContextScope scope1 = scopedContext(context1)) {
            when(mockService, context1, () -> mockService.getData()).thenReturn("test1");
            mockService.getData();
            mockService.getData();
            
            // Check stats while context is active
            MockRegistry.MemoryUsageStats activeStats = MockRegistry.getMemoryUsageStats();
            assertTrue(activeStats.getTotalMocks() > emptyStats.getTotalMocks(),
                "Expected mocks to increase, got: " + activeStats.getTotalMocks());
            assertTrue(activeStats.getTotalInvocations() > emptyStats.getTotalInvocations(),
                "Expected invocations to increase, got: " + activeStats.getTotalInvocations());
            assertTrue(activeStats.getTotalStubbingRules() > emptyStats.getTotalStubbingRules(),
                "Expected stubbing rules to increase, got: " + activeStats.getTotalStubbingRules());
        }
        
        // After context scope ends, invocations are cleared but mocks remain
        MockRegistry.MemoryUsageStats finalStats = MockRegistry.getMemoryUsageStats();
        assertTrue(finalStats.getTotalMocks() > emptyStats.getTotalMocks(),
            "Expected mocks to remain after context close, got: " + finalStats.getTotalMocks());
        
        // Test toString
        assertNotNull(finalStats.toString());
        assertTrue(finalStats.toString().contains("mocks="));
    }

    @Test
    void testClearMockData() {
        TestService mockService1 = mock(TestService.class);
        TestService mockService2 = mock(TestService.class);
        ContextID context = new StringContextId(UUID.randomUUID().toString());
        
        // Add data for both mocks
        try (ContextScope scope = scopedContext(context)) {
            when(mockService1, context, () -> mockService1.getData()).thenReturn("test1");
            when(mockService2, context, () -> mockService2.getData()).thenReturn("test2");
            mockService1.getData();
            mockService2.getData();
        }
        
        MockRegistry.MemoryUsageStats statsBefore = MockRegistry.getMemoryUsageStats();
        assertTrue(statsBefore.getTotalMocks() >= 2);
        
        // Clear data for one mock
        boolean cleared = MockRegistry.clearMockData(mockService1);
        assertTrue(cleared);
        
        // Verify that only one mock's data remains
        MockRegistry.MemoryUsageStats statsAfter = MockRegistry.getMemoryUsageStats();
        assertTrue(statsAfter.getTotalMocks() < statsBefore.getTotalMocks());
        
        // Try to clear non-existent mock
        boolean clearedAgain = MockRegistry.clearMockData(mockService1);
        assertFalse(clearedAgain);
        
        // Test with null
        assertFalse(MockRegistry.clearMockData(null));
    }

    @Test
    void testClearAllData() {
        TestService mockService = mock(TestService.class);
        ContextID context = new StringContextId(UUID.randomUUID().toString());
        
        // Add some data
        try (ContextScope scope = scopedContext(context)) {
            when(mockService, context, () -> mockService.getData()).thenReturn("test");
            mockService.getData();
        }
        
        MockRegistry.MemoryUsageStats statsBefore = MockRegistry.getMemoryUsageStats();
        assertTrue(statsBefore.getTotalMocks() > 0);
        
        MockRegistry.clearAllData();
        
        MockRegistry.MemoryUsageStats statsAfter = MockRegistry.getMemoryUsageStats();
        assertEquals(0, statsAfter.getTotalMocks());
        assertEquals(0, statsAfter.getTotalContexts());
        assertEquals(0, statsAfter.getTotalInvocations());
        assertEquals(0, statsAfter.getTotalStubbingRules());
    }

    @Test
    void testManualCleanup() {
        TestService mockService = mock(TestService.class);
        ContextID context = new StringContextId(UUID.randomUUID().toString());
        
        // Add some data
        try (ContextScope scope = scopedContext(context)) {
            when(mockService, context, () -> mockService.getData()).thenReturn("test");
            mockService.getData();
        }
        
        MockRegistry.CleanupStats stats = MockRegistry.performCleanup();
        assertNotNull(stats);
        
        // Test toString
        assertNotNull(stats.toString());
        assertTrue(stats.toString().contains("mocks="));
    }

    @Test
    void testAutoCleanupEnableDisable() {
        // Configure with auto cleanup disabled
        MockRegistry.CleanupConfiguration config = new MockRegistry.CleanupConfiguration(
            1000, 1000, 100, false
        );
        MockRegistry.setCleanupConfiguration(config);
        
        MockRegistry.enableAutoCleanup();
        // Should not start because config disables it
        
        // Configure with auto cleanup enabled
        config = new MockRegistry.CleanupConfiguration(
            1000, 1000, 100, true
        );
        MockRegistry.setCleanupConfiguration(config);
        
        MockRegistry.enableAutoCleanup();
        // Should start now
        
        MockRegistry.disableAutoCleanup();
        // Should stop
    }

    @Test
    void testAgeBasedCleanup() throws InterruptedException {
        // Configure very short max age
        MockRegistry.CleanupConfiguration config = new MockRegistry.CleanupConfiguration(
            10000, 50, 100, false // 50ms max age
        );
        MockRegistry.setCleanupConfiguration(config);
        
        TestService mockService = mock(TestService.class);
        ContextID context = new StringContextId(UUID.randomUUID().toString());
        
        // Add some data directly to the registry (bypassing scoped context cleanup)
        MockRegistry.MemoryUsageStats statsBefore;
        try {
            ContextHolder.setContext(context);
            mockService.getData();
            mockService.getData();
            mockService.getData();
            
            statsBefore = MockRegistry.getMemoryUsageStats();
        } finally {
            ContextHolder.clearContext();
        }
        
        long invocationsBefore = statsBefore.getTotalInvocations();
        
        // Wait for data to age
        Thread.sleep(100);
        
        // Perform cleanup
        MockRegistry.CleanupStats cleanupStats = MockRegistry.performCleanup();
        
        MockRegistry.MemoryUsageStats statsAfter = MockRegistry.getMemoryUsageStats();
        long invocationsAfter = statsAfter.getTotalInvocations();
        
        // Check that cleanup happened (data should be old enough to be removed)
        assertTrue(invocationsBefore > 0, "Should have had invocations before cleanup");
        assertTrue(cleanupStats.getRemovedInvocations() > 0 || invocationsAfter < invocationsBefore,
            "Expected cleanup to remove aged invocations. Before: " + invocationsBefore + 
            ", After: " + invocationsAfter + ", Removed: " + cleanupStats.getRemovedInvocations());
    }

    @Test
    void testSizeBasedCleanup() {
        // Configure very small max invocations per context
        MockRegistry.CleanupConfiguration config = new MockRegistry.CleanupConfiguration(
            2, 300000, 100, false // Max 2 invocations per context
        );
        MockRegistry.setCleanupConfiguration(config);
        
        TestService mockService = mock(TestService.class);
        ContextID context = new StringContextId(UUID.randomUUID().toString());
        
        // Add more data than the limit directly to registry
        MockRegistry.MemoryUsageStats statsBefore;
        try {
            ContextHolder.setContext(context);
            for (int i = 0; i < 10; i++) {
                mockService.getData();
            }
            
            statsBefore = MockRegistry.getMemoryUsageStats();
        } finally {
            ContextHolder.clearContext();
        }
        
        long invocationsBefore = statsBefore.getTotalInvocations();
        
        // Perform cleanup
        MockRegistry.CleanupStats cleanupStats = MockRegistry.performCleanup();
        
        MockRegistry.MemoryUsageStats statsAfter = MockRegistry.getMemoryUsageStats();
        long invocationsAfter = statsAfter.getTotalInvocations();
        
        // Check that we had data to clean up
        assertTrue(invocationsBefore > 2, "Should have had more than 2 invocations before cleanup, got: " + invocationsBefore);
        
        // Check that cleanup reduced the number of invocations to the limit
        assertTrue(cleanupStats.getRemovedInvocations() > 0 || invocationsAfter <= 2,
            "Expected cleanup to limit invocations to 2. Before: " + invocationsBefore + 
            ", After: " + invocationsAfter + ", Removed: " + cleanupStats.getRemovedInvocations());
    }

    @Test
    void testInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> {
            MockRegistry.setCleanupConfiguration(null);
        });
    }

    @Test
    void testCleanupStatsGetters() {
        MockRegistry.CleanupStats stats = new MockRegistry.CleanupStats(1, 2, 3, 4, 5);
        
        assertEquals(1, stats.getRemovedMocks());
        assertEquals(2, stats.getRemovedContexts());
        assertEquals(3, stats.getRemovedInvocations());
        assertEquals(4, stats.getRemovedStubbingRules());
        assertEquals(5, stats.getRemovedStates());
    }

    public interface TestService {
        String getData();
    }
}