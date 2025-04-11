# ContextualMocker: A Parallel-Safe, Context-Aware Java Mocking Framework

## 1. Introduction

Modern software applications, especially those built on microservices or handling concurrent user requests, often rely on shared components implemented as singletons (e.g., service beans in dependency injection frameworks). Testing these applications effectively under concurrent load poses significant challenges for existing Java mocking frameworks.

Current tools, while powerful for traditional unit testing, have limitations when dealing with concurrent operations on shared mock instances. Issues such as race conditions during stubbing or verification lead to flaky, unreliable tests, undermining developer confidence. Furthermore, these frameworks lack native support for context-aware mocking – the ability to define mock behavior based on the specific operational context (e.g., user session, request ID, tenant ID) of a concurrent request interacting with a shared component. Developers often resort to complex workarounds involving intricate `Answer` implementations or fragile parameter matching, which obscure test intent and are difficult to maintain.

This document outlines the design for ContextualMocker, a new, hypothetical open-source Java mocking framework engineered specifically to address these shortcomings. ContextualMocker aims to provide robust, parallel-safe mocking capabilities for shared instances, coupled with a first-class, intuitive API for defining context-aware behavior and verification. It targets developers building and testing concurrent Java applications who require reliable and expressive mocking tools for shared dependencies operating under varying contexts.

## 2. The Core Problem: Concurrency and Context Limitations in Existing Frameworks

The fundamental motivation for ContextualMocker stems from two interconnected deficiencies in the current Java mocking landscape: the lack of reliable thread safety for concurrent operations on shared mocks, and the absence of native mechanisms for context-aware mocking.

### Concurrency Challenges with Shared Mocks:

* **Mockito's Thread-Safety Issues:** While Mockito is a widely used Java mocking framework, its design has documented thread-safety problems when stubbing or verifying a shared mock instance concurrently from multiple threads. While concurrent invocations on an already-stubbed mock are generally considered safe, the setup (stubbing) and assertion (verification) phases can lead to intermittent failures like `UnfinishedStubbingException`, `WrongTypeOfReturnValue`, or incorrect verification counts when tests involving shared mocks are run in parallel.
* **Underlying Cause:** These issues appear to be rooted in Mockito's internal reliance on thread-local state to manage the process of stubbing and verification (e.g., tracking the "last invocation" before `thenReturn` or `verify`). When multiple threads attempt to stub or verify the same shared mock instance simultaneously, this thread-local mechanism can lead to race conditions, where one thread's operation interferes with another's, corrupting Mockito's internal state.
* **Workarounds and Their Limits:** Developers have attempted workarounds, such as external synchronization using locks around Mockito calls, avoiding shared mocks altogether by creating mocks per test or per thread (often using `ThreadLocal`), or using Spring Boot's `@MockBean(reset = MockReset.NONE)`. However, these solutions add significant complexity, boilerplate, or only address specific scenarios, failing to provide a general, framework-level guarantee of safety. Other frameworks like EasyMock and JMockit also face challenges or lack explicit support for robust parallel testing on shared mocks. Spock, while supporting parallel execution via the JUnit Platform, requires careful use of `@ResourceLock` for shared state and may have issues with shared spies.

### The Need for Explicit Context-Awareness:

* **Modern Application Complexity:** Concurrent applications often process multiple independent operations simultaneously (e.g., handling web requests for different users, processing messages for various tenants). Shared components (singletons) involved in these operations frequently need to behave differently based on the context of the specific operation (e.g., return user-specific data based on `userId`, apply tenant-specific rules based on `tenantId`, correlate logs via `requestId`).
* **Limitations of Implicit Context:** Existing mocking frameworks lack first-class support for defining mock behavior based on such application-level contexts. Developers are forced to simulate context-awareness indirectly through:
    * **Complex Answer Implementations:** Writing custom `Answer` logic that inspects method arguments to infer context and return different values. This tightly couples tests to implementation details (argument order/type) and makes tests verbose and hard to read.
    * **Argument Matching:** Using intricate argument matchers to differentiate calls based on context data passed as parameters. This also leads to brittle and less readable tests.
    * **Stateful Mocks:** Employing stateful mocking techniques where the mock transitions between states based on interactions, indirectly representing context changes. This often requires significant setup and doesn't map cleanly to distinct, independent contexts.
* **Contextual Mocking Decisions:** Research indicates that mocking decisions themselves are often context-dependent. A dependency might be mocked in one test scenario but not another, based on how it interacts with the class under test (CUT) in that specific context. Inappropriate mocking (under-mocking or over-mocking) can lead to ineffective tests or maintenance burdens. ContextualMocker aims to provide a mechanism to manage these context-specific behaviors explicitly.
* **Existing Contextual Tools:** The need for context management in testing is recognized in specific domains. For example, AEM Mocks provide an `AemContext` object to manage the AEM-specific environment for tests, and Ctest4J links configuration parameters (a form of context) to specific tests. These demonstrate the value of context management, highlighting the gap for a general-purpose mocking solution.

The challenges of concurrency safety and context awareness are not independent; they converge critically in the testing of modern applications. Concurrent requests or operations are often the distinct contexts that necessitate different behaviors from shared components. Testing shared singletons under realistic parallel load inherently requires the ability to define mock behavior specific to the context of each concurrent operation. Existing frameworks struggle on two fronts: concurrent stubbing/verification on the shared mock instance breaks due to internal thread-safety issues, and even if these operations were safe, defining and managing behavior per-context remains cumbersome and lacks dedicated API support.

Furthermore, it is important to distinguish the goals of a mocking framework like ContextualMocker from specialized concurrency testing tools. Tools like RaceFuzzer, Fray, MAPTest, or jcstress focus on exploring thread interleavings, detecting data races or deadlocks, and controlling thread scheduling, often by instrumenting code or manipulating the runtime environment without necessarily mocking dependencies. ContextualMocker, conversely, focuses on providing controllable behavior (via mocking) for dependencies within a potentially concurrent execution environment. Its primary contribution lies in robust internal state management that allows context-specific behavior definition and verification, addressing the specific shortcomings of mocking frameworks in concurrent settings. It assumes the underlying test execution (potentially parallel) is managed by the test runner (e.g., JUnit, TestNG) or potentially integrated with concurrency control tools in the future, rather than controlling thread scheduling itself.

**Table 2.1: Feature Comparison: Concurrency & Context in Mocking Frameworks**

| Feature                        | Mockito                       | EasyMock                      | JMockit                       | Spock                         | WireMock (Server)             | ContextualMocker (Proposed) |
| :----------------------------- | :---------------------------- | :---------------------------- | :---------------------------- | :---------------------------- | :---------------------------- | :-------------------------- |
| Parallel Test Runner Support | Yes (JUnit/TestNG)            | Limited/Issues                | No/Problematic                | Yes (JUnit Platform)          | Yes (Server)                  | Yes                       |
| Shared Mock Stubbing Safety  | No                            | Limited/Issues                | No/Problematic                | Issues w/ shared/spy          | Yes (via API/Scenarios)       | Yes                       |
| Shared Mock Verification Safety| No                            | Limited/Issues                | No/Problematic                | Yes (w/ @ResourceLock)         | Yes (via API/Scenarios)       | Yes                       |
| Explicit Context API           | No                            | No                            | No                            | No                            | Limited (Scenarios/Proxy)     | Yes                       |
| Stateful Mocking (Basic)     | Yes (via Answer)              | Yes                           | Yes                           | Yes                           | Yes                           | Yes                       |
| Contextual Stateful Mocking    | No                            | No                            | No                            | No                            | Limited (via Scenarios)       | Yes                       |

## 3. ContextualMocker: Design Philosophy and Goals

ContextualMocker is guided by a set of core principles aimed at providing a robust, intuitive, and performant solution for mocking in concurrent, context-driven environments.

* **Safety First for Concurrency:** The paramount goal is to guarantee thread safety and deterministic behavior for all framework operations, particularly concurrent stubbing and verification targeting shared mock instances. The design must proactively eliminate the race conditions and flakiness observed in existing tools. Reliability in concurrent scenarios is non-negotiable and foundational to the framework's value proposition. This necessitates a shift away from designs relying implicitly on thread-local state for core operations, towards explicit, thread-safe state management.
* **Intuitive Context Management:** Context should be treated as a first-class citizen within the framework. The API must provide clear, explicit, and user-friendly mechanisms for defining the context associated with a mock interaction (stubbing or verification). The goal is to make specifying context-dependent behavior straightforward and highly readable, reflecting the reality that mocking decisions are often context-aware. Complex boilerplate or overly implicit context handling should be avoided in favor of clarity and safety.
* **Expressive and Fluent API:** The public APIs for stubbing and verification should be designed for fluency and readability, promoting maintainable test code. Where applicable, the API should align with established patterns like Behavior-Driven Development (Given-When-Then). Lessons learned from the success and usability of Mockito's API design should be incorporated, adapted for the requirements of context management and concurrency safety.
* **Performance and Scalability:** While correctness and safety are primary, the framework must be designed with performance and scalability in mind, especially for highly concurrent test scenarios. The internal architecture should minimize lock contention and reduce memory overhead. Efficient concurrency primitives, such as `ConcurrentHashMap` and its atomic operations, should be leveraged. Performance benchmarks under load will be essential to validate the design.
* **Extensibility and Integration:** The framework should offer well-defined extension points (Service Provider Interfaces - SPIs) to facilitate integration with various test runners (e.g., JUnit 5, TestNG), context propagation frameworks (e.g., those using MDC or other mechanisms), and potentially other testing or diagnostic tools. This allows for customization and broader adoption, learning from Mockito's extension mechanisms like `MockMaker` and `MockitoListener`.

A key consideration influencing these principles is the inherent trade-off between implicit convenience and explicit safety. Mockito's original API, exemplified by `when(mock.method()).thenReturn(...)`, prioritized convenience by implicitly managing stubbing state via thread-locals. This worked well in single-threaded tests but proved fragile under concurrency. In contrast, approaches like WireMock's rely on explicit state management within the mock server. ContextualMocker must deliberately favor explicitness in its core state management for concurrency and context handling. The safety guarantees provided by explicit context identification and thread-safe central storage outweigh the minor increase in API verbosity compared to the simplest single-threaded Mockito usage. Fluency will be achieved through the overall API structure (e.g., `given(...).forContext(...).when(...).thenReturn(...)`) rather than by hiding the underlying state management complexities.

## 4. Core Architecture

The architecture of ContextualMocker is designed around a central, thread-safe registry that manages mock state based on both the mock instance and the specific context of interaction.

### 4.1 Mock Instantiation:

* **Mechanism:** Mock instances will be created using bytecode manipulation, generating proxies that intercept method calls. ByteBuddy is the preferred library for this task, given its capabilities, active maintenance, and successful adoption by Mockito 2+ and Spock.
* **Process:** When `ContextualMocker.mock()` is called, ByteBuddy generates a proxy class extending or implementing the target type. This proxy holds a reference to a central `ContextualInvocationHandler`.
* **Extensibility:** A `ContextualMockMaker` interface, analogous to Mockito's `MockMaker`, could be provided as an extension point for custom mock creation strategies, ensuring the design accommodates context requirements.

### 4.2 Invocation Interception:

* **Mechanism:** All method calls on a mock instance are intercepted by the generated ByteBuddy proxy.
* **Handler Delegation:** The proxy delegates every intercepted invocation to a shared (but internally thread-safe) `ContextualInvocationHandler` instance associated with the mock. This handler is the central point for processing incoming calls.

### 4.3 Thread-Safe, Context-Aware State Management (CRUCIAL):

* **Central Component:** A singleton `MockRegistry` instance serves as the global repository for all mock state managed by the framework. This registry must be implemented using thread-safe constructs.
* **Data Structures:** The core of the registry involves nested concurrent maps to store stubbing rules and invocation records, keyed first by the mock instance and then by the context ID.

    * **Stubbing Rules:**
        ```java
### 4.x Stubbing Invocation Recording and Verification Separation

A key challenge in Java mocking frameworks is ensuring that invocations made during stubbing setup (e.g., the method call inside `when(mock.method(...))`) are not counted as real invocations for verification. In ContextualMocker, this is addressed by:

- Marking invocations made during stubbing setup and filtering them out from verification counts.
- Due to Java's evaluation order, the stubbing flag cannot be set before the method call in the `when` API. As a workaround, the framework removes the most recent invocation for the mock/context after stubbing is set up, ensuring only real test-driven invocations are counted.
- This approach is robustly tested with scenarios involving multiple stubbing setups, interleaved stubbing and invocation, and context separation, confirming that stubbing does not affect verification counts.

This design ensures that verification accurately reflects only the invocations made during the test, not those made as part of stubbing setup, and is critical for correct and predictable test outcomes.
        // Key: Weak ref to mock instance, Value: Map of ContextID -> Rules
        ConcurrentMap<WeakReference<Object>, ConcurrentMap<Object, List<StubbingRule>>> stubbingRules;
        ```
        * `WeakReference<Object>`: Using a weak reference to the mock instance allows the mock object to be garbage collected if it's no longer referenced elsewhere, preventing memory leaks.
        * `Object` (Inner Map Key): Represents the `ContextID`. This can be any user-defined object (String, Long, custom type) provided it correctly implements `equals()` and `hashCode()`.
        * `List<StubbingRule>`: A thread-safe list containing the ordered stubbing rules for that specific mock/context pair. Each `StubbingRule` encapsulates method matchers, argument matchers, and the corresponding `Answer` or return value. The choice of list implementation (e.g., `CopyOnWriteArrayList`, `synchronized ArrayList`, `ConcurrentLinkedQueue`) depends on the expected read/write ratio for stubbing operations. `CopyOnWriteArrayList` is efficient if stubbing is infrequent compared to invocation handling reads.

    * **Invocation Records:**
        ```java
        // Key: Weak ref to mock instance, Value: Map of ContextID -> Invocations
        ConcurrentMap<WeakReference<Object>, ConcurrentMap<Object, BlockingQueue<InvocationRecord>>> invocationRecords;
        ```
        * `BlockingQueue<InvocationRecord>`: A thread-safe queue (e.g., `ConcurrentLinkedQueue` or `LinkedBlockingQueue`) is suitable for recording invocations, as this is primarily a high-frequency write operation during test execution. Using a queue facilitates ordered recording. Sharding or other strategies might be needed for extreme throughput scenarios.
        * `InvocationRecord`: An immutable object capturing invocation details (mock reference, method, arguments, context ID, timestamp, thread ID).

* **Concurrency Control:**
    * Leverage `ConcurrentHashMap`'s inherent thread safety and atomic operations (`computeIfAbsent`, `compute`, `merge`) for managing the nested map structures. This provides efficient, fine-grained concurrency control for accessing state related to different mocks or contexts.
    * Operations modifying the `List<StubbingRule>` for a specific mock/context must be atomic. If `CopyOnWriteArrayList` isn't suitable due to frequent stubbing changes, explicit locking (e.g., a `ReentrantLock` associated with the inner map entry) might be needed around list modifications.
    * Adding to the `invocationRecords` queue should be highly concurrent; `ConcurrentLinkedQueue` is a strong candidate.
    * Avoid global locks on the entire `MockRegistry`. Locking should be scoped as narrowly as possible, ideally at the level of a specific mock instance or mock/context pair.

* **Rationale:** This architecture directly addresses the core requirement: state isolation per mock and per context, managed concurrently. `ConcurrentHashMap` provides a robust and performant foundation. Weak references prevent memory leaks often associated with global registries holding onto mock objects.

### 4.4 Context Identification Strategies:

Determining the correct `ContextID` for an invocation is critical. Several strategies are possible, each with trade-offs:

* **Strategy 1: Explicit Context Passing (Recommended Default):**
    * **Mechanism:** The test author explicitly provides the `ContextID` object through the API, such as `ContextualMocker.given(mock).forContext(myContextId)....`.
    * **Pros:** Most robust and unambiguous. Guarantees thread safety regardless of execution model (thread pools, async frameworks). Makes context dependency explicit in the test code.
    * **Cons:** Requires the test author to manage and pass the context identifier, potentially increasing verbosity.
    * **Implementation:** The passed `ContextID` is used directly as the key for the inner `ConcurrentHashMap` in the `MockRegistry`.

* **Strategy 2: Implicit Context Capture via ThreadLocal:**
    * **Mechanism:** The framework offers utilities like `ContextualMocker.runInContext(contextId, () -> { /* test code */ })` or integrates with JUnit/TestNG extensions to manage a `ThreadLocal<ContextID>`. Framework methods like `when(...)` or `verify(...)` would implicitly retrieve the context from this `ThreadLocal`.
    * **Pros:** Can lead to less verbose API calls within the contextual block. Feels more "automatic".
    * **Cons:** Inherently fragile in asynchronous execution environments or applications using thread pools where threads are reused, as the `ThreadLocal` value might leak or be incorrect. Requires extremely careful setup and cleanup (e.g., using `ThreadLocal.remove()` in `finally` blocks or via test framework extensions). Can obscure the dependency on context. Potential for memory leaks if `remove()` is not diligently called.
    * **Implementation:** The `ContextualInvocationHandler` reads from the `ThreadLocal`. Requires robust lifecycle management, ideally automated via extensions.

* **Strategy 3: Framework Integration Hooks:**
    * **Mechanism:** Define SPIs (e.g., `ContextResolver`) or listener interfaces (akin to `MockitoListener` or AEM Context Plugins) that allow external frameworks (e.g., web frameworks, context propagation libraries) to provide the `ContextID`.
    * **Pros:** Enables seamless integration with existing application context mechanisms (e.g., retrieving a request ID from MDC).
    * **Cons:** Requires specific integration code to be developed for each supported framework. Can be complex to implement correctly.
    * **Implementation:** The registered hook/listener is invoked by the `ContextualInvocationHandler` to obtain the current `ContextID`.

* **Default Choice & Rationale:** Explicit Context Passing is recommended as the default strategy due to its superior robustness, predictability, and safety across diverse execution

---

**Implementation Status (v1):**
- The explicit context-passing API (`forContext(contextId)`) is implemented as described.
- Core stubbing logic (`when`, `thenReturn`, `thenThrow`, `thenAnswer`) is implemented and thread-safe via `MockRegistry`.
- The public API now enforces explicit context for all stubbing operations.
- Verification API is implemented: `verify`, `forContext`, and verification modes (`times`, `never`, `atLeastOnce`, `atMost`) allow context-specific verification of mock interactions.
- Argument matcher support is implemented: `any()`, `eq()`, and matcher context are supported for both stubbing and verification, enabling expressive and flexible matching of method arguments.

## 5. Verification API and Argument Matcher Support

### 5.1 Verification API

ContextualMocker provides a fluent, context-aware verification API to assert mock interactions within a specific context. The API supports verification modes and integrates with argument matchers.

**Example Usage:**
```java
ContextualMocker.verify(mock)
    .forContext(contextId)
    .verify(ContextualMocker.times(2))
    .someMethod(ArgumentMatchers.any(), ArgumentMatchers.eq("foo"));
```

- `verify(mock)`: Begins verification for the given mock.
- `forContext(contextId)`: Specifies the context for verification.
- `verify(mode)`: Sets the verification mode (e.g., times, never, atLeastOnce, atMost).
- Method call: The method to verify, with support for argument matchers.

**Verification Modes:**
- `ContextualMocker.times(int n)`: Expects exactly n invocations.
- `ContextualMocker.never()`: Expects zero invocations.
- `ContextualMocker.atLeastOnce()`: Expects at least one invocation.
- `ContextualMocker.atLeast(int n)`: Expects at least n invocations.
- `ContextualMocker.atMost(int n)`: Expects at most n invocations.

### 5.2 Argument Matcher Support

Argument matchers allow flexible matching of method arguments in both stubbing and verification.

- `ArgumentMatchers.any()`: Matches any argument.
- `ArgumentMatchers.eq(value)`: Matches arguments equal to the given value.

Matchers are registered in a thread-local context and are consumed in the order of method arguments. If matchers are present, they are used for matching; otherwise, direct value comparison is used.

**Example Usage:**
```java
ContextualMocker.given(mock)
    .forContext(contextId)
    .when(mock.someMethod(ArgumentMatchers.any(), ArgumentMatchers.eq("foo")))
    .thenReturn(result);
```

Thread safety is maintained throughout stubbing and verification, with all matcher and invocation state managed per context in the MockRegistry.

## 6. Stateful Mocking and State Transitions

### 6.1 Motivation

In many real-world scenarios, the behavior of a dependency is not only context-dependent but also state-dependent within that context. For example, a mock representing a session or workflow may need to return different results or throw exceptions based on its current state, and state transitions may occur as a result of method invocations. ContextualMocker supports stateful mocking to enable these advanced, realistic test scenarios.

### 6.2 API Extensions

Stateful mocking introduces new API methods to the stubbing DSL:

- `whenStateIs(Object state)`: Restricts the stubbing rule to apply only when the mock is in the specified state for the given context.
- `willSetStateTo(Object newState)`: Specifies that, when the stubbed method is invoked, the mock's state for the context will transition to `newState`.

These methods can be composed fluently with existing stubbing methods:

```java
ContextualMocker.given(mock)
    .forContext(contextId)
    .whenStateIs("LOGGED_OUT")
    .when(mock.login("user", "pass"))
    .willSetStateTo("LOGGED_IN")
    .thenReturn(true);
```

### 6.3 State Storage and Transitions

- The `MockRegistry` maintains a per-mock, per-context state map:
    ```java
    ConcurrentMap<WeakReference<Object>, ConcurrentMap<Object, AtomicReference<Object>>> stateMap;
    ```
    - The outer map is keyed by mock instance (weak reference).
    - The inner map is keyed by context ID.
    - The value is an `AtomicReference<Object>` representing the current state for that mock/context.

- When a stubbed method is invoked:
    - The framework checks the current state for the mock/context.
    - If a stubbing rule with a matching `whenStateIs` is found, it is applied.
    - If the rule specifies `willSetStateTo`, the state is atomically updated after the invocation.

- The initial state for a mock/context is `null` unless explicitly set.

### 6.4 Thread Safety

- All state transitions are managed using `AtomicReference` to ensure atomicity and visibility across threads.
- The state map is managed using `ConcurrentHashMap` for thread-safe access and updates.
- State transitions are performed atomically to prevent race conditions in concurrent test scenarios.

### 6.5 Example Usage

```java
// Initial state is null
ContextualMocker.given(mock)
    .forContext(ctx)
    .whenStateIs(null)
    .when(mock.login("user", "pass"))
    .willSetStateTo("LOGGED_IN")
    .thenReturn(true);

ContextualMocker.given(mock)
    .forContext(ctx)
    .whenStateIs("LOGGED_IN")
    .when(mock.logout())
    .willSetStateTo("LOGGED_OUT")
    .thenReturn(true);

ContextualMocker.given(mock)
    .forContext(ctx)
    .whenStateIs("LOGGED_IN")
    .when(mock.getSecret())
    .thenReturn("top-secret");
```

### 6.6 Design Considerations

- The stateful mocking feature is fully compatible with context-aware and thread-safe design principles.
- State is always scoped to both the mock instance and the context, ensuring isolation between concurrent test scenarios.
- The API is designed to be fluent and expressive, supporting realistic workflow and session-based mocking patterns.

---