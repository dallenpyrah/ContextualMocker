# Design Decisions: ContextualMocker

This document explains the rationale behind major architectural and API decisions in ContextualMocker. It is intended for engineers who want to understand why the framework is designed the way it is, and what alternatives were considered.

---

## 1. Thread Safety and Shared Mocks

**Decision:**  
All internal state (stubbing rules, invocation records, mock state) is managed using thread-safe data structures (ConcurrentHashMap, ConcurrentLinkedDeque, AtomicReference), keyed by both mock instance and context.

**Rationale:**  
- Existing frameworks (e.g., Mockito, EasyMock) are not reliably thread-safe for stubbing/verification on shared mocks.
- Many modern applications use dependency injection and shared singletons, requiring robust parallel test support.
- Fine-grained concurrency control (per-mock/context) avoids global locks and scales well.

**Alternatives Considered:**  
- Relying on thread-local state (as in Mockito): rejected due to fragility in concurrent and async environments.
- Global synchronization: rejected due to performance bottlenecks.

---

## 2. Explicit Context API

**Decision:**  
All stubbing and verification must specify a context explicitly via `forContext(contextId)`.

**Rationale:**  
- Makes context dependencies explicit and test code more readable.
- Avoids accidental context leakage between tests or threads.
- Supports advanced use cases (multi-tenant, user/session/request isolation).

**Alternatives Considered:**  
- Implicit context via ThreadLocal: rejected as default due to risk of context leakage in thread pools and async code.
- Hybrid approach: may be supported in the future via SPI, but explicit is default for safety.

---

## 3. Stateful Mocking

**Decision:**  
Mocks can have per-context state, and stubbing rules can depend on and change state (`whenStateIs`, `willSetStateTo`).

**Rationale:**  
- Many real-world dependencies are stateful (e.g., login/logout, workflow).
- Enables realistic, expressive tests for session-based or workflow-based logic.
- State is always scoped to both mock and context for isolation.

**Alternatives Considered:**  
- Stateless mocking only: rejected as too limiting for advanced scenarios.

---

## 4. Fluent, BDD-Inspired API

**Decision:**  
The API is designed to be fluent and readable, inspired by Behavior-Driven Development (BDD) patterns.

**Rationale:**  
- Improves test readability and maintainability.
- Aligns with industry best practices and developer expectations.

**Alternatives Considered:**  
- More verbose or imperative APIs: rejected for usability reasons.

---

## 5. ByteBuddy for Mock Creation

**Decision:**  
Mocks are created using ByteBuddy to generate proxies for interfaces.

**Rationale:**  
- ByteBuddy is robust, actively maintained, and widely used (e.g., by Mockito).
- Supports advanced proxying and method interception.

**Alternatives Considered:**  
- Java Proxy API: limited to interfaces, less flexible.
- CGLIB: less actively maintained.

---

## 6. Stubbing Rule Expiration (TTL)

**Decision:**  
Stubbing rules can have a time-to-live (TTL) and expire automatically.

**Rationale:**  
- Prevents memory leaks in long-running or dynamic tests.
- Ensures obsolete rules do not affect future tests.

**Alternatives Considered:**  
- Manual cleanup only: rejected for safety and convenience.

---

## 7. Documentation and Test Coverage

**Decision:**  
All public APIs must be documented with JavaDocs. All features must have comprehensive tests, including concurrency and edge cases.

**Rationale:**  
- Ensures maintainability and reliability.
- Facilitates onboarding and external contributions.

---

## 8. Enhanced Error Messages Design

**Decision:**  
Verification failures generate detailed error messages through `VerificationFailureException` with context information, invocation history, and troubleshooting tips.

**Rationale:**  
- Reduces debugging time by providing comprehensive failure information
- Helps developers understand not just what failed, but why and how to fix it
- Improves developer productivity in test-driven development workflows

**Alternatives Considered:**  
- Basic assertion errors: rejected as insufficient for complex testing scenarios
- Separate logging vs. exception: exception chosen for immediate visibility in test output

---

## 9. Memory Management Architecture

**Decision:**  
Automatic memory management with configurable cleanup policies including age-based and size-based strategies, background cleanup, and real-time monitoring.

**Rationale:**  
- Prevents memory leaks in long-running test suites without manual intervention
- Configurable policies allow optimization for different test scenarios
- Background cleanup avoids impacting test performance
- Monitoring provides visibility into memory usage patterns

**Alternatives Considered:**  
- Manual cleanup only: rejected due to risk of memory leaks and developer burden
- Fixed cleanup policies: rejected in favor of configurable strategies for flexibility
- Synchronous cleanup: rejected due to performance impact on test execution

---

## 10. Spy Implementation Approach

**Decision:**  
Spy support using ByteBuddy to create enhanced subclasses that wrap real objects, with method dispatch checking stubbing rules before delegating to real implementations.

**Rationale:**  
- Enables integration testing with legacy code that cannot be fully mocked
- Maintains all verification capabilities while preserving real behavior
- Consistent with existing mock creation infrastructure

**Alternatives Considered:**  
- Interface-based proxies: rejected due to inability to spy on concrete classes
- Reflection-based interception: rejected due to performance and complexity concerns

---

## 11. JUnit 5 Integration Strategy

**Decision:**  
Annotation-based dependency injection via `ContextualMockerExtension` with automatic lifecycle management and field injection.

**Rationale:**  
- Reduces boilerplate in test setup and teardown
- Aligns with modern testing framework patterns
- Provides automatic resource management for context and mock lifecycles

**Alternatives Considered:**  
- Manual setup in test methods: rejected due to increased boilerplate
- Static factory methods: rejected in favor of declarative annotation approach

---

## 12. API Design Philosophy

**Decision:**  
Multiple API layers including scoped context management, direct methods, builder pattern, and convenience methods to serve different use cases and preferences.

**Rationale:**  
- Scoped contexts provide safety and automatic resource management
- Direct methods reduce verbosity for simple operations
- Builder pattern enables efficient chaining for complex scenarios
- Convenience methods address common patterns with minimal code

**Alternatives Considered:**  
- Single API approach: rejected as insufficient for diverse use cases
- Complex unified API: rejected in favor of focused, purpose-built APIs

---

## 13. Extensibility

**Decision:**  
The framework is designed with extension points (SPI) for future integration with test runners, context propagation, custom matchers, verification modes, and memory management policies.

**Rationale:**  
- Enables adaptation to new frameworks and advanced use cases
- Encourages community contributions
- Supports enterprise customization requirements

---

## 14. Alternatives and Trade-offs

- **Explicit vs. Implicit Context:**  
  Explicit context is safer and more predictable, but slightly more verbose.
- **Per-mock/context state vs. global state:**  
  Per-mock/context is more scalable and avoids test interference.
- **Concurrent collections vs. global locks:**  
  Concurrent collections provide better performance and scalability.

---

For more details, see DESIGN.md and HOW_IT_WORKS.md.