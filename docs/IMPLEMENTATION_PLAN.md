# ContextualMocker Implementation Plan

## Overview

This document outlines the prioritized and sequenced plan to implement the missing and incomplete features required for a functional, parallel-safe, context-aware mocking framework as defined in the project design.

---

## Implementation Phases

```mermaid
graph TD
    subgraph Phase 1: Core Mocking & Stubbing Foundation
        A[1.1: Implement ContextualMocker.mock()] --> B(1.2: Implement Explicit Stubbing Flow)
        B --> C(1.3: Integrate Argument Matchers - Stubbing)
        C --> D(1.4: Basic Stubbing Error Handling)
    end

    subgraph Phase 2: Core Verification
        Phase1 --> E[2.1: Implement Explicit Verification Flow]
        E --> F(2.2: Implement Verification Modes)
        F --> G(2.3: Implement Verification method() & Matchers)
        G --> H(2.4: Implement verifyNo(More)Interactions)
        H --> I(2.5: Basic Verification Error Handling)
    end

    subgraph Phase 3: Refinement & Documentation
        Phase2 --> J[3.1: Write Javadoc]
        Phase2 --> K(3.2: Write Initial User Guide)
        Phase2 --> L(3.3: Refine Error Handling)
        Phase2 --> M(3.4: Add Concurrency Tests)
    end

    subgraph Phase 4: Advanced Features (Post-Core)
        Phase3 --> N[4.1: Stateful Mocking]
        Phase3 --> O[4.2: ThreadLocal Context]
        Phase3 --> P[4.3: SPIs]
        Phase3 --> Q[4.4: Performance Tuning]
    end

    Phase1 --> Phase2
    Phase2 --> Phase3
    Phase3 --> Phase4
```

---

## Phase 1: Core Mocking & Stubbing Foundation

**Objective:** Establish the ability to create mocks and define context-specific stubbing rules reliably.

- **1.1: Implement `ContextualMocker.mock(Class<T>)`**
  - Use ByteBuddy to generate proxy instances.
  - Proxies delegate to `ContextualInvocationHandler`.
  - Handle interfaces and non-final classes.

- **1.2: Implement Explicit Contextual Stubbing Flow** (Implemented)
- Implemented `given(T mock)`, `forContext(ContextID contextId)`, `when(Function<T, R> methodCall)`, and terminal methods (`thenReturn`, `thenThrow`, `thenAnswer`).
- Ensured atomic, thread-safe updates to `MockRegistry.stubbingRules`.

- **1.3: Integrate Argument Matchers (Stubbing)**
  - Allow standard Mockito `ArgumentMatchers` in `when()`.
  - Ensure thread-safe capture and application of matchers.

- **1.4: Basic Stubbing Error Handling**
  - Implement error handling for invalid API usage during stubbing.

---

## Phase 2: Core Verification

**Objective:** Enable verification of mock interactions within specific contexts.

- **2.1: Implement Explicit Contextual Verification Flow**
  - Implement `verify(T mock)`, `forContext(ContextID contextId)`, and create `ContextSpecificVerificationInitiator`.

- **2.2: Implement Verification Modes**
  - Implement `times`, `never`, `atLeastOnce`, `atMost`, etc.

- **2.3: Implement Verification `method()` & Matchers**
  - Implement terminal `method(Consumer<T> methodCall)`.
  - Query `MockRegistry.invocationRecords` for the specified context.
  - Apply verification mode logic and argument matchers.

- **2.4: Implement `verifyNoMoreInteractions` / `verifyNoInteractions`**
  - Implement context-specific interaction checks.

- **2.5: Basic Verification Error Handling**
  - Implement error handling for verification failures.

---

## Phase 3: Refinement & Documentation

**Objective:** Solidify the core implementation with documentation and essential tests.

- **3.1: Write Javadoc**
  - Document all public APIs from Phases 1 & 2.

- **3.2: Write Initial User Guide**
  - Guide on core concepts and usage.

- **3.3: Refine Error Handling**
  - Improve error messages and handling.

- **3.4: Add Concurrency Tests**
  - Validate thread safety with targeted tests.

---

## Phase 4: Advanced Features (Potential Post-v1.0)

**Objective:** Implement features beyond the core requirements.

- **4.1: Stateful Mocking**
- **4.2: ThreadLocal Context**
- **4.3: Extension Points (SPIs)**
- **4.4: Performance Analysis & Optimization**

---

## Key Considerations

- Concurrency: Rigorous testing at each stage, especially for concurrent access to `MockRegistry`.
- Atomicity: Ensure operations modifying shared state are atomic.
- Error Handling: Provide clear and informative error messages.
- Changelog: Update `CHANGELOG.md` after significant tasks or phases.
- Commits: Make granular commits reflecting sub-task completion.