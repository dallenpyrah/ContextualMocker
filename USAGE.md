# ContextualMocker Usage Guide

This guide provides examples on how to use the core features of ContextualMocker.

## 1. Setup

Add ContextualMocker as a dependency to your project (details depend on build system, e.g., Maven, Gradle).

## 2. Creating Mocks

Use `ContextualMocker.mock()` to create mock instances. Currently, only interfaces are supported.

```java
import com.contextualmocker.core.ContextualMocker;

// Define your service interface
interface MyService {
    String greet(String name);
    boolean process(int id);
}

// Create a mock
MyService mockService = ContextualMocker.mock(MyService.class);
```

## 3. Defining Context

ContextualMocker requires an explicit context for all stubbing and verification. A context is represented by an object implementing the `ContextID` interface. A simple implementation using `String` is provided:

```java
import com.contextualmocker.core.ContextID;
import com.contextualmocker.core.StringContextId;

ContextID userContext1 = new StringContextId("user1");
ContextID userContext2 = new StringContextId("user2");
```

You can create your own `ContextID` implementations if needed, ensuring they correctly implement `equals()` and `hashCode()`.

## 4. Stubbing

Stubbing defines how a mock should behave when its methods are called within a specific context.

Note: The `.forContext(contextId)` method not only associates the stubbing/verification with a context but also sets this `contextId` as the active `ThreadLocal` context in `ContextHolder`. This means an explicit `ContextHolder.setContext(contextId)` call immediately before a `given(...).forContext(contextId)` or `verify(...).forContext(contextId)` chain is often redundant if that chain is the first to establish the context for subsequent mock interactions on the same thread.

### Basic Stubbing (`thenReturn`)

```java
ContextualMocker.given(mockService)
    .forContext(userContext1) // Specify the context
    .when(mockService.greet("Alice")) // Specify the method call
    .thenReturn("Hello Alice from Context 1!"); // Define the return value

ContextualMocker.given(mockService)
    .forContext(userContext2)
    .when(mockService.greet("Alice")) // Same method, different context
    .thenReturn("Greetings Alice from Context 2!");

// --- Usage ---
// Context is userContext1 (implicitly set by .forContext above, persists for this thread)
String greeting1 = mockService.greet("Alice"); // returns "Hello Alice from Context 1!"

// Context is userContext2 (implicitly set by .forContext above, persists for this thread)
String greeting2 = mockService.greet("Alice"); // returns "Greetings Alice from Context 2!"
```

### Stubbing with Exceptions (`thenThrow`)

```java
ContextualMocker.given(mockService)
    .forContext(userContext1)
    .when(mockService.process(99))
    .thenThrow(new IllegalArgumentException("ID 99 is invalid in Context 1"));

// --- Usage ---
// Context is userContext1 (implicitly set by .forContext above, persists for this thread)
try {
    mockService.process(99);
} catch (IllegalArgumentException e) {
    // Expected exception
}
```

### Stubbing with Dynamic Behavior (`thenAnswer`)

```java
import com.contextualmocker.core.ContextualAnswer;
import com.contextualmocker.core.InvocationDetails;

ContextualMocker.given(mockService)
    .forContext(userContext1)
    .when(mockService.greet(ArgumentMatchers.any())) // Using an argument matcher
    .thenAnswer(new ContextualAnswer<String>() {
        @Override
        public String answer(InvocationDetails invocation) {
            String name = (String) invocation.getArguments()[0];
            ContextID ctx = invocation.getContextId();
            return "Dynamic greeting for " + name + " in context " + ctx;
        }
    });

// --- Usage ---
// Context is userContext1 (implicitly set by .forContext above, persists for this thread)
String dynamicGreeting = mockService.greet("Bob");
// returns "Dynamic greeting for Bob in context StringContextId{id='user1'}"
```

## 5. Verification

Verification checks if methods on a mock were called as expected within a specific context.

### Basic Verification (`times`)

```java
// --- Perform actions ---
// To perform actions, ensure userContext1 is active (e.g., via a preceding forContext in a 'given' block, or explicit ContextHolder.setContext)
mockService.greet("Charlie");
mockService.greet("Charlie");

// --- Verification ---
ContextualMocker.verify(mockService)
    .forContext(userContext1) // Specify context
    .verify(ContextualMocker.times(2)) // Specify verification mode (exactly 2 times)
    .greet("Charlie"); // Specify method call to verify

ContextualMocker.verify(mockService)
    .forContext(userContext1)
    .verify(ContextualMocker.never()) // Expect zero calls
    .greet("David"); // Method was never called with "David"
```

### Other Verification Modes

```java
// To perform actions, ensure userContext1 is active (e.g., via a preceding forContext in a 'given' block, or explicit ContextHolder.setContext)
mockService.process(1);
mockService.process(2);
mockService.process(3);

ContextualMocker.verify(mockService)
    .forContext(userContext1)
    .verify(ContextualMocker.atLeastOnce()) // At least 1 call
    .process(1);

ContextualMocker.verify(mockService)
    .forContext(userContext1)
    .verify(ContextualMocker.atLeast(2)) // At least 2 calls
    .process(ArgumentMatchers.anyInt()); // Use matcher

ContextualMocker.verify(mockService)
    .forContext(userContext1)
    .verify(ContextualMocker.atMost(5)) // At most 5 calls
    .process(ArgumentMatchers.anyInt());
```

### Verifying No (More) Interactions

```java
// To perform actions, ensure userContext1 is active (e.g., via a preceding forContext in a 'given' block, or explicit ContextHolder.setContext)
mockService.greet("Eve");

// Verify the specific interaction
ContextualMocker.verify(mockService)
    .forContext(userContext1)
    .verify(ContextualMocker.times(1))
    .greet("Eve");

// Verify that *no other* interactions happened in this context
ContextualMocker.verifyNoMoreInteractions(mockService, userContext1);

// Verify that *no interactions at all* happened in another context
ContextualMocker.verifyNoInteractions(mockService, userContext2);
```

## 6. Argument Matchers

Matchers provide flexibility when stubbing or verifying method calls.

```java
import com.contextualmocker.matchers.ArgumentMatchers;

// Stubbing with matchers
ContextualMocker.given(mockService)
    .forContext(userContext1)
    .when(mockService.greet(ArgumentMatchers.any())) // Matches any string
    .thenReturn("Generic Greeting");

ContextualMocker.given(mockService)
    .forContext(userContext2)
    .when(mockService.process(ArgumentMatchers.eq(10))) // Matches exactly 10
    .thenReturn(true);

// Verification with matchers
// To perform actions, ensure userContext1 is active (e.g., via a preceding forContext in a 'given' block, or explicit ContextHolder.setContext)
mockService.greet("Frank");
mockService.process(100);

ContextualMocker.verify(mockService)
    .forContext(userContext1)
    .verify(ContextualMocker.times(1))
    .greet(ArgumentMatchers.any()); // Verify any string was passed

// To perform actions, ensure userContext2 is active (e.g., via a preceding forContext in a 'given' block, or explicit ContextHolder.setContext)
mockService.process(10);

ContextualMocker.verify(mockService)
    .forContext(userContext2)
    .verify(ContextualMocker.times(1))
    .process(ArgumentMatchers.eq(10)); // Verify exactly 10 was passed
```

Available matchers include `any()`, `anyInt()`, `eq()`, etc. See `ArgumentMatchers` class.

## 7. Stateful Mocking

Stateful mocking allows mock behavior to depend on and change the mock's state within a specific context.

```java
// Define a stateful interface
interface SessionService {
    boolean login(String user, String pass);
    String getData();
    void logout();
}

// Define states (can be Strings, Enums, or any Object)
final String STATE_LOGGED_OUT = "LOGGED_OUT";
final String STATE_LOGGED_IN = "LOGGED_IN";

SessionService sessionMock = ContextualMocker.mock(SessionService.class);
ContextID sessionContext = new StringContextId("session123");

// Stubbing with state transitions
ContextualMocker.given(sessionMock)
    .forContext(sessionContext)
    .whenStateIs(null) // Initial state is null
    .when(sessionMock.login("test", "pw"))
    .willSetStateTo(STATE_LOGGED_IN) // Transition state on successful login
    .thenReturn(true);

ContextualMocker.given(sessionMock)
    .forContext(sessionContext)
    .whenStateIs(STATE_LOGGED_IN) // Rule applies only when logged in
    .when(sessionMock.getData())
    .thenReturn("Secret Data");

ContextualMocker.given(sessionMock)
    .forContext(sessionContext)
    .whenStateIs(STATE_LOGGED_IN) // Rule applies only when logged in
    .when(sessionMock.logout())
    .willSetStateTo(STATE_LOGGED_OUT); // Transition state on logout

// --- Usage ---
// To perform actions, ensure sessionContext is active (e.g., via a preceding forContext in a 'given' block, or explicit ContextHolder.setContext)

// Initially, getData() might return null or throw (if default not stubbed)
// String initialData = sessionMock.getData(); // Behavior depends on default/other stubbing

boolean loggedIn = sessionMock.login("test", "pw"); // returns true, state becomes LOGGED_IN
String data = sessionMock.getData(); // returns "Secret Data"
sessionMock.logout(); // state becomes LOGGED_OUT

// Now getData() won't match the stateful rule anymore
// String postLogoutData = sessionMock.getData(); // Behavior depends on default/other stubbing
```

State is managed per-mock, per-context, ensuring isolation.

## 8. Concurrency and Context Management

ContextualMocker is designed for thread safety. However, you are responsible for managing the `ContextID` and setting it correctly (e.g., using `ContextHolder.setContext(id)`) before interacting with mocks, especially in concurrent scenarios. Ensure the correct context is active on the thread interacting with the mock.

```java
// Example using ExecutorService (simplified)
ExecutorService executor = Executors.newFixedThreadPool(2);
ContextID ctxA = new StringContextId("A");
ContextID ctxB = new StringContextId("B");

// Stubbing for different contexts...
ContextualMocker.given(mockService).forContext(ctxA).when(mockService.greet("X")).thenReturn("Hello from A");
ContextualMocker.given(mockService).forContext(ctxB).when(mockService.greet("X")).thenReturn("Hi from B");

Future<String> futureA = executor.submit(() -> {
    ContextHolder.setContext(ctxA); // Set context for this thread
    String result = mockService.greet("X");
    ContextHolder.clearContext(); // Clean up context
    return result;
});

Future<String> futureB = executor.submit(() -> {
    ContextHolder.setContext(ctxB); // Set context for this thread
    String result = mockService.greet("X");
    ContextHolder.clearContext(); // Clean up context
    return result;
});

assertEquals("Hello from A", futureA.get());
assertEquals("Hi from B", futureB.get());

executor.shutdown();
```

Always ensure `ContextHolder.clearContext()` or setting the correct context is handled, often using `try-finally` blocks or test framework extensions, to prevent context leakage between threads or tests. 