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
- State is managed per-mock and per-context, ensuring complete isolation.

### ContextScope Lifecycle

The `ContextScope` class provides automatic resource management:

1. **Creation**: `scopedContext(contextId)` saves the current context and sets the new one
2. **Usage**: All operations within the scope use the active context automatically
3. **Cleanup**: When the scope closes (via try-with-resources), the previous context is restored
4. **Nesting**: Scopes can be nested, with inner scopes temporarily overriding outer ones
5. **Thread Safety**: Each thread maintains its own context stack via ThreadLocal storage

---

## 3. Internal Flows and API Architecture

### Mock Creation

- Uses ByteBuddy to generate proxy instances for interfaces and concrete classes.
- Proxies delegate all method calls to a ContextualInvocationHandler.

### API Architecture Overview

ContextualMocker provides multiple API layers for different use cases:

**1. Scoped Context Management (Recommended)**
- `scopedContext(contextId)` returns a `ContextScope` that implements `AutoCloseable`
- Automatically manages context setup/cleanup via try-with-resources
- Prevents context leaks and ensures proper resource management

**2. Direct Methods**
- `when(mock, context, methodCall)` for direct stubbing
- `verify(mock, context, mode, methodCall)` for direct verification
- Bypass verbose fluent chains for simple operations

**3. Builder Pattern**
- `withContext(contextId)` returns a `ContextualMockBuilder`
- Efficient chaining of multiple operations in the same context
- Methods return the builder for continued chaining

**4. Convenience Methods**
- `verifyOnce()`, `verifyNever()` for common verification patterns
- Reduce boilerplate for frequently used operations

### Stubbing Flow (New APIs)

**Scoped Context Approach:**
1. User calls `try (ContextScope scope = scopedContext(contextId))`
2. Context is automatically set for the current thread
3. User calls `scope.when(mock, () -> mock.method()).thenReturn(value)`
4. Stubbing rule is created and registered for the mock/context
5. Context is automatically restored when scope closes

**Direct Method Approach:**
1. User calls `when(mock, contextId, () -> mock.method()).thenReturn(value)`
2. Context is temporarily set for the stubbing operation
3. Stubbing rule is created and registered
4. Context management is handled internally

### Invocation Flow

1. When a method is called on a mock, the handler:
   - Looks up the current context (from ContextHolder or active scope)
   - Looks up the current state for the mock/context
   - Finds the first matching stubbing rule (method, arguments, state)
   - Executes the rule (returns value, throws, or answers dynamically)
   - Records the invocation for later verification
   - Applies any state transitions

### Verification Flow (New APIs)

**Scoped Context Approach:**
1. User calls `scope.verify(mock, mode, () -> mock.method())`
2. Context is already active from the scope
3. Registry is queried for matching invocation records
4. Verification mode is checked and assertions made

**Direct Method Approach:**
1. User calls `verify(mock, contextId, mode, () -> mock.method())`
2. Context is temporarily set for verification
3. Registry queried and verification performed
4. Context management handled internally

**Streamlined Verification:**
1. User calls `verify(mock).forContext(contextId).that(mode, () -> mock.method())`
2. Eliminates the double `.verify()` call pattern
3. More readable than traditional fluent approach

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