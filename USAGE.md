# ContextualMocker Usage Guide

This comprehensive guide demonstrates how to use ContextualMocker's improved APIs for efficient, thread-safe, context-aware mocking.

## Table of Contents
1. [Setup and Dependency](#1-setup-and-dependency)
2. [API Overview](#2-api-overview)
3. [Recommended API: Scoped Context Management](#3-recommended-api-scoped-context-management)
4. [Alternative APIs](#4-alternative-apis)
5. [Creating Mocks](#5-creating-mocks)
6. [Defining Context](#6-defining-context)
7. [Stubbing Examples](#7-stubbing-examples)
8. [Verification Examples](#8-verification-examples)
9. [Argument Matchers](#9-argument-matchers)
10. [Stateful Mocking](#10-stateful-mocking)
11. [Concurrency and Thread Safety](#11-concurrency-and-thread-safety)
12. [Best Practices](#12-best-practices)

## 1. Setup and Dependency

Add ContextualMocker to your Maven project:

```xml
<dependency>
    <groupId>io.github.dallenpyrah</groupId>
    <artifactId>contextual-mocker</artifactId>
    <version>1.0.0</version> <!-- Use the latest version -->
    <scope>test</scope>
</dependency>
```

Import the static methods:
```java
import static com.contextualmocker.core.ContextualMocker.*;
```

## 2. API Overview

ContextualMocker provides multiple APIs for different use cases:

| API Style | Best For | Key Benefits |
|-----------|----------|--------------|
| **Scoped Context** (Recommended) | Most cases | Automatic context management, prevents leaks |
| **Direct Methods** | Simple stubbing/verification | Reduced boilerplate, no context management |
| **Builder Pattern** | Multiple operations in same context | Efficient chaining, readable |
| **Original Fluent API** | Legacy compatibility | Compatible with existing code |

## 3. Recommended API: Scoped Context Management

**`scopedContext()` automatically manages context setup and cleanup**, preventing context leaks and ensuring proper resource management.

### Basic Example

```java
// Define your service interface
interface UserService {
    String getUserData(String userId);
    boolean updateUser(String userId, String data);
    void deleteUser(String userId);
}

// Create mock and context
UserService userService = mock(UserService.class);
ContextID tenantContext = new StringContextId("tenant-123");

// Use scoped context (RECOMMENDED)
try (ContextScope scope = scopedContext(tenantContext)) {
    // Define behavior for this tenant context
    scope.when(userService, () -> userService.getUserData("user-456"))
         .thenReturn("tenant-specific-user-data");
    
    // Use the mock - context is automatically active
    String userData = userService.getUserData("user-456");
    assertEquals("tenant-specific-user-data", userData);
    
    // Verify interactions in this context
    scope.verify(userService, times(1), () -> userService.getUserData("user-456"));
    scope.verifyNoInteractions(userService); // Verify no other calls happened
}
// Context is automatically cleaned up when scope closes
```

### Multiple Operations in Scoped Context

```java
try (ContextScope scope = scopedContext(tenantContext)) {
    // Set up multiple behaviors
    scope.when(userService, () -> userService.getUserData("user-1"))
         .thenReturn("data-1");
    scope.when(userService, () -> userService.updateUser("user-1", "new-data"))
         .thenReturn(true);
    scope.when(userService, () -> userService.deleteUser("invalid-user"))
         .thenThrow(new IllegalArgumentException("User not found"));
    
    // Execute operations
    String data = userService.getUserData("user-1");
    boolean updated = userService.updateUser("user-1", "new-data");
    
    // Verify all interactions
    scope.verify(userService, times(1), () -> userService.getUserData("user-1"));
    scope.verify(userService, times(1), () -> userService.updateUser("user-1", "new-data"));
    scope.verify(userService, never(), () -> userService.deleteUser(any()));
}
```

### Nested Context Scopes

**Scoped contexts can be nested**, allowing for complex testing scenarios:

```java
ContextID globalContext = new StringContextId("global");
ContextID userContext = new StringContextId("user-session");

try (ContextScope globalScope = scopedContext(globalContext)) {
    globalScope.when(userService, () -> userService.getUserData("admin"))
              .thenReturn("global-admin-data");
    
    // Nested scope for user-specific context
    try (ContextScope userScope = scopedContext(userContext)) {
        userScope.when(userService, () -> userService.getUserData("admin"))
                 .thenReturn("user-session-admin-data");
        
        // Inner context takes precedence
        assertEquals("user-session-admin-data", userService.getUserData("admin"));
    }
    
    // Back to global context
    assertEquals("global-admin-data", userService.getUserData("admin"));
}
```

## 4. Alternative APIs

### Direct Methods (Simple Cases)

**Direct methods bypass fluent chains** for simple stubbing and verification:

```java
ContextID context = new StringContextId("simple-context");

// Direct stubbing - no context management needed
when(userService, context, () -> userService.getUserData("user-123"))
    .thenReturn("simple-data");

// Direct verification - cleaner than fluent chains  
verifyOnce(userService, context, () -> userService.getUserData("user-123"));
verifyNever(userService, context, () -> userService.deleteUser("user-123"));
```

### Builder Pattern (Multiple Operations)

**Builder pattern is efficient for multiple operations in the same context**:

```java
// Chain multiple stubs and verifications efficiently
withContext(tenantContext)
    .stub(userService, () -> userService.getUserData("user-1")).thenReturn("data-1")
    .stub(userService, () -> userService.getUserData("user-2")).thenReturn("data-2")
    .stub(userService, () -> userService.updateUser("user-1", "new-data")).thenReturn(true)
    .verify(userService, never(), () -> userService.deleteUser(any()))
    .verifyNoMoreInteractions(userService);
```

### Convenience Methods

**Convenience methods reduce boilerplate for common patterns**:

```java
// Instead of verify(service, context, times(1), methodCall)
verifyOnce(userService, tenantContext, () -> userService.getUserData("user"));

// Instead of verify(service, context, never(), methodCall)  
verifyNever(userService, tenantContext, () -> userService.deleteUser("user"));

// Instead of verify(service, context, atLeastOnce(), methodCall)
// (Note: atLeastOnce is built-in, but you can create convenience methods for other modes)
```

## 5. Creating Mocks

ContextualMocker supports mocking both interfaces and concrete classes:

```java
// Mock interfaces (recommended)
interface PaymentService {
    boolean processPayment(String accountId, double amount);
    String getPaymentStatus(String transactionId);
}
PaymentService paymentMock = mock(PaymentService.class);

// Mock concrete classes (must be non-final, with accessible constructors)
public class EmailService {
    public void sendEmail(String recipient, String message) { }
    public boolean isEmailValid(String email) { return false; }
}
EmailService emailMock = mock(EmailService.class);

// Mock generic interfaces
interface Repository<T> {
    T findById(String id);
    void save(T entity);
}
Repository<User> userRepoMock = mock(Repository.class);
```

## 6. Defining Context

**Context IDs identify different execution contexts** (users, tenants, requests, etc.):

```java
// String-based context IDs (most common)
ContextID userContext = new StringContextId("user-12345");
ContextID tenantContext = new StringContextId("tenant-acme-corp");
ContextID requestContext = new StringContextId("request-" + UUID.randomUUID());

// You can implement custom ContextID types
public class UserContextId implements ContextID {
    private final String userId;
    private final String role;
    
    public UserContextId(String userId, String role) {
        this.userId = userId;
        this.role = role;
    }
    
    @Override
    public boolean equals(Object obj) { /* implement */ }
    
    @Override
    public int hashCode() { /* implement */ }
    
    @Override
    public String toString() { return "User{" + userId + "," + role + "}"; }
}

ContextID customContext = new UserContextId("user123", "admin");
```

## 7. Stubbing Examples

### Basic Stubbing

```java
try (ContextScope scope = scopedContext(tenantContext)) {
    // Return specific values
    scope.when(userService, () -> userService.getUserData("active-user"))
         .thenReturn("active-user-data");
    
    // Throw exceptions
    scope.when(userService, () -> userService.getUserData("invalid-user"))
         .thenThrow(new UserNotFoundException("User not found"));
    
    // Return different values for different arguments
    scope.when(userService, () -> userService.updateUser("user-1", "data"))
         .thenReturn(true);
    scope.when(userService, () -> userService.updateUser("readonly-user", "data"))
         .thenReturn(false);
}
```

### Dynamic Behavior with ContextualAnswer

```java
try (ContextScope scope = scopedContext(tenantContext)) {
    scope.when(userService, () -> userService.getUserData(any()))
         .thenAnswer((contextId, mock, method, args) -> {
             String userId = (String) args[0];
             if (userId.startsWith("admin")) {
                 return "admin-level-data";
             } else if (userId.startsWith("guest")) {
                 return "guest-level-data";
             }
             return "default-user-data";
         });
    
    assertEquals("admin-level-data", userService.getUserData("admin-123"));
    assertEquals("guest-level-data", userService.getUserData("guest-456"));
    assertEquals("default-user-data", userService.getUserData("user-789"));
}
```

### Stubbing with TTL (Time-To-Live)

```java
try (ContextScope scope = scopedContext(tenantContext)) {
    // Stub that expires after 100ms
    scope.when(userService, () -> userService.getUserData("temp-user"))
         .ttlMillis(100)
         .thenReturn("temporary-data");
    
    // Immediately available
    assertEquals("temporary-data", userService.getUserData("temp-user"));
    
    // Wait for expiration
    Thread.sleep(150);
    
    // Stub has expired, returns null (or default behavior)
    assertNull(userService.getUserData("temp-user"));
}
```

## 8. Verification Examples

### Basic Verification

```java
try (ContextScope scope = scopedContext(tenantContext)) {
    // Perform operations
    userService.getUserData("user-1");
    userService.getUserData("user-1"); // Called twice
    userService.updateUser("user-2", "new-data");
    
    // Verify exact number of calls
    scope.verify(userService, times(2), () -> userService.getUserData("user-1"));
    scope.verify(userService, times(1), () -> userService.updateUser("user-2", "new-data"));
    scope.verify(userService, never(), () -> userService.deleteUser(any()));
}
```

### Verification Modes

```java
try (ContextScope scope = scopedContext(tenantContext)) {
    // Call service multiple times
    userService.getUserData("frequent-user");
    userService.getUserData("frequent-user");
    userService.getUserData("frequent-user");
    
    // Different verification modes
    scope.verify(userService, times(3), () -> userService.getUserData("frequent-user"));
    scope.verify(userService, atLeast(2), () -> userService.getUserData("frequent-user"));
    scope.verify(userService, atMost(5), () -> userService.getUserData("frequent-user"));
    scope.verify(userService, atLeastOnce(), () -> userService.getUserData("frequent-user"));
}
```

### No Interaction Verification

```java
try (ContextScope scope = scopedContext(tenantContext)) {
    userService.getUserData("user-1");
    
    // Verify specific interaction
    scope.verify(userService, times(1), () -> userService.getUserData("user-1"));
    
    // Verify no additional interactions beyond what was already verified
    scope.verifyNoMoreInteractions(userService);
}

// Verify no interactions at all in a different context
verifyNoInteractions(userService, new StringContextId("empty-context"));
```

## 9. Argument Matchers

**Argument matchers provide flexibility** when stubbing or verifying method calls:

```java
import static com.contextualmocker.matchers.ArgumentMatchers.*;

try (ContextScope scope = scopedContext(tenantContext)) {
    // Stub with matchers
    scope.when(userService, () -> userService.getUserData(any()))
         .thenReturn("generic-user-data");
    
    scope.when(userService, () -> userService.updateUser(eq("admin"), any()))
         .thenReturn(true);
    
    scope.when(userService, () -> userService.updateUser(any(), eq("secret-data")))
         .thenThrow(new SecurityException("Cannot update secret data"));
    
    // Test stubbed behavior
    assertEquals("generic-user-data", userService.getUserData("any-user"));
    assertTrue(userService.updateUser("admin", "any-data"));
    
    assertThrows(SecurityException.class, () -> 
        userService.updateUser("regular-user", "secret-data"));
    
    // Verify with matchers
    scope.verify(userService, times(1), () -> userService.getUserData(any()));
    scope.verify(userService, times(1), () -> userService.updateUser(eq("admin"), any()));
    scope.verify(userService, never(), () -> userService.deleteUser(any()));
}
```

### Available Matchers

```java
// Generic matchers
any()           // Matches any object (including null)
eq(value)       // Matches objects equal to value

// Primitive type matchers  
anyInt()        // Matches any int
anyLong()       // Matches any long
anyDouble()     // Matches any double
anyFloat()      // Matches any float
anyBoolean()    // Matches any boolean
anyByte()       // Matches any byte
anyShort()      // Matches any short
anyChar()       // Matches any char
```

## 10. Stateful Mocking

**Stateful mocking allows mock behavior to change based on internal state** within each context:

```java
// Define a stateful interface
interface SessionService {
    boolean login(String username, String password);
    String getSecretData();
    void logout();
    boolean isLoggedIn();
}

SessionService sessionMock = mock(SessionService.class);
ContextID sessionContext = new StringContextId("session-abc123");

// Define states (can be any Object)
enum SessionState { LOGGED_OUT, LOGGED_IN, EXPIRED }

try (ContextScope scope = scopedContext(sessionContext)) {
    // Login transitions from null state to LOGGED_IN
    scope.when(sessionMock, () -> sessionMock.login("user", "pass"))
         .whenStateIs(null)  // Only when not logged in
         .willSetStateTo(SessionState.LOGGED_IN)
         .thenReturn(true);
    
    // Invalid login doesn't change state
    scope.when(sessionMock, () -> sessionMock.login("user", "wrong"))
         .whenStateIs(null)
         .thenReturn(false);  // No state change
    
    // Secret data only available when logged in
    scope.when(sessionMock, () -> sessionMock.getSecretData())
         .whenStateIs(SessionState.LOGGED_IN)
         .thenReturn("top-secret-information");
    
    // Logout transitions to LOGGED_OUT
    scope.when(sessionMock, () -> sessionMock.logout())
         .whenStateIs(SessionState.LOGGED_IN)
         .willSetStateTo(SessionState.LOGGED_OUT);
    
    // State query
    scope.when(sessionMock, () -> sessionMock.isLoggedIn())
         .whenStateIs(SessionState.LOGGED_IN)
         .thenReturn(true);
    scope.when(sessionMock, () -> sessionMock.isLoggedIn())
         .whenStateIs(SessionState.LOGGED_OUT)
         .thenReturn(false);
    
    // Test the stateful behavior
    assertFalse(sessionMock.isLoggedIn());  // Initially logged out
    assertNull(sessionMock.getSecretData()); // No access to secret data
    
    assertTrue(sessionMock.login("user", "pass")); // Login succeeds
    assertTrue(sessionMock.isLoggedIn());   // Now logged in
    assertEquals("top-secret-information", sessionMock.getSecretData());
    
    sessionMock.logout();  // Logout
    assertFalse(sessionMock.isLoggedIn());  // Logged out again
    assertNull(sessionMock.getSecretData()); // No more access to secret data
}
```

### Complex State Transitions

```java
// Shopping cart example with complex state management
interface ShoppingCart {
    void addItem(String item, int quantity);
    boolean removeItem(String item);
    double calculateTotal();
    boolean checkout(String paymentMethod);
    void clear();
}

enum CartState { EMPTY, HAS_ITEMS, CHECKED_OUT }

try (ContextScope scope = scopedContext(cartContext)) {
    // Adding first item transitions from EMPTY to HAS_ITEMS
    scope.when(cartMock, () -> cartMock.addItem(any(), anyInt()))
         .whenStateIs(null)  // Start with empty cart
         .willSetStateTo(CartState.HAS_ITEMS);
    
    // Calculate total when cart has items
    scope.when(cartMock, () -> cartMock.calculateTotal())
         .whenStateIs(CartState.HAS_ITEMS)
         .thenAnswer((ctx, mock, method, args) -> {
             // Simulate calculation based on items
             return 29.99;
         });
    
    // Checkout transitions to CHECKED_OUT
    scope.when(cartMock, () -> cartMock.checkout(any()))
         .whenStateIs(CartState.HAS_ITEMS)
         .willSetStateTo(CartState.CHECKED_OUT)
         .thenReturn(true);
    
    // Clear cart resets to null state
    scope.when(cartMock, () -> cartMock.clear())
         .willSetStateTo(null);  // Works from any state
    
    // Test state transitions
    cartMock.addItem("laptop", 1);  // EMPTY -> HAS_ITEMS
    assertEquals(29.99, cartMock.calculateTotal(), 0.01);
    assertTrue(cartMock.checkout("credit-card"));  // HAS_ITEMS -> CHECKED_OUT
    cartMock.clear();  // CHECKED_OUT -> null
}
```

## 11. Concurrency and Thread Safety

**ContextualMocker is designed for thread safety** and concurrent test execution:

### Concurrent Context Isolation

```java
ExecutorService executor = Executors.newFixedThreadPool(4);
List<Future<String>> results = new ArrayList<>();

// Different contexts for concurrent execution
ContextID[] contexts = {
    new StringContextId("user-1"),
    new StringContextId("user-2"), 
    new StringContextId("user-3"),
    new StringContextId("user-4")
};

// Set up different behavior for each context
for (int i = 0; i < contexts.length; i++) {
    final int userId = i + 1;
    when(userService, contexts[i], () -> userService.getUserData("profile"))
        .thenReturn("user-" + userId + "-profile-data");
}

// Execute concurrently - each thread uses different context
for (int i = 0; i < contexts.length; i++) {
    final ContextID context = contexts[i];
    final int expectedUserId = i + 1;
    
    results.add(executor.submit(() -> {
        try (ContextScope scope = scopedContext(context)) {
            String result = userService.getUserData("profile");
            return result;
        }
    }));
}

// Verify isolated results
for (int i = 0; i < results.size(); i++) {
    String expected = "user-" + (i + 1) + "-profile-data";
    assertEquals(expected, results.get(i).get());
}

executor.shutdown();
```

### Thread-Safe Stateful Mocking

```java
// Multiple threads manipulating state independently
ContextID sharedServiceContext = new StringContextId("shared-service");
CountDownLatch latch = new CountDownLatch(3);

// Set up stateful behavior
try (ContextScope scope = scopedContext(sharedServiceContext)) {
    scope.when(counterService, () -> counterService.increment())
         .thenAnswer((ctx, mock, method, args) -> {
             // Thread-safe state management is handled internally
             return getCurrentCount() + 1;
         });
}

// Multiple threads incrementing concurrently
for (int i = 0; i < 3; i++) {
    executor.submit(() -> {
        try (ContextScope scope = scopedContext(sharedServiceContext)) {
            counterService.increment();
            latch.countDown();
        }
    });
}

latch.await();
// Verify all interactions were recorded safely
verifyTimes(counterService, sharedServiceContext, times(3), 
           () -> counterService.increment());
```

## 12. Best Practices

### Context Management Best Practices

```java
// ✅ GOOD: Use scopedContext() for automatic management
try (ContextScope scope = scopedContext(contextId)) {
    // All operations automatically use the correct context
    scope.when(service, () -> service.method()).thenReturn(value);
    assertEquals(value, service.method());
}

// ❌ AVOID: Manual context management (error-prone)
ContextHolder.setContext(contextId);  
try {
    // Easy to forget cleanup or have exceptions
    service.method();
} finally {
    ContextHolder.clearContext();  // Manual cleanup required
}
```

### Stubbing Best Practices

```java
// ✅ GOOD: Specific, readable stubs
try (ContextScope scope = scopedContext(userContext)) {
    scope.when(userService, () -> userService.getUserByEmail("admin@company.com"))
         .thenReturn(new User("admin", "admin@company.com", Role.ADMIN));
    
    scope.when(userService, () -> userService.getUserByEmail("user@company.com"))
         .thenReturn(new User("user", "user@company.com", Role.USER));
}

// ❌ AVOID: Overly generic stubs that hide test intent
try (ContextScope scope = scopedContext(userContext)) {
    scope.when(userService, () -> userService.getUserByEmail(any()))
         .thenReturn(someGenericUser);  // Unclear what's being tested
}
```

### Verification Best Practices

```java
// ✅ GOOD: Verify specific, meaningful interactions
try (ContextScope scope = scopedContext(orderContext)) {
    orderService.processOrder("order-123");
    
    scope.verify(paymentService, times(1), () -> 
        paymentService.processPayment("order-123", 99.99));
    scope.verify(emailService, times(1), () -> 
        emailService.sendConfirmation("user@email.com", "order-123"));
    scope.verifyNoMoreInteractions(paymentService);
}

// ❌ AVOID: Over-verification or under-verification
try (ContextScope scope = scopedContext(orderContext)) {
    orderService.processOrder("order-123");
    
    // Over-verification: checking internal implementation details
    scope.verify(logger, times(5), () -> logger.debug(any()));
    
    // Under-verification: missing important side effects
    // Should verify payment and email were called
}
```

### Testing Complex Scenarios

```java
@Test
void testMultiTenantOrderProcessing() {
    // Create contexts for different tenants
    ContextID tenantA = new StringContextId("tenant-a");
    ContextID tenantB = new StringContextId("tenant-b");
    
    // Set up tenant-specific behavior
    when(configService, tenantA, () -> configService.getTaxRate())
        .thenReturn(0.08);  // 8% tax for tenant A
    when(configService, tenantB, () -> configService.getTaxRate())
        .thenReturn(0.05);  // 5% tax for tenant B
    
    // Test tenant A
    try (ContextScope scopeA = scopedContext(tenantA)) {
        Order order = new Order("item", 100.00);
        orderService.processOrder(order);
        
        scopeA.verify(taxService, times(1), () -> 
            taxService.calculateTax(100.00, 0.08));
    }
    
    // Test tenant B  
    try (ContextScope scopeB = scopedContext(tenantB)) {
        Order order = new Order("item", 100.00);
        orderService.processOrder(order);
        
        scopeB.verify(taxService, times(1), () -> 
            taxService.calculateTax(100.00, 0.05));
    }
    
    // Verify tenant isolation
    verifyNoInteractions(taxService, new StringContextId("tenant-c"));
}
```

This comprehensive guide covers all the major features and patterns for using ContextualMocker effectively. The recommended `scopedContext()` approach provides the best balance of safety, readability, and ease of use for most testing scenarios.