package com.contextualmocker.core;

import com.contextualmocker.handlers.ContextualAnswer;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

class ContextualMockBuilderTest {

    public interface TestService {
        String getData(String id);
        void updateData(String id, String data);
        int getNumber();
        boolean isEnabled();
    }

    private ContextID contextId;
    private TestService mock;

    @BeforeEach
    void setUp() {
        contextId = new StringContextId(UUID.randomUUID().toString());
        mock = ContextualMocker.mock(TestService.class);
    }

    @AfterEach
    void tearDown() {
        ContextHolder.clearContext();
    }

    @Test
    void testWithContext() {
        ContextualMockBuilder builder = ContextualMockBuilder.withContext(contextId);
        assertEquals(contextId, builder.getContextId());
    }

    @Test
    void testConstructorWithNullContext() {
        assertThrows(NullPointerException.class, () -> new ContextualMockBuilder(null));
    }

    @Test
    void testStubAndThenReturn() {
        ContextualMockBuilder builder = ContextualMockBuilder.withContext(contextId);
        
        ContextHolder.setContext(contextId);
        
        ContextualMockBuilder returnedBuilder = builder
            .stub(mock, () -> mock.getData("test"))
            .thenReturn("result");
            
        // Should return the same builder instance
        assertSame(builder, returnedBuilder);
        
        // Verify the stubbing worked
        assertEquals("result", mock.getData("test"));
    }

    @Test
    void testStubAndThenThrow() {
        ContextualMockBuilder builder = ContextualMockBuilder.withContext(contextId);
        RuntimeException expectedException = new RuntimeException("Test exception");
        
        ContextHolder.setContext(contextId);
        
        ContextualMockBuilder returnedBuilder = builder
            .stub(mock, () -> mock.getData("test"))
            .thenThrow(expectedException);
            
        assertSame(builder, returnedBuilder);
        
        RuntimeException thrownException = assertThrows(RuntimeException.class, () -> mock.getData("test"));
        assertEquals("Test exception", thrownException.getMessage());
    }

    @Test
    void testStubAndThenAnswer() {
        ContextualMockBuilder builder = ContextualMockBuilder.withContext(contextId);
        
        ContextHolder.setContext(contextId);
        
        ContextualAnswer<String> answer = (contextId, mock, method, args) -> "custom-" + args[0];
        
        ContextualMockBuilder returnedBuilder = builder
            .stub(mock, () -> mock.getData("test"))
            .thenAnswer(answer);
            
        assertSame(builder, returnedBuilder);
        
        assertEquals("custom-test", mock.getData("test"));
    }

    @Test
    void testOngoingStubbingWhenStateIs() {
        ContextualMockBuilder builder = ContextualMockBuilder.withContext(contextId);
        
        ContextHolder.setContext(contextId);
        
        // Test chaining with state conditions
        ContextualMockBuilder returnedBuilder = builder
            .stub(mock, () -> mock.getData("test"))
            .whenStateIs("enabled")
            .thenReturn("enabled-result");
            
        assertSame(builder, returnedBuilder);
        
        // Set the state and verify
        MockRegistry.setState(mock, contextId, "enabled");
        assertEquals("enabled-result", mock.getData("test"));
    }

    @Test
    void testOngoingStubbingWillSetStateTo() {
        ContextualMockBuilder builder = ContextualMockBuilder.withContext(contextId);
        
        ContextHolder.setContext(contextId);
        
        ContextualMockBuilder returnedBuilder = builder
            .stub(mock, () -> mock.getData("test"))
            .willSetStateTo("new-state")
            .thenReturn("result");
            
        assertSame(builder, returnedBuilder);
        
        assertEquals("result", mock.getData("test"));
        assertEquals("new-state", MockRegistry.getState(mock, contextId));
    }

    @Test
    void testOngoingStubbingTtlMillis() {
        ContextualMockBuilder builder = ContextualMockBuilder.withContext(contextId);
        
        ContextHolder.setContext(contextId);
        
        ContextualMockBuilder returnedBuilder = builder
            .stub(mock, () -> mock.getData("test"))
            .ttlMillis(5000)
            .thenReturn("temporary-result");
            
        assertSame(builder, returnedBuilder);
        
        assertEquals("temporary-result", mock.getData("test"));
    }

    @Test
    void testOngoingStubbingChaining() {
        ContextualMockBuilder builder = ContextualMockBuilder.withContext(contextId);
        
        ContextHolder.setContext(contextId);
        
        // Test complex chaining of all stubbing methods
        ContextualMockBuilder returnedBuilder = builder
            .stub(mock, () -> mock.getData("test"))
            .whenStateIs("active")
            .willSetStateTo("processed")
            .ttlMillis(10000)
            .thenReturn("chained-result");
            
        assertSame(builder, returnedBuilder);
        
        // Set up the required state
        MockRegistry.setState(mock, contextId, "active");
        
        assertEquals("chained-result", mock.getData("test"));
        assertEquals("processed", MockRegistry.getState(mock, contextId));
    }

    @Test
    void testVerifyWithBuilder() {
        ContextualMockBuilder builder = ContextualMockBuilder.withContext(contextId);
        
        ContextHolder.setContext(contextId);
        
        // Perform some interaction
        mock.getData("test");
        
        // Verify using builder
        ContextualMockBuilder returnedBuilder = builder.verify(
            mock, 
            ContextualMocker.times(1), 
            () -> mock.getData("test")
        );
        
        assertSame(builder, returnedBuilder);
    }

    @Test
    void testVerifyNoMoreInteractions() {
        ContextualMockBuilder builder = ContextualMockBuilder.withContext(contextId);
        
        ContextHolder.setContext(contextId);
        
        // Perform one interaction
        mock.getData("test");
        
        // Verify that interaction
        ContextualMocker.verify(mock, contextId, ContextualMocker.times(1), () -> mock.getData("test"));
        
        // Verify no more interactions using builder
        ContextualMockBuilder returnedBuilder = builder.verifyNoMoreInteractions(mock);
        
        assertSame(builder, returnedBuilder);
    }

    @Test
    void testVerifyNoInteractions() {
        ContextualMockBuilder builder = ContextualMockBuilder.withContext(contextId);
        
        ContextHolder.setContext(contextId);
        
        // Don't perform any interactions
        
        // Verify no interactions using builder
        ContextualMockBuilder returnedBuilder = builder.verifyNoInteractions(mock);
        
        assertSame(builder, returnedBuilder);
    }

    @Test
    void testGetContextId() {
        ContextualMockBuilder builder = ContextualMockBuilder.withContext(contextId);
        assertEquals(contextId, builder.getContextId());
    }

    @Test
    void testBuilderPatternFluentInterface() {
        ContextualMockBuilder builder = ContextualMockBuilder.withContext(contextId);
        
        ContextHolder.setContext(contextId);
        
        // Test a complete workflow using the fluent interface
        builder
            .stub(mock, () -> mock.getData("input1"))
            .thenReturn("output1")
            .stub(mock, () -> mock.getNumber())
            .thenReturn(42);
            
        // Verify the stubs work
        assertEquals("output1", mock.getData("input1"));
        assertEquals(42, mock.getNumber());
        
        // Now verify the interactions
        builder
            .verify(mock, ContextualMocker.times(1), () -> mock.getData("input1"))
            .verify(mock, ContextualMocker.times(1), () -> mock.getNumber());
    }

    @Test
    void testOngoingStubbingIndependentInstances() {
        ContextualMockBuilder builder = ContextualMockBuilder.withContext(contextId);
        
        ContextHolder.setContext(contextId);
        
        // Create different ongoing stubbings
        var stubbing1 = builder.stub(mock, () -> mock.getData("key1"));
        var stubbing2 = builder.stub(mock, () -> mock.getData("key2"));
        
        // They should be different instances
        assertNotSame(stubbing1, stubbing2);
        
        // Complete them differently
        stubbing1.thenReturn("value1");
        stubbing2.thenReturn("value2");
        
        assertEquals("value1", mock.getData("key1"));
        assertEquals("value2", mock.getData("key2"));
    }

    @Test
    void testOngoingStubbingReturnsSameBuilderReference() {
        ContextualMockBuilder builder = ContextualMockBuilder.withContext(contextId);
        
        ContextHolder.setContext(contextId);
        
        var ongoingStubbing = builder.stub(mock, () -> mock.getData("test"));
        
        // All completion methods should return the same builder
        ContextualMockBuilder returned1 = ongoingStubbing.thenReturn("result");
        assertSame(builder, returned1);
        
        // Test another completion method with new stubbing
        var ongoingStubbing2 = builder.stub(mock, () -> mock.getNumber());
        ContextualMockBuilder returned2 = ongoingStubbing2.thenThrow(new RuntimeException());
        assertSame(builder, returned2);
        
        // Test the third completion method with new stubbing
        var ongoingStubbing3 = builder.stub(mock, () -> mock.isEnabled());
        ContextualMockBuilder returned3 = ongoingStubbing3.thenAnswer((ctx, mock, method, args) -> true);
        assertSame(builder, returned3);
    }

    @Test
    void testOngoingStubbingChainingReturnsSelf() {
        ContextualMockBuilder builder = ContextualMockBuilder.withContext(contextId);
        
        ContextHolder.setContext(contextId);
        
        var ongoingStubbing = builder.stub(mock, () -> mock.getData("test"));
        
        // All chaining methods should return the same ongoing stubbing instance
        var returned1 = ongoingStubbing.whenStateIs("state1");
        assertSame(ongoingStubbing, returned1);
        
        var returned2 = ongoingStubbing.willSetStateTo("state2");
        assertSame(ongoingStubbing, returned2);
        
        var returned3 = ongoingStubbing.ttlMillis(1000);
        assertSame(ongoingStubbing, returned3);
    }
}