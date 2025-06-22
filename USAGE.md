# ContextualMocker Usage Guide

This comprehensive guide demonstrates how to use ContextualMocker's improved APIs for efficient, thread-safe, context-aware mocking.

## Table of Contents
1. [Setup and Dependency](#1-setup-and-dependency)
2. [API Overview](#2-api-overview)
3. [Recommended API: Scoped Context Management](#3-recommended-api-scoped-context-management)
4. [Alternative APIs](#4-alternative-apis)
5. [Creating Mocks](#5-creating-mocks)
6. [Spy Support (Partial Mocking)](#6-spy-support-partial-mocking)
7. [JUnit 5 Integration](#7-junit-5-integration)
8. [Defining Context](#8-defining-context)
9. [Stubbing Examples](#9-stubbing-examples)
10. [Verification Examples](#10-verification-examples)
11. [Enhanced Error Messages](#11-enhanced-error-messages)
12. [Argument Matchers](#12-argument-matchers)
13. [ArgumentCaptors](#13-argumentcaptors)
14. [Memory Management](#14-memory-management)
15. [Stateful Mocking](#15-stateful-mocking)
16. [Concurrency and Thread Safety](#16-concurrency-and-thread-safety)
17. [Best Practices](#17-best-practices)

## 1. Setup and Dependency

Add ContextualMocker to your Maven project:

```xml
<dependency>
    <groupId>io.github.dallenpyrah</groupId>
    <artifactId>contextual-mocker</artifactId>
    <version>1.1.0</version> <!-- Use the latest version -->
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

## 6. Spy Support (Partial Mocking)

**Spies wrap real objects** allowing selective stubbing while delegating unstubbed methods to the real implementation:

```java
// Create a real service instance
class UserServiceImpl implements UserService {
    public String getUserData(String userId) {
        return "real-data-for-" + userId;
    }
    
    public String externalApiCall(String request) {
        // This makes actual network calls - we want to stub this
        return "external-api-response";
    }
    
    public boolean validateUser(String userId) {
        // Real validation logic we want to keep
        return userId != null && userId.length() > 0;
    }
}

UserService realService = new UserServiceImpl();
UserService spy = spy(realService);

try (ContextScope scope = scopedContext(context)) {
    // Stub only the external API call
    scope.when(spy, () -> spy.externalApiCall("test-request"))
         .thenReturn("mocked-external-response");
    
    // Real methods still work normally
    assertTrue(spy.validateUser("user123"));  // Real implementation
    assertEquals("real-data-for-user123", spy.getUserData("user123"));  // Real implementation
    
    // Stubbed method returns mocked value
    assertEquals("mocked-external-response", spy.externalApiCall("test-request"));
    
    // Verify interactions on both real and stubbed methods
    scope.verify(spy, times(1), () -> spy.validateUser("user123"));
    scope.verify(spy, times(1), () -> spy.externalApiCall("test-request"));
}
```

### Spy Use Cases

```java
// Legacy system integration
class LegacyOrderService {
    public Order processOrder(Order order) {
        // Complex legacy logic we don't want to mock completely
        order.setStatus("processed");
        return order;
    }
    
    public void sendNotification(String email) {
        // External service we want to stub
        throw new RuntimeException("Would send real email");
    }
}

LegacyOrderService legacyService = new LegacyOrderService();
LegacyOrderService spy = spy(legacyService);

try (ContextScope scope = scopedContext(context)) {
    // Stub only the external notification
    scope.when(spy, () -> spy.sendNotification(any()))
         .thenReturn(null);  // Void method stubbing
    
    Order order = new Order("item", 100.0);
    Order result = spy.processOrder(order);  // Real processing
    spy.sendNotification("user@email.com");  // Stubbed
    
    assertEquals("processed", result.getStatus());  // Real logic worked
    scope.verify(spy, times(1), () -> spy.sendNotification("user@email.com"));
}
```

### Spy Limitations

- Cannot spy on **final classes** or **interfaces**
- Cannot stub **final methods** or **private methods** 
- Constructor calls during spy creation may have side effects

## 7. JUnit 5 Integration

**Automatic dependency injection** with annotations eliminates boilerplate setup:

```java
@ExtendWith(ContextualMockerExtension.class)
class UserServiceTest {
    
    @Mock UserRepository userRepository;
    @Mock EmailService emailService;
    @Spy UserValidationService validationService;  // Uses real instance with selective stubbing
    @ContextId("test-context") ContextID testContext;
    
    // Mocks and context are automatically injected before each test
    
    @Test
    void testUserCreation() {
        // No manual setup needed - mocks are ready to use
        try (ContextScope scope = scopedContext(testContext)) {
            scope.when(userRepository, () -> userRepository.existsByEmail("test@email.com"))
                 .thenReturn(false);
            
            scope.when(emailService, () -> emailService.sendWelcomeEmail(any()))
                 .thenReturn(true);
            
            UserService userService = new UserService(userRepository, emailService, validationService);
            User user = userService.createUser("test@email.com", "password");
            
            assertNotNull(user);
            scope.verify(userRepository, times(1), () -> userRepository.save(any()));
            scope.verify(emailService, times(1), () -> emailService.sendWelcomeEmail(any()));
        }
    }
    
    @Test
    void testUserValidation() {
        // Same mocks automatically available in each test
        try (ContextScope scope = scopedContext(testContext)) {
            // Real validation logic (spy) + stubbed dependencies
            assertTrue(validationService.isValidEmail("test@email.com"));
        }
    }
}
```

### Custom Context IDs

```java
@ExtendWith(ContextualMockerExtension.class)
class MultiContextTest {
    
    @Mock UserService userService;
    @ContextId("admin-context") ContextID adminContext;
    @ContextId("user-context") ContextID userContext;
    @ContextId ContextID dynamicContext;  // Will be generated automatically
    
    @Test
    void testDifferentContexts() {
        // All contexts are available and isolated
        when(userService, adminContext, () -> userService.getRole()).thenReturn("admin");
        when(userService, userContext, () -> userService.getRole()).thenReturn("user");
        
        try (ContextScope scope = scopedContext(adminContext)) {
            assertEquals("admin", userService.getRole());
        }
        
        try (ContextScope scope = scopedContext(userContext)) {
            assertEquals("user", userService.getRole());
        }
    }
}
```

## 8. Defining Context

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

## 11. Enhanced Error Messages

**ContextualMocker provides detailed verification failure messages** with context information and troubleshooting tips:

### Basic Error Message Example

When a verification fails, you get comprehensive information:

```java
try (ContextScope scope = scopedContext(userContext)) {
    // Call method twice
    userService.getUserData("user123");
    userService.getUserData("user123");
    
    // This will fail and show enhanced error message
    scope.verify(userService, times(1), () -> userService.getUserData("user123"));
}
```

**Enhanced error output:**
```
================================================================================
VERIFICATION FAILURE
================================================================================
Expected: exactly 1 time (1 invocations)
Actual:   2 invocations

Target Method:
  UserService.getUserData(String)

Expected Arguments:
  "user123"

Context:
  StringContextId{id='user-context'}

Mock Object:
  UserService@a1b2c3d4

Actual Invocations (2):
  1. getUserData("user123") at 2023-12-01T10:30:15.123Z
  2. getUserData("user123") at 2023-12-01T10:30:15.125Z

Troubleshooting Tips:
  - Look for unexpected additional method calls
  - Check if the method is called multiple times unintentionally
================================================================================
```

### No Invocations Error Example

```java
try (ContextScope scope = scopedContext(userContext)) {
    // Forget to call the method
    
    // This will fail with helpful guidance
    scope.verify(userService, times(1), () -> userService.getUserData("user123"));
}
```

**Enhanced error output:**
```
================================================================================
VERIFICATION FAILURE
================================================================================
Expected: exactly 1 time (1 invocations)
Actual:   0 invocations

Target Method:
  UserService.getUserData(String)

Expected Arguments:
  "user123"

Context:
  StringContextId{id='user-context'}

Actual Invocations:
  NO INVOCATIONS RECORDED
  - Check if the method was actually called
  - Verify the correct context is set
  - Ensure the mock object is being used

Troubleshooting Tips:
  - Ensure the method is called on the correct mock instance
  - Check that the context is properly set before the method call
  - Verify argument matchers are correct
================================================================================
```

### Error Message Benefits

- **Detailed Context**: Shows exact mock object, context ID, and target method
- **Argument Information**: Displays expected vs actual arguments with proper formatting
- **Invocation History**: Lists all actual invocations with timestamps
- **Troubleshooting Tips**: Provides specific guidance based on the failure type
- **Clear Formatting**: Easy-to-read structure with visual separators

## 12. Argument Matchers

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
isNull()        // Matches null values
notNull()       // Matches non-null values

// Primitive type matchers  
anyInt()        // Matches any int
anyLong()       // Matches any long
anyDouble()     // Matches any double
anyFloat()      // Matches any float
anyBoolean()    // Matches any boolean
anyByte()       // Matches any byte
anyShort()      // Matches any short
anyChar()       // Matches any char

// String matchers
anyString()     // Matches any string
contains("text") // Matches strings containing "text"
startsWith("prefix") // Matches strings starting with "prefix"
endsWith("suffix")   // Matches strings ending with "suffix"
regex("pattern")     // Matches strings matching the regex pattern

// Collection and range matchers
anyCollection() // Matches any collection
anyList()       // Matches any list
anySet()        // Matches any set
anyMap()        // Matches any map
range(min, max) // Matches numbers in range [min, max]

// Custom matchers
predicate(lambda) // Matches objects satisfying the predicate
argThat(matcher)  // Matches objects satisfying custom ArgumentMatcher
```

### Advanced Matcher Examples

```java
try (ContextScope scope = scopedContext(userContext)) {
    // String matching
    scope.when(userService, () -> userService.findUser(contains("admin")))
         .thenReturn(adminUser);
    scope.when(userService, () -> userService.findUser(startsWith("guest_")))
         .thenReturn(guestUser);
    scope.when(userService, () -> userService.findUser(regex("user-\\d+")))
         .thenReturn(regularUser);
    
    // Custom predicate matching
    scope.when(paymentService, () -> paymentService.processPayment(
            any(), 
            predicate(amount -> amount > 100.0)))
         .thenReturn(false); // Reject large payments
    
    // Range matching for numbers
    scope.when(discountService, () -> discountService.calculateDiscount(range(50.0, 200.0)))
         .thenReturn(0.1); // 10% discount for amounts in range
    
    // Custom argument matcher
    scope.when(userService, () -> userService.updateUser(
            argThat(user -> user.getRole() == Role.ADMIN)))
         .thenReturn(true);
    
    // Verify with advanced matchers
    scope.verify(userService, times(1), () -> 
        userService.findUser(contains("admin")));
    scope.verify(paymentService, never(), () -> 
        paymentService.processPayment(any(), predicate(amount -> amount < 0)));
}
```

## 13. ArgumentCaptors

**ArgumentCaptors allow you to capture arguments** passed to mocked methods for detailed verification and inspection. This is especially useful when you need to verify complex objects or validate specific properties of arguments.

### Basic ArgumentCaptor Usage

```java
import static com.contextualmocker.matchers.ArgumentMatchers.*;
import com.contextualmocker.captors.ArgumentCaptor;

// Create a captor for the argument type
ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

try (ContextScope scope = scopedContext(context)) {
    // Execute code that calls the mock
    userService.createUser("john.doe@example.com", "John Doe");
    
    // Capture the argument during verification
    scope.verify(userService, times(1), () -> 
        userService.createUser(stringCaptor.capture(), any()));
    
    // Access the captured value
    String capturedEmail = stringCaptor.getValue();
    assertEquals("john.doe@example.com", capturedEmail);
    assertTrue(capturedEmail.contains("@"));
}
```

### Capturing Multiple Arguments

```java
ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

try (ContextScope scope = scopedContext(context)) {
    // Multiple method calls
    userService.createUser("user1@example.com", "User One");
    userService.createUser("user2@example.com", "User Two");
    userService.createUser("user3@example.com", "User Three");
    
    // Capture all invocations
    scope.verify(userService, times(3), () -> 
        userService.createUser(emailCaptor.capture(), nameCaptor.capture()));
    
    // Get all captured values
    List<String> allEmails = emailCaptor.getAllValues();
    List<String> allNames = nameCaptor.getAllValues();
    
    assertEquals(3, allEmails.size());
    assertEquals(Arrays.asList("user1@example.com", "user2@example.com", "user3@example.com"), allEmails);
    assertEquals(Arrays.asList("User One", "User Two", "User Three"), allNames);
    
    // Get specific captured values
    assertEquals("user2@example.com", emailCaptor.getAllValues().get(1));
    assertEquals("User Two", nameCaptor.getAllValues().get(1));
}
```

### Capturing Complex Objects

```java
public class Order {
    private String id;
    private List<OrderItem> items;
    private double total;
    private OrderStatus status;
    // getters and setters...
}

ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

try (ContextScope scope = scopedContext(context)) {
    // Process an order
    orderService.processOrder(new Order("ORD-123", items, 99.99, OrderStatus.PENDING));
    
    // Capture and inspect the order
    scope.verify(paymentService, times(1), () -> 
        paymentService.chargePayment(orderCaptor.capture()));
    
    Order capturedOrder = orderCaptor.getValue();
    assertEquals("ORD-123", capturedOrder.getId());
    assertEquals(99.99, capturedOrder.getTotal(), 0.01);
    assertEquals(OrderStatus.PENDING, capturedOrder.getStatus());
    assertFalse(capturedOrder.getItems().isEmpty());
}
```

### Context-Aware Capturing

**ArgumentCaptors work seamlessly with ContextualMocker's context system**:

```java
ContextID userContext = new StringContextId("user-123");
ContextID adminContext = new StringContextId("admin-456");
ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);

// Different behavior for different contexts
when(auditService, userContext, () -> auditService.logAction(any()))
    .thenReturn(true);
when(auditService, adminContext, () -> auditService.logAction(any()))
    .thenReturn(true);

// User context actions
try (ContextScope scope = scopedContext(userContext)) {
    auditService.logAction("view_profile");
    auditService.logAction("update_profile");
    
    scope.verify(auditService, times(2), () -> 
        auditService.logAction(actionCaptor.capture()));
    
    List<String> userActions = actionCaptor.getAllValues();
    assertEquals(Arrays.asList("view_profile", "update_profile"), userActions);
}

// Admin context actions - separate capture
actionCaptor = ArgumentCaptor.forClass(String.class); // New captor for clean state
try (ContextScope scope = scopedContext(adminContext)) {
    auditService.logAction("delete_user");
    auditService.logAction("grant_permission");
    
    scope.verify(auditService, times(2), () -> 
        auditService.logAction(actionCaptor.capture()));
    
    List<String> adminActions = actionCaptor.getAllValues();
    assertEquals(Arrays.asList("delete_user", "grant_permission"), adminActions);
}
```

### Combining Captors with Matchers

```java
ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

try (ContextScope scope = scopedContext(context)) {
    Map<String, Object> userData = new HashMap<>();
    userData.put("name", "John");
    userData.put("age", 30);
    
    userService.updateUserData("user-123", userData);
    
    // Mix captors with regular matchers
    scope.verify(userService, times(1), () -> 
        userService.updateUserData(
            userIdCaptor.capture(),  // Capture first argument
            dataCaptor.capture()     // Capture second argument
        ));
    
    // Verify captured values
    assertEquals("user-123", userIdCaptor.getValue());
    Map<String, Object> capturedData = dataCaptor.getValue();
    assertEquals("John", capturedData.get("name"));
    assertEquals(30, capturedData.get("age"));
}
```

### Advanced Capture Patterns

```java
// Capture only specific invocations using conditional verification
ArgumentCaptor<Double> amountCaptor = ArgumentCaptor.forClass(Double.class);

try (ContextScope scope = scopedContext(context)) {
    // Multiple payment attempts
    paymentService.processPayment("user-1", 50.0);
    paymentService.processPayment("user-2", 150.0);
    paymentService.processPayment("user-3", 75.0);
    paymentService.processPayment("user-4", 200.0);
    
    // Capture only large payments (> 100)
    scope.verify(paymentService, atLeast(2), () -> 
        paymentService.processPayment(
            any(), 
            and(amountCaptor.capture(), predicate(amount -> amount > 100.0))
        ));
    
    List<Double> largePayments = amountCaptor.getAllValues();
    assertEquals(2, largePayments.size());
    assertTrue(largePayments.contains(150.0));
    assertTrue(largePayments.contains(200.0));
}
```

### Capture and Stubbing Together

```java
ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);

try (ContextScope scope = scopedContext(context)) {
    // Stub to return different responses based on input
    scope.when(apiService, () -> apiService.makeRequest(any()))
         .thenAnswer((ctx, mock, method, args) -> {
             String request = (String) args[0];
             return "Response for: " + request;
         });
    
    // Make several API calls
    String response1 = apiService.makeRequest("GET /users");
    String response2 = apiService.makeRequest("POST /orders");
    
    // Capture and verify all requests
    scope.verify(apiService, times(2), () -> 
        apiService.makeRequest(requestCaptor.capture()));
    
    List<String> allRequests = requestCaptor.getAllValues();
    assertEquals("GET /users", allRequests.get(0));
    assertEquals("POST /orders", allRequests.get(1));
    
    // Verify responses
    assertEquals("Response for: GET /users", response1);
    assertEquals("Response for: POST /orders", response2);
}
```

### Best Practices for ArgumentCaptors

1. **Create fresh captors for each test** to avoid state pollution:
```java
@BeforeEach
void setUp() {
    // Don't reuse captors between tests
    stringCaptor = ArgumentCaptor.forClass(String.class);
}
```

2. **Use type-safe captors** with proper generics:
```java
// Good: Type-safe
ArgumentCaptor<List<String>> listCaptor = ArgumentCaptor.forClass(List.class);

// Avoid: Raw types
ArgumentCaptor listCaptor = ArgumentCaptor.forClass(List.class);
```

3. **Verify before capturing** to ensure the method was actually called:
```java
// The verify call will fail if method wasn't called, preventing NPE
scope.verify(service, times(1), () -> service.method(captor.capture()));
String value = captor.getValue(); // Safe to access
```

4. **Use getAllValues() for multiple invocations**:
```java
// For single invocation
String singleValue = captor.getValue();

// For multiple invocations
List<String> allValues = captor.getAllValues();
```

5. **Combine with custom assertions** for complex verification:
```java
ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
scope.verify(service, times(1), () -> service.createUser(userCaptor.capture()));

User capturedUser = userCaptor.getValue();
assertAll("User validation",
    () -> assertNotNull(capturedUser.getId()),
    () -> assertTrue(capturedUser.getEmail().contains("@")),
    () -> assertTrue(capturedUser.getCreatedAt().isBefore(Instant.now()))
);
```

## 14. Memory Management

**ContextualMocker provides automatic memory management** to prevent memory leaks in long-running test suites:

### Default Configuration

Memory management is enabled by default with sensible defaults:

```java
// Default settings (applied automatically)
CleanupConfiguration defaultConfig = CleanupConfiguration.defaultConfig();
// - Max 10,000 invocations per context
// - 5 minutes max age for invocation records  
// - Cleanup every minute
// - Auto cleanup enabled
```

### Custom Memory Management Configuration

```java
// Configure custom cleanup policies
CleanupConfiguration customConfig = new CleanupConfiguration(
    5000,    // Max 5,000 invocations per context
    120000,  // 2 minutes max age (in milliseconds)
    30000,   // Cleanup every 30 seconds
    true     // Auto cleanup enabled
);

MockRegistry.setCleanupConfiguration(customConfig);
```

### Memory Usage Monitoring

```java
// Get current memory usage statistics
MemoryUsageStats stats = MockRegistry.getMemoryUsageStats();

System.out.println("Total mocks: " + stats.getTotalMocks());
System.out.println("Total contexts: " + stats.getTotalContexts());  
System.out.println("Total invocations: " + stats.getTotalInvocations());
System.out.println("Total stubbing rules: " + stats.getTotalStubbingRules());
System.out.println("Total state objects: " + stats.getTotalStates());

// Example output:
// Total mocks: 25
// Total contexts: 12
// Total invocations: 1,247
// Total stubbing rules: 89
// Total state objects: 5
```

### Manual Cleanup Operations

```java
// Perform immediate cleanup
CleanupStats cleanupResult = MockRegistry.performCleanup();
System.out.println("Cleanup completed: " + cleanupResult);
// Output: CleanupStats{mocks=2, contexts=5, invocations=150, rules=8, states=1}

// Clear all data for a specific mock
boolean wasCleared = MockRegistry.clearMockData(specificMock);

// Clear all data (use with caution!)
MockRegistry.clearAllData();

// Enable/disable automatic cleanup
MockRegistry.enableAutoCleanup();
MockRegistry.disableAutoCleanup();
```

### Memory Management in Tests

```java
@Test
void testWithMemoryManagement() {
    // Configure aggressive cleanup for this test
    CleanupConfiguration testConfig = new CleanupConfiguration(
        100,   // Very low limit
        5000,  // 5 second age limit  
        1000,  // Cleanup every second
        true
    );
    
    CleanupConfiguration originalConfig = MockRegistry.getCleanupConfiguration();
    MockRegistry.setCleanupConfiguration(testConfig);
    
    try {
        // Run memory-intensive test operations
        for (int i = 0; i < 1000; i++) {
            try (ContextScope scope = scopedContext(new StringContextId("test-" + i))) {
                mockService.performOperation();
                scope.verify(mockService, times(1), () -> mockService.performOperation());
            }
        }
        
        // Memory should be managed automatically
        MemoryUsageStats stats = MockRegistry.getMemoryUsageStats();
        assertTrue(stats.getTotalInvocations() < 500); // Aggressive cleanup prevented buildup
        
    } finally {
        // Restore original configuration
        MockRegistry.setCleanupConfiguration(originalConfig);
    }
}
```

### Memory Management Benefits

- **Prevents Memory Leaks**: Automatic cleanup removes old data
- **Configurable Policies**: Age-based and size-based cleanup strategies
- **Low Overhead**: Background cleanup doesn't impact test performance  
- **Monitoring**: Real-time statistics help identify memory usage patterns
- **Test Isolation**: Each test can have custom cleanup policies

## 15. Stateful Mocking

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

## 16. Concurrency and Thread Safety

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

## 17. Best Practices

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