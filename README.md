ContextualMocker is a Java mocking framework designed specifically for testing modern, concurrent applications. It addresses the limitations of existing frameworks in handling shared mock instances safely under parallel load and provides first-class support for context-aware mocking and verification.

Traditional mocking tools often exhibit thread-safety issues (like race conditions during stubbing/verification) when used with shared singleton instances common in dependency injection frameworks (e.g., Spring). Furthermore, defining mock behavior based on the operational context (like user ID, request ID, tenant ID) often requires complex workarounds.

ContextualMocker tackles these challenges with:

* **Guaranteed Thread Safety:** Robust internal state management using concurrent data structures ensures reliable parallel test execution, even with shared mocks.
* **Explicit Context API:** A clear and intuitive API (`forContext(contextId)`) makes defining context-specific behavior and verification straightforward and readable.
* **Stateful Mocking:** Support for defining mock behavior based on its current state within a specific context (`whenStateIs`, `willSetStateTo`).
* **Enhanced Error Messages:** Detailed verification failure messages with context information, expected vs actual invocations, and troubleshooting tips.
* **Automatic Memory Management:** Built-in cleanup policies prevent memory leaks with configurable age-based and size-based cleanup strategies.
* **Comprehensive Spy Support:** Partial mocking with real object delegation for legacy code integration.
* **JUnit 5 Integration:** Automatic dependency injection with `@Mock`, `@Spy`, and `@ContextId` annotations.
* **Extended Argument Matchers:** Rich set of matchers including `anyString()`, `contains()`, `regex()`, and custom predicates.
* **ArgumentCaptor Support:** Capture arguments passed to mocked methods for detailed verification and inspection.
* **Fluent API:** A BDD-style API inspired by common mocking patterns.
* **Flexible Mocking:** Supports mocking both interfaces and concrete (non-final) classes.

## Current Status (as of latest update)

* **Core Mocking & Stubbing:** Complete. Mocks can be created for both interfaces and concrete (non-final) classes. Explicit context-aware stubbing (`given`, `forContext`, `when`, `thenReturn`/`Throw`/`Answer`) is implemented and thread-safe. Extended argument matchers (`anyString`, `contains`, `regex`, etc.) are supported.
* **Spy Support:** Complete. Partial mocking with `spy()` method allows selective stubbing while delegating unstubbed methods to real implementations.
* **Core Verification:** Complete. Explicit context-aware verification (`verify`, `forContext`), verification modes (`times`, `never`, `atLeast`, `atMost`), and argument matchers are implemented. Enhanced verification failure messages provide detailed context and troubleshooting tips.
* **Memory Management:** Complete. Automatic cleanup policies with configurable age-based and size-based strategies prevent memory leaks. Manual cleanup controls and memory usage monitoring are available.
* **JUnit 5 Integration:** Complete. Automatic dependency injection with `@Mock`, `@Spy`, and `@ContextId` annotations via `@ExtendWith(ContextualMockerExtension.class)`.
* **Stubbing/Verification Separation:** Complete. Invocations during stubbing setup are correctly excluded from verification counts.
* **Stateful Mocking:** Complete. The API (`whenStateIs`, `willSetStateTo`), state storage (`MockRegistry`), and state transition logic are implemented and thread-safe.
* **Edge Case Tests:** Comprehensive tests cover concurrency, context isolation, state transitions, argument matchers, verification modes, exceptions, GC, memory management, and API misuse. All 130+ tests are passing.
* **Documentation:** Javadoc added to core APIs. A detailed `USAGE.md` guide is available. `DESIGN.md` and implementation documentation cover the architecture and development process.
* **Build & Test:** Configured with Maven and includes a GitHub Actions workflow for automated building, testing, and packaging.

## Quick Start / Usage

ContextualMocker provides multiple APIs for thread-safe, context-aware mocking. **We recommend using the improved APIs** which offer better readability, automatic context management, and reduced boilerplate.

### Maven Dependency
```xml
<dependency>
    <groupId>io.github.dallenpyrah</groupId>
    <artifactId>contextual-mocker</artifactId>
    <version>1.1.0</version> <!-- Use the latest version -->
</dependency>
```

### Basic Usage (Recommended Approach)

**1. Create mocks:**
```java
import static com.contextualmocker.core.ContextualMocker.*;

MyService mockService = mock(MyService.class);
ContextID userId = new StringContextId("user-123");
```

**2. Use automatic context management (Recommended):**
```java
// scopedContext() automatically manages context setup and cleanup
try (ContextScope scope = scopedContext(userId)) {
    // Define behavior for this context
    scope.when(mockService, () -> mockService.getData("request"))
         .thenReturn("user-specific-data");
    
    // Use the mock - context is automatically active
    String result = mockService.getData("request");
    assertEquals("user-specific-data", result);
    
    // Verify interactions in this context
    scope.verify(mockService, times(1), () -> mockService.getData("request"));
}
// Context is automatically restored when scope closes
```

**3. Alternative: Direct methods for simple cases:**
```java
// Direct stubbing - no context management needed
when(mockService, userId, () -> mockService.getData("request"))
    .thenReturn("user-specific-data");

// Direct verification - cleaner than fluent chains
verifyOnce(mockService, userId, () -> mockService.getData("request"));
```

**4. Builder pattern for multiple operations:**
```java
// Efficient for multiple stubs/verifications in same context
withContext(userId)
    .stub(mockService, () -> mockService.getData("request")).thenReturn("data")
    .stub(mockService, () -> mockService.getConfig()).thenReturn("config")
    .verify(mockService, never(), () -> mockService.deleteData());
```

### Key Benefits of the New APIs

- **`scopedContext()`**: Automatic context management prevents context leaks and ensures cleanup
- **Direct methods**: Reduce verbose fluent chains for simple stubbing/verification
- **Builder pattern**: Efficient chaining of multiple operations in the same context
- **Convenience methods**: `verifyOnce()`, `verifyNever()` for common patterns

### Advanced Features

**Spy Support (Partial Mocking):**
```java
UserService realService = new UserServiceImpl();
UserService spy = spy(realService);

// Stub only specific methods
when(spy, context, () -> spy.externalCall()).thenReturn("stubbed");

// Real methods still work
String result = spy.processData("input"); // Calls real implementation
```

**JUnit 5 Integration:**
```java
@ExtendWith(ContextualMockerExtension.class)
class MyTest {
    @Mock MyService mockService;
    @Spy UserService spyService;
    @ContextId("user-123") ContextID userId;
    
    @Test void testWithInjectedMocks() {
        // Mocks and context are automatically injected
    }
}
```

**ArgumentCaptor (Capture Method Arguments):**
```java
// Create a captor for the argument type
ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);

// Use the captor in verification
verifyOnce(userService, context, () -> userService.updateUser(userIdCaptor.capture(), any()));

// Access the captured value
String capturedUserId = userIdCaptor.getValue();
assertEquals("expected-user-id", capturedUserId);

// For multiple invocations
List<String> allCapturedIds = userIdCaptor.getAllValues();
```

**Memory Management:**
```java
// Configure cleanup policies
MockRegistry.setCleanupConfiguration(new CleanupConfiguration(
    5000,    // Max 5000 invocations per context
    120000,  // 2 minutes max age
    30000,   // Cleanup every 30 seconds
    true     // Auto cleanup enabled
));

// Get memory usage statistics
MemoryUsageStats stats = MockRegistry.getMemoryUsageStats();
System.out.println("Total mocks: " + stats.getTotalMocks());

// Manual cleanup
CleanupStats cleaned = MockRegistry.performCleanup();
```

For comprehensive examples including stateful mocking, argument matchers, and concurrency patterns, see [USAGE.md](USAGE.md).

## Limitations and Caveats

- **Final classes and final methods cannot be mocked.**
- **Static methods are not mocked.**
- **Constructors must be accessible** (public or package-private, or accessible via reflection).
- **Abstract classes can be mocked, but only non-final methods will be intercepted.**
- **Inner classes:** Only static inner classes can be mocked; non-static inner classes are not supported.
- **Native methods:** Behavior is undefined and not guaranteed to be intercepted.

## Building and Testing

This project uses Apache Maven.

* **Compile:** `mvn compile`
* **Run Tests:** `mvn test`
* **Package (create JAR):** `mvn package`
* **Build All (Compile, Test, Package):** `mvn verify`

## Comparison to Other Frameworks

| Feature                        | ContextualMocker | Mockito         | EasyMock        | JMockit         | Spock           |
|-------------------------------|------------------|-----------------|-----------------|-----------------|-----------------|
| Thread-safe stubbing/verification on shared mocks | **Yes**         | No              | Limited         | No              | Issues/Partial  |
| Explicit context API            | **Yes**          | No              | No              | No              | No              |
| Context-aware stubbing/verification | **Yes**      | Workarounds     | Workarounds     | Workarounds     | Workarounds     |
| Stateful mocking (per context)  | **Yes**          | Workarounds     | Workarounds     | Workarounds     | Workarounds     |
| Enhanced error messages with context | **Yes**      | Basic           | Basic           | Basic           | Basic           |
| Automatic memory management     | **Yes**          | No              | No              | No              | No              |
| Spy support (partial mocking)  | **Yes**          | Yes             | No              | Yes             | Yes             |
| JUnit 5 integration with annotations | **Yes**     | Yes             | No              | No              | Partial         |
| Extended argument matchers      | **Yes**          | Yes             | Basic           | Yes             | Yes             |
| ArgumentCaptor support          | **Yes**          | Yes             | No              | Partial         | Partial         |
| Fluent, BDD-style API           | **Yes**          | Yes             | Partial         | Partial         | Yes             |
| Stubbing rule expiration (TTL)  | **Yes**          | No              | No              | No              | No              |
| Designed for parallel/concurrent tests | **Yes**   | No              | No              | No              | Partial         |
| JavaDoc & onboarding docs       | **Yes**          | Yes             | Yes             | Yes             | Yes             |

**Key differences:**
- ContextualMocker is designed for thread safety and context-awareness from the ground up, making it uniquely suited for concurrent and multi-tenant applications.
- Other frameworks require workarounds or are not safe for parallel stubbing/verification on shared mocks.
- ContextualMocker provides explicit APIs for context and state, reducing test flakiness and improving clarity.

## Contributing

Contributions are welcome! Please read our [Contributing Guide](docs/CONTRIBUTING.md) for details on our code of conduct, development setup, and the process for submitting pull requests.

## Documentation

- [ONBOARDING.md](docs/ONBOARDING.md): Onboarding guide for new engineers and contributors.
- [CONTRIBUTING.md](docs/CONTRIBUTING.md): Guidelines for contributing to the project.
- [USAGE.md](USAGE.md): Detailed usage guide with practical examples.
- [docs/ARGUMENT_CAPTORS.md](docs/ARGUMENT_CAPTORS.md): Comprehensive guide on using ArgumentCaptors.
- [docs/HOW_IT_WORKS.md](docs/HOW_IT_WORKS.md): Technical walkthrough of the framework.
- [docs/DESIGN.md](docs/DESIGN.md): In-depth design and architecture documentation.
- [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md): Implementation phases, test plan, and status.
- [docs/DESIGN_DECISIONS.md](docs/DESIGN_DECISIONS.md): Rationale for major architectural choices.

## License

This project is licensed under the [Apache License 2.0](LICENSE).
