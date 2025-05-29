# When and Why Use ContextualMocker

ContextualMocker is a Java mocking framework designed for scenarios where traditional mocking tools fall short—especially when your tests require precise control over mock behavior in different contexts. If your project involves complex logic that depends on execution context, or if you need to isolate mock behavior across parallel or stateful test runs, ContextualMocker provides the tools to make your tests robust, maintainable, and reliable.

## Real-World Use Cases with New APIs

### 1. Multi-Tenant Applications

**Problem**: In SaaS applications, the same code serves different tenants with different configurations and data. Traditional mocks can't easily simulate tenant-specific behavior.

**Solution with ContextualMocker**:

```java
// Define tenant contexts
ContextID tenantA = new StringContextId("tenant-corp-a");
ContextID tenantB = new StringContextId("tenant-startup-b");

@Test
void testTenantSpecificBehavior() {
    ConfigService configMock = mock(ConfigService.class);
    
    // Use scoped contexts for automatic tenant isolation
    try (ContextScope scopeA = scopedContext(tenantA)) {
        scopeA.when(configMock, () -> configMock.getFeatureFlag("advanced-analytics"))
              .thenReturn(true);  // Enterprise tenant has premium features
        
        scopeA.when(configMock, () -> configMock.getMaxUsers())
              .thenReturn(1000);
        
        // Test tenant A's behavior
        assertTrue(analyticsService.canGenerateAdvancedReports());
        scopeA.verify(configMock, times(1), () -> configMock.getFeatureFlag("advanced-analytics"));
    }
    
    try (ContextScope scopeB = scopedContext(tenantB)) {
        scopeB.when(configMock, () -> configMock.getFeatureFlag("advanced-analytics"))
              .thenReturn(false);  // Startup tenant has basic features
        
        scopeB.when(configMock, () -> configMock.getMaxUsers())
              .thenReturn(50);
        
        // Test tenant B's behavior  
        assertFalse(analyticsService.canGenerateAdvancedReports());
        scopeB.verify(configMock, times(1), () -> configMock.getFeatureFlag("advanced-analytics"));
    }
}
```

### 2. User Session Management

**Problem**: Web applications need to test user authentication, authorization, and session-specific behavior. Traditional mocks can't maintain session state across test operations.

**Solution with Stateful Mocking**:

```java
@Test
void testUserSessionWorkflow() {
    SessionService sessionMock = mock(SessionService.class);
    ContextID userSession = new StringContextId("user-session-123");
    
    enum SessionState { LOGGED_OUT, LOGGED_IN, EXPIRED }
    
    try (ContextScope scope = scopedContext(userSession)) {
        // Define stateful behavior
        scope.when(sessionMock, () -> sessionMock.login("user@company.com", "password"))
             .whenStateIs(null)  // Initially logged out
             .willSetStateTo(SessionState.LOGGED_IN)
             .thenReturn(true);
        
        scope.when(sessionMock, () -> sessionMock.getUserData())
             .whenStateIs(SessionState.LOGGED_IN)
             .thenReturn("sensitive-user-data");
        
        scope.when(sessionMock, () -> sessionMock.logout())
             .whenStateIs(SessionState.LOGGED_IN)
             .willSetStateTo(SessionState.LOGGED_OUT);
        
        // Test complete session workflow
        assertTrue(sessionMock.login("user@company.com", "password"));
        assertEquals("sensitive-user-data", sessionMock.getUserData());
        sessionMock.logout();
        assertNull(sessionMock.getUserData());  // No access after logout
        
        // Verify session interactions
        scope.verify(sessionMock, times(1), () -> sessionMock.login("user@company.com", "password"));
        scope.verify(sessionMock, times(1), () -> sessionMock.getUserData());
        scope.verify(sessionMock, times(1), () -> sessionMock.logout());
    }
}
```

### 3. Request-Scoped Services

**Problem**: Web applications often have request-scoped dependencies that behave differently based on the current HTTP request context (user permissions, request headers, etc.).

**Solution with Request Context Isolation**:

```java
@Test
void testRequestScopedBehavior() {
    AuthService authMock = mock(AuthService.class);
    AuditService auditMock = mock(AuditService.class);
    
    ContextID adminRequest = new StringContextId("request-admin-token");
    ContextID userRequest = new StringContextId("request-user-token");
    
    // Set up different behaviors per request context
    when(authMock, adminRequest, () -> authMock.hasPermission("DELETE_USERS"))
        .thenReturn(true);
    when(authMock, userRequest, () -> authMock.hasPermission("DELETE_USERS"))
        .thenReturn(false);
    
    // Test admin request
    try (ContextScope adminScope = scopedContext(adminRequest)) {
        boolean result = userController.deleteUser("user-123");
        assertTrue(result);
        
        adminScope.verify(auditMock, times(1), () -> 
            auditMock.logAction("DELETE_USER", "user-123", "admin"));
    }
    
    // Test regular user request  
    try (ContextScope userScope = scopedContext(userRequest)) {
        assertThrows(UnauthorizedException.class, () -> 
            userController.deleteUser("user-123"));
        
        userScope.verify(auditMock, never(), () -> 
            auditMock.logAction(any(), any(), any()));
    }
}
```

### 4. Parallel Test Execution

**Problem**: Running tests in parallel often leads to flaky tests due to shared mock state between test methods.

**Solution with Context Isolation**:

```java
@Test
@Execution(ExecutionMode.CONCURRENT)
void testParallelUserProcessing() throws Exception {
    UserService userMock = mock(UserService.class);
    ExecutorService executor = Executors.newFixedThreadPool(4);
    
    // Create isolated contexts for each parallel operation
    List<ContextID> userContexts = IntStream.range(1, 5)
        .mapToObj(i -> new StringContextId("user-" + i))
        .collect(Collectors.toList());
    
    // Set up different behavior for each user context
    for (int i = 0; i < userContexts.size(); i++) {
        final int userId = i + 1;
        when(userMock, userContexts.get(i), () -> userMock.getProfile("user-" + userId))
            .thenReturn("profile-data-" + userId);
    }
    
    // Execute operations in parallel
    List<Future<String>> results = new ArrayList<>();
    for (int i = 0; i < userContexts.size(); i++) {
        final ContextID context = userContexts.get(i);
        final int userId = i + 1;
        
        results.add(executor.submit(() -> {
            try (ContextScope scope = scopedContext(context)) {
                return userMock.getProfile("user-" + userId);
            }
        }));
    }
    
    // Verify isolated results
    for (int i = 0; i < results.size(); i++) {
        assertEquals("profile-data-" + (i + 1), results.get(i).get());
    }
    
    executor.shutdown();
}
```

### 5. Integration Testing with External Services

**Problem**: Integration tests need to simulate different external service responses and failure scenarios without affecting other tests.

**Solution with Builder Pattern for Complex Setup**:

```java
@Test
void testPaymentProcessingScenarios() {
    PaymentGateway paymentMock = mock(PaymentGateway.class);
    EmailService emailMock = mock(EmailService.class);
    
    ContextID successScenario = new StringContextId("payment-success");
    ContextID failureScenario = new StringContextId("payment-failure");
    
    // Use builder pattern for complex multi-service setup
    withContext(successScenario)
        .stub(paymentMock, () -> paymentMock.processPayment("card-123", 99.99))
        .thenReturn(new PaymentResult(true, "txn-456"))
        .stub(emailMock, () -> emailMock.sendConfirmation("user@email.com", "order-789"))
        .thenReturn(true)
        .verify(auditMock, never(), () -> auditMock.logFailure(any()));
    
    withContext(failureScenario)
        .stub(paymentMock, () -> paymentMock.processPayment("card-456", 199.99))
        .thenReturn(new PaymentResult(false, "insufficient-funds"))
        .stub(emailMock, () -> emailMock.sendFailureNotification("user2@email.com", "order-790"))
        .thenReturn(true);
    
    // Test success scenario
    try (ContextScope scope = scopedContext(successScenario)) {
        OrderResult result = orderService.processOrder("order-789", "card-123", 99.99);
        assertTrue(result.isSuccess());
        assertEquals("txn-456", result.getTransactionId());
    }
    
    // Test failure scenario
    try (ContextScope scope = scopedContext(failureScenario)) {
        OrderResult result = orderService.processOrder("order-790", "card-456", 199.99);
        assertFalse(result.isSuccess());
        assertEquals("insufficient-funds", result.getErrorMessage());
    }
}
```

### 6. Microservice Communication Testing

**Problem**: Testing microservice interactions requires simulating different service availability and response scenarios.

**Solution with Dynamic Behavior and TTL**:

```java
@Test
void testServiceResiliencePatterns() {
    RecommendationService recMock = mock(RecommendationService.class);
    ContextID resilientContext = new StringContextId("service-resilience");
    
    try (ContextScope scope = scopedContext(resilientContext)) {
        // Simulate service degradation with TTL
        scope.when(recMock, () -> recMock.getRecommendations("user-123"))
             .ttlMillis(100)  // Service works for 100ms
             .thenReturn(Arrays.asList("item-1", "item-2", "item-3"));
        
        // After TTL, simulate service being down
        scope.when(recMock, () -> recMock.getRecommendations("user-123"))
             .thenThrow(new ServiceUnavailableException("Recommendation service down"));
        
        // Test normal operation
        List<String> recommendations = productService.getProductRecommendations("user-123");
        assertEquals(3, recommendations.size());
        
        // Wait for service degradation
        Thread.sleep(150);
        
        // Test fallback behavior
        List<String> fallbackRecs = productService.getProductRecommendations("user-123");
        assertTrue(fallbackRecs.isEmpty() || fallbackRecs.size() < 3);  // Fallback gives fewer items
        
        // Verify circuit breaker behavior
        scope.verify(cacheMock, atLeastOnce(), () -> 
            cacheMock.get("fallback-recommendations-user-123"));
    }
}
```

### 7. Event-Driven Architecture Testing

**Problem**: Testing event-driven systems requires verifying that events are published and handled correctly in different contexts.

**Solution with Context-Aware Event Verification**:

```java
@Test
void testEventDrivenWorkflows() {
    EventPublisher eventMock = mock(EventPublisher.class);
    ContextID orderContext = new StringContextId("order-workflow");
    
    try (ContextScope scope = scopedContext(orderContext)) {
        // Stub event publishing
        scope.when(eventMock, () -> eventMock.publish(any(OrderCreatedEvent.class)))
             .thenAnswer((ctx, mock, method, args) -> {
                 OrderCreatedEvent event = (OrderCreatedEvent) args[0];
                 // Simulate async event processing
                 return CompletableFuture.completedFuture(null);
             });
        
        // Execute business logic
        Order order = orderService.createOrder("user-456", Arrays.asList("item-1", "item-2"));
        
        // Verify events were published in correct order
        scope.verify(eventMock, times(1), () -> 
            eventMock.publish(argThat(event -> 
                event instanceof OrderCreatedEvent && 
                ((OrderCreatedEvent) event).getOrderId().equals(order.getId()))));
        
        scope.verify(eventMock, times(1), () -> 
            eventMock.publish(argThat(event -> 
                event instanceof InventoryReservedEvent)));
        
        scope.verifyNoMoreInteractions(eventMock);
    }
}
```

## Typical Scenarios and Problems Solved

*   **Context-Dependent Logic:** When your code behaves differently based on context (such as user sessions, request scopes, or multi-tenant systems), ContextualMocker allows you to define and verify mock behavior specific to each context, avoiding cross-contamination between tests.
*   **Parallel and Concurrent Testing:** In environments where tests run in parallel or share resources, generic mocks can lead to flaky or brittle tests due to shared state. ContextualMocker enables context isolation, ensuring that stubbing and verification are scoped correctly, even under concurrency.
*   **Stateful and Sequential Interactions:** For systems where the sequence of interactions or the state of the mock matters, ContextualMocker provides mechanisms to capture, stub, and verify method calls with full awareness of the current context.
*   **Complex Verification Requirements:** When you need to verify that certain interactions happened only within a specific context or under certain conditions, ContextualMocker's context-aware verification modes make these assertions straightforward and reliable.

## Who Should Use ContextualMocker

*   **Teams Building Large or Complex Java Applications:** Especially those with layered architectures, microservices, or domain-driven designs where context is a first-class concern.
*   **Projects with High Concurrency or Parallelism:** Such as server-side applications, frameworks, or libraries that must be tested under concurrent conditions.
*   **Developers Needing Fine-Grained Mock Control:** If you find yourself fighting with global or static mocks, or needing to reset or reconfigure mocks between tests, ContextualMocker's context isolation will simplify your workflow.
*   **Organizations Seeking Reliable, Maintainable Tests:** By reducing test flakiness and making context explicit, ContextualMocker helps teams maintain a high-quality, trustworthy test suite.

## Unique Strengths and Differentiators

*   **Automatic Context Management:** The `scopedContext()` API prevents context leaks and ensures proper cleanup, eliminating a major source of test flakiness.
*   **Multiple API Styles:** From simple direct methods to sophisticated builder patterns, choose the right level of abstraction for each test scenario.
*   **Enhanced Error Messages:** Detailed verification failure messages with context information, invocation history, and troubleshooting tips significantly reduce debugging time.
*   **Automatic Memory Management:** Built-in cleanup policies prevent memory leaks in long-running test suites with configurable age-based and size-based strategies.
*   **Comprehensive Spy Support:** Partial mocking capabilities allow selective stubbing while preserving real behavior for legacy code integration.
*   **JUnit 5 Integration:** Seamless annotation-based dependency injection eliminates boilerplate test setup code.
*   **Extended Argument Matchers:** Rich set of matchers including string patterns, collections, ranges, and custom predicates for flexible test scenarios.
*   **Contextual Isolation:** Unlike generic mocking frameworks, ContextualMocker allows you to define, stub, and verify mocks within explicit contexts, preventing accidental leakage of behavior between tests.
*   **Advanced Stubbing and Verification:** Support for context-specific stubbing rules, stateful mocking, and sophisticated verification modes enables precise, expressive tests for complex scenarios.
*   **Concurrency-Friendly Design:** Built with parallel and concurrent test execution in mind, minimizing issues related to shared state or race conditions.
*   **Extensible and Modular:** The framework's architecture is designed for extensibility, making it suitable for advanced users and custom integrations.

## In Summary

Use ContextualMocker when you need more than just basic mocking—when your tests demand context awareness, isolation, and reliability, especially in complex or concurrent Java projects. Its unique approach to context management, combined with multiple API styles for different use cases, sets it apart from traditional mocking tools, making it an excellent choice for teams who value robust, maintainable, and precise tests.

The enhanced features (enhanced error messages, automatic memory management, comprehensive spy support, JUnit 5 integration) combined with the new APIs (`scopedContext()`, direct methods, builder patterns) make ContextualMocker not just more powerful, but also easier to use than traditional mocking frameworks for context-aware scenarios.