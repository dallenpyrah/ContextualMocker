# ArgumentCaptors in ContextualMocker

ArgumentCaptors are a powerful feature in ContextualMocker that allow you to capture arguments passed to mocked methods during test execution. This enables detailed verification and inspection of method arguments, particularly useful when testing complex interactions.

## Table of Contents
1. [Introduction](#introduction)
2. [When to Use ArgumentCaptors](#when-to-use-argumentcaptors)
3. [Basic Usage](#basic-usage)
4. [Advanced Usage](#advanced-usage)
5. [Context-Aware Features](#context-aware-features)
6. [Comparison with Mockito](#comparison-with-mockito)
7. [Performance Considerations](#performance-considerations)
8. [Troubleshooting](#troubleshooting)
9. [Best Practices](#best-practices)

## Introduction

ArgumentCaptors provide a way to capture and inspect arguments that are passed to mocked methods. Unlike simple argument matching, captors allow you to:

- Capture the exact values passed to methods
- Perform complex assertions on captured arguments
- Verify properties of arguments that would be difficult to express with matchers
- Debug test failures by examining actual values
- Build more expressive and maintainable tests

### Basic Concept

```java
// Create a captor
ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

// Use it in verification
verify(mock, times(1), () -> mock.method(captor.capture()));

// Access the captured value
String capturedValue = captor.getValue();
```

## When to Use ArgumentCaptors

### Use ArgumentCaptors When:

1. **Verifying Complex Objects**
   ```java
   ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
   verify(orderService, times(1), () -> orderService.process(orderCaptor.capture()));
   
   Order captured = orderCaptor.getValue();
   assertEquals("PENDING", captured.getStatus());
   assertTrue(captured.getTotal() > 100.0);
   assertNotNull(captured.getOrderId());
   ```

2. **Debugging Test Failures**
   ```java
   ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);
   verify(apiClient, times(1), () -> apiClient.sendRequest(requestCaptor.capture()));
   
   // Print actual value for debugging
   System.out.println("Actual request: " + requestCaptor.getValue());
   ```

3. **Capturing Multiple Values**
   ```java
   ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
   verify(calculator, times(3), () -> calculator.add(captor.capture()));
   
   List<Integer> allValues = captor.getAllValues();
   assertEquals(Arrays.asList(1, 2, 3), allValues);
   ```

4. **Verifying Transformation Logic**
   ```java
   ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
   
   userService.createUser("john@example.com", "password123");
   
   verify(userRepository, times(1), () -> userRepository.save(savedUserCaptor.capture()));
   
   User saved = savedUserCaptor.getValue();
   assertTrue(saved.getPassword().startsWith("$2b$")); // Verify password was hashed
   ```

### Avoid ArgumentCaptors When:

1. **Simple Equality Checks** - Use `eq()` matcher instead
2. **Type Checking Only** - Use `any()` or type-specific matchers
3. **Not Interested in Value** - Use `any()` for verification only

## Basic Usage

### Creating ArgumentCaptors

```java
// For simple types
ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
ArgumentCaptor<Integer> intCaptor = ArgumentCaptor.forClass(Integer.class);

// For generic types
ArgumentCaptor<List<String>> listCaptor = ArgumentCaptor.forClass(List.class);
ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);

// For custom types
ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
```

### Capturing Single Values

```java
public interface EmailService {
    void sendEmail(String to, String subject, String body);
}

@Test
void testEmailSending() {
    EmailService emailService = mock(EmailService.class);
    ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    
    // Execute the code
    notificationService.notifyUser("user@example.com", "Welcome!");
    
    // Capture and verify
    verify(emailService, times(1), () -> 
        emailService.sendEmail(
            toCaptor.capture(), 
            subjectCaptor.capture(), 
            bodyCaptor.capture()
        )
    );
    
    // Assert on captured values
    assertEquals("user@example.com", toCaptor.getValue());
    assertEquals("Welcome to our service", subjectCaptor.getValue());
    assertTrue(bodyCaptor.getValue().contains("Welcome!"));
}
```

### Capturing Multiple Values

```java
@Test
void testBatchProcessing() {
    ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
    
    // Process multiple items
    batchProcessor.processItems(Arrays.asList("item1", "item2", "item3"));
    
    // Verify each was processed
    verify(itemService, times(3), () -> itemService.process(idCaptor.capture()));
    
    // Get all captured values
    List<String> processedIds = idCaptor.getAllValues();
    assertEquals(3, processedIds.size());
    assertEquals("item1", processedIds.get(0));
    assertEquals("item2", processedIds.get(1));
    assertEquals("item3", processedIds.get(2));
}
```

## Advanced Usage

### Capturing with Complex Matchers

```java
@Test
void testConditionalCapture() {
    ArgumentCaptor<Double> amountCaptor = ArgumentCaptor.forClass(Double.class);
    
    // Make several payments
    paymentService.charge("user1", 50.0);
    paymentService.charge("user2", 150.0);
    paymentService.charge("user3", 75.0);
    
    // Capture only amounts > 100
    verify(paymentGateway, atLeastOnce(), () -> 
        paymentGateway.processPayment(
            any(), 
            and(amountCaptor.capture(), predicate(amount -> amount > 100.0))
        )
    );
    
    // Should only capture the 150.0 payment
    assertEquals(150.0, amountCaptor.getValue(), 0.01);
}
```

### Capturing in Order

```java
@Test
void testOrderedOperations() {
    ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
    
    // Execute workflow
    workflow.start();
    workflow.process();
    workflow.complete();
    
    // Verify status updates in order
    InOrder inOrder = inOrder(statusService);
    inOrder.verify(statusService, times(1), () -> 
        statusService.updateStatus(statusCaptor.capture()));
    inOrder.verify(statusService, times(1), () -> 
        statusService.updateStatus(statusCaptor.capture()));
    inOrder.verify(statusService, times(1), () -> 
        statusService.updateStatus(statusCaptor.capture()));
    
    List<String> statuses = statusCaptor.getAllValues();
    assertEquals(Arrays.asList("STARTED", "PROCESSING", "COMPLETED"), statuses);
}
```

### Capturing with Custom Assertions

```java
@Test
void testUserCreation() {
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    
    userService.registerUser("john@example.com", "John Doe");
    
    verify(userRepository, times(1), () -> userRepository.save(userCaptor.capture()));
    
    User capturedUser = userCaptor.getValue();
    
    // Use AssertJ for fluent assertions
    assertThat(capturedUser)
        .hasFieldOrPropertyWithValue("email", "john@example.com")
        .hasFieldOrPropertyWithValue("name", "John Doe")
        .hasFieldOrProperty("id")
        .hasFieldOrProperty("createdAt")
        .matches(user -> user.getCreatedAt().isBefore(Instant.now()))
        .matches(user -> user.getId() != null);
}
```

## Context-Aware Features

### Capturing in Different Contexts

```java
@Test
void testMultiTenantCapture() {
    ContextID tenant1 = new StringContextId("tenant-1");
    ContextID tenant2 = new StringContextId("tenant-2");
    ArgumentCaptor<Config> configCaptor = ArgumentCaptor.forClass(Config.class);
    
    // Configure tenant 1
    try (ContextScope scope = scopedContext(tenant1)) {
        configService.updateConfig(new Config("theme", "dark"));
        
        scope.verify(configRepository, times(1), () -> 
            configRepository.save(configCaptor.capture()));
        
        Config tenant1Config = configCaptor.getValue();
        assertEquals("dark", tenant1Config.getValue());
    }
    
    // Configure tenant 2 with fresh captor
    configCaptor = ArgumentCaptor.forClass(Config.class);
    try (ContextScope scope = scopedContext(tenant2)) {
        configService.updateConfig(new Config("theme", "light"));
        
        scope.verify(configRepository, times(1), () -> 
            configRepository.save(configCaptor.capture()));
        
        Config tenant2Config = configCaptor.getValue();
        assertEquals("light", tenant2Config.getValue());
    }
}
```

### Thread-Safe Capturing

```java
@Test
void testConcurrentCapture() throws InterruptedException {
    ExecutorService executor = Executors.newFixedThreadPool(4);
    CountDownLatch latch = new CountDownLatch(4);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    
    // Submit concurrent tasks
    for (int i = 0; i < 4; i++) {
        final int taskId = i;
        executor.submit(() -> {
            try (ContextScope scope = scopedContext(new StringContextId("task-" + taskId))) {
                taskService.executeTask("task-" + taskId);
                latch.countDown();
            }
        });
    }
    
    latch.await();
    
    // Verify all tasks were executed
    verify(taskExecutor, times(4), () -> taskExecutor.run(captor.capture()));
    
    List<String> allTasks = captor.getAllValues();
    assertEquals(4, allTasks.size());
    assertTrue(allTasks.contains("task-0"));
    assertTrue(allTasks.contains("task-1"));
    assertTrue(allTasks.contains("task-2"));
    assertTrue(allTasks.contains("task-3"));
}
```

## Comparison with Mockito

### Similarities
- Basic API design (`ArgumentCaptor.forClass()`, `capture()`, `getValue()`)
- Type safety with generics
- Support for capturing multiple values

### Key Differences

1. **Context Awareness**
   ```java
   // ContextualMocker - Context-specific capturing
   try (ContextScope scope = scopedContext(userContext)) {
       scope.verify(service, times(1), () -> service.method(captor.capture()));
   }
   
   // Mockito - No built-in context support
   verify(service).method(captor.capture());
   ```

2. **Thread Safety**
   - ContextualMocker: Thread-safe by design, safe for concurrent tests
   - Mockito: Requires careful synchronization in concurrent scenarios

3. **Integration with Verification**
   ```java
   // ContextualMocker - Integrated with context verification
   scope.verify(service, times(1), () -> service.method(captor.capture()));
   
   // Mockito - Separate verification
   verify(service).method(captor.capture());
   ```

4. **Memory Management**
   - ContextualMocker: Automatic cleanup with context lifecycle
   - Mockito: Manual cleanup required

## Performance Considerations

### Memory Usage

ArgumentCaptors store all captured values in memory:

```java
// Be cautious with large collections
ArgumentCaptor<List<Data>> captor = ArgumentCaptor.forClass(List.class);

// This could consume significant memory if called many times
for (int i = 0; i < 10000; i++) {
    service.processLargeDataSet(generateLargeList());
}

verify(service, times(10000), () -> service.processLargeDataSet(captor.capture()));
// captor now holds 10,000 lists in memory
```

### Best Practices for Performance

1. **Create Fresh Captors**
   ```java
   @BeforeEach
   void setUp() {
       // Fresh captor for each test
       captor = ArgumentCaptor.forClass(String.class);
   }
   ```

2. **Limit Scope**
   ```java
   // Good: Captor scoped to test
   @Test
   void test() {
       ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
       // Use captor
   } // Captor eligible for GC after test
   ```

3. **Use Context Cleanup**
   ```java
   try (ContextScope scope = scopedContext(context)) {
       // Captors used here will be cleaned up with context
   }
   ```

## Troubleshooting

### Common Issues and Solutions

1. **NoSuchElementException when calling getValue()**
   ```java
   // Problem: No value was captured
   ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
   String value = captor.getValue(); // Throws exception
   
   // Solution: Verify method was called first
   verify(mock, times(1), () -> mock.method(captor.capture()));
   String value = captor.getValue(); // Now safe
   ```

2. **Getting Wrong Values**
   ```java
   // Problem: Captor reused across tests
   // Test 1 captures "value1"
   // Test 2 captures "value2"
   // captor.getAllValues() returns ["value1", "value2"]
   
   // Solution: Create fresh captor for each test
   @BeforeEach
   void setUp() {
       captor = ArgumentCaptor.forClass(String.class);
   }
   ```

3. **Type Safety Issues**
   ```java
   // Problem: Raw type usage
   ArgumentCaptor captor = ArgumentCaptor.forClass(List.class);
   List values = captor.getValue(); // No type safety
   
   // Solution: Use generics
   ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
   List<String> values = captor.getValue(); // Type safe
   ```

4. **Capturing in Wrong Context**
   ```java
   // Problem: Capturing outside context
   ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
   service.method("value"); // Called in default context
   
   try (ContextScope scope = scopedContext(context)) {
       scope.verify(service, times(1), () -> service.method(captor.capture()));
       // Fails - no invocation in this context
   }
   
   // Solution: Ensure method call and verification use same context
   try (ContextScope scope = scopedContext(context)) {
       service.method("value");
       scope.verify(service, times(1), () -> service.method(captor.capture()));
   }
   ```

## Best Practices

### 1. Use Type-Safe Captors

```java
// Good
ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
ArgumentCaptor<List<String>> listCaptor = ArgumentCaptor.forClass(List.class);

// Avoid
ArgumentCaptor captor = ArgumentCaptor.forClass(Object.class);
```

### 2. Create Fresh Captors

```java
@Test
void test1() {
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    // Use captor
}

@Test
void test2() {
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    // Fresh captor, no pollution from test1
}
```

### 3. Verify Before Accessing

```java
// Always verify first
verify(mock, times(1), () -> mock.method(captor.capture()));
// Then access
String value = captor.getValue();
```

### 4. Use Meaningful Assertions

```java
// Good: Specific assertions
ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
verify(service, times(1), () -> service.process(orderCaptor.capture()));

Order order = orderCaptor.getValue();
assertAll("Order validation",
    () -> assertEquals("PENDING", order.getStatus()),
    () -> assertTrue(order.getTotal() > 0),
    () -> assertNotNull(order.getCustomerId()),
    () -> assertEquals(3, order.getItems().size())
);

// Avoid: Just capturing without assertions
verify(service, times(1), () -> service.process(orderCaptor.capture()));
// No assertions on captured value
```

### 5. Document Complex Captures

```java
@Test
void testComplexWorkflow() {
    // Capture intermediate transformation results
    ArgumentCaptor<ProcessedData> captor = ArgumentCaptor.forClass(ProcessedData.class);
    
    // Original data goes through: validate -> transform -> store
    workflow.process(rawData);
    
    // We're capturing the data after transformation but before storage
    verify(storage, times(1), () -> storage.save(captor.capture()));
    
    ProcessedData transformed = captor.getValue();
    // Verify transformation was applied correctly
    assertNotEquals(rawData, transformed);
    assertTrue(transformed.isValidated());
}
```

### 6. Combine with Other Matchers

```java
// Capture some arguments, match others
ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);

verify(service, times(1), () -> 
    service.update(
        idCaptor.capture(),      // Capture ID for inspection
        any(),                   // Don't care about data
        eq(true)                 // Must be async=true
    )
);

String capturedId = idCaptor.getValue();
assertTrue(capturedId.matches("\\d{6}")); // Verify ID format
```

ArgumentCaptors are a powerful tool in ContextualMocker's testing arsenal. When used appropriately, they make tests more expressive, maintainable, and easier to debug. The context-aware and thread-safe implementation ensures they work seamlessly in complex, concurrent testing scenarios while maintaining the simplicity of the basic API.