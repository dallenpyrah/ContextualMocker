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

## 8. Extensibility

**Decision:**  
The framework is designed with extension points (SPI) for future integration with test runners, context propagation, and custom matchers.

**Rationale:**  
- Enables adaptation to new frameworks and advanced use cases.
- Encourages community contributions.

---

## 9. Alternatives and Trade-offs

- **Explicit vs. Implicit Context:**  
  Explicit context is safer and more predictable, but slightly more verbose.
- **Per-mock/context state vs. global state:**  
  Per-mock/context is more scalable and avoids test interference.
- **Concurrent collections vs. global locks:**  
  Concurrent collections provide better performance and scalability.

---

For more details, see DESIGN.md and HOW_IT_WORKS.md.