package com.contextualmocker.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.contextualmocker.core.ContextualMocker.*;
import static org.junit.jupiter.api.Assertions.*;

class BasicSpyTest {

    public static class SimpleService {
        public String getData() {
            return "real data";
        }
        
        public String externalCall() {
            return "real external call";
        }
        
        public int getNumber() {
            return 42;
        }
    }

    private ContextID testContext;

    @BeforeEach
    void setUp() {
        testContext = new StringContextId(UUID.randomUUID().toString());
        ContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        ContextHolder.clearContext();
    }

    @Test
    void spyBasicFunctionality() {
        SimpleService realService = new SimpleService();
        SimpleService spy = spy(realService);

        try (ContextScope scope = scopedContext(testContext)) {
            // Without stubbing, should delegate to real object
            assertEquals("real data", spy.getData());
            assertEquals(42, spy.getNumber());
            
            // Verify interactions were recorded
            scope.verify(spy, times(1), () -> spy.getData());
            scope.verify(spy, times(1), () -> spy.getNumber());
        }
    }

}