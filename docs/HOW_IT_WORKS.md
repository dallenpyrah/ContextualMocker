# How ContextualMocker Works: Technical Walkthrough

This document provides a technical overview of the ContextualMocker framework for engineers who want to understand its internal architecture, core flows, and extensibility.

---

## 1. High-Level Architecture

ContextualMocker is designed to enable thread-safe, context-aware mocking for concurrent Java applications. The framework is organized into several key modules:

- **core/**: Central logic, including the MockRegistry and main API entry points.
- **handlers/**: Invocation handlers for managing method calls on mocks.
- **initiators/**: Classes for initiating stubbing and verification flows.
- **matchers/**: Argument matcher logic for flexible stubbing and verification.

**Key Principle:**  
All state (stubbing rules, invocation records, mock state) is managed per-mock and per-context, using thread-safe data structures.

---

## 2. Core Concepts

### Context

- **ContextID**: An object (often a String or custom type) that uniquely identifies the operational context (e.g., user, request, tenant).
- **ContextHolder**: Manages the current context for the executing thread, using an InheritableThreadLocal.

### Mock Registry

- **MockRegistry**: A singleton, thread-safe registry that stores:
  - Stubbing rules: What should happen when a method is called in a given context/state.
  - Invocation records: What actually happened (for verification).
  - State: The current state of a mock in a given context.

### Stubbing and Verification

- **Stubbing**:  
  - Use `ContextualMocker.given(mock).forContext(contextId).when(...).thenReturn(...)` to define behavior for a mock in a specific context.
  - Stubbing rules are stored in the registry and matched in LIFO order.

- **Verification**:  
  - Use `ContextualMocker.verify(mock).forContext(contextId).verify(times(1)).method(...)` to check that a method was called as expected.
  - Verification queries invocation records for the given mock/context.

### Stateful Mocking

- Mocks can have state that changes as methods are called (e.g., login/logout).
- Stubbing rules can be conditional on the current state and can trigger state transitions.

---

## 3. Internal Flows

### Mock Creation

- Uses ByteBuddy to generate proxy instances for interfaces.
- Proxies delegate all method calls to a ContextualInvocationHandler.

### Stubbing Flow

1. User calls `given(mock).forContext(contextId).when(...).thenReturn(...)`.
2. A stubbing rule is created and added to the registry for the mock/context.
3. Rules can specify required state and state transitions.

### Invocation Flow

1. When a method is called on a mock, the handler:
   - Looks up the current context (from ContextHolder or explicit API).
   - Looks up the current state for the mock/context.
   - Finds the first matching stubbing rule (method, arguments, state).
   - Executes the rule (returns value, throws, or answers dynamically).
   - Records the invocation for later verification.
   - Applies any state transitions.

### Verification Flow

1. User calls `verify(mock).forContext(contextId).verify(mode).method(...)`.
2. The registry is queried for invocation records matching the method, arguments, and context.
3. The verification mode (times, never, atLeast, atMost) is checked.
4. If verification fails, an AssertionError is thrown with details.

---

## 4. Extension Points

- **Custom ContextID**: Implement your own context identifier type (must implement equals/hashCode).
- **Custom ArgumentMatchers**: Extend the matchers module for new matching logic.
- **SPI for Context Resolution**: (Planned) Plug in custom logic for resolving context from frameworks (e.g., web request, MDC).
- **Custom Mock Creation**: (Planned) Extend or replace the mock creation logic for advanced use cases.

---

## 5. Adding New Features

- Add new modules or classes in the appropriate package (core, handlers, initiators, matchers).
- Update the registry and handler logic as needed.
- Add comprehensive tests for new features, including concurrency and edge cases.
- Update documentation (README, USAGE, DESIGN, HOW_IT_WORKS).

---

## 6. Further Reading

- **README.md**: Project overview and value proposition.
- **USAGE.md**: Practical usage examples.
- **docs/DESIGN.md**: In-depth design and rationale.
- **docs/IMPLEMENTATION_PLAN.md**: Implementation phases and test plan.
- **docs/DESIGN_DECISIONS.md**: Rationale for major architectural choices.

---

For questions or suggestions, see the ONBOARDING.md or contact a maintainer.