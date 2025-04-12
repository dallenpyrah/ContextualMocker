# ContextualMocker: A Parallel-Safe, Context-Aware Java Mocking Framework

[![Build Status](https://github.com/<YOUR_USERNAME>/ContextualMocker/actions/workflows/build-test-package.yml/badge.svg)](https://github.com/<YOUR_USERNAME>/ContextualMocker/actions/workflows/build-test-package.yml) <!-- Placeholder: Replace with actual repo path -->

## Introduction

ContextualMocker is a Java mocking framework designed specifically for testing modern, concurrent applications. It addresses the limitations of existing frameworks in handling shared mock instances safely under parallel load and provides first-class support for context-aware mocking and verification.

Traditional mocking tools often exhibit thread-safety issues (like race conditions during stubbing/verification) when used with shared singleton instances common in dependency injection frameworks (e.g., Spring). Furthermore, defining mock behavior based on the operational context (like user ID, request ID, tenant ID) often requires complex workarounds.

ContextualMocker tackles these challenges with:

*   **Guaranteed Thread Safety:** Robust internal state management using concurrent data structures ensures reliable parallel test execution, even with shared mocks.
*   **Explicit Context API:** A clear and intuitive API (`forContext(contextId)`) makes defining context-specific behavior and verification straightforward and readable.
*   **Stateful Mocking:** Support for defining mock behavior based on its current state within a specific context (`whenStateIs`, `willSetStateTo`).
*   **Fluent API:** A BDD-style API inspired by common mocking patterns.

## Current Status (as of latest update)

*   **Core Mocking & Stubbing:** Complete. Mocks can be created for interfaces. Explicit context-aware stubbing (`given`, `forContext`, `when`, `thenReturn`/`Throw`/`Answer`) is implemented and thread-safe. Argument matchers (`any`, `eq`, etc.) are supported.
*   **Core Verification:** Complete. Explicit context-aware verification (`verify`, `forContext`), verification modes (`times`, `never`, `atLeast`, `atMost`), and argument matchers are implemented. Verification failures throw clear errors. Methods `verifyNoMoreInteractions` and `verifyNoInteractions` are available.
*   **Stubbing/Verification Separation:** Complete. Invocations during stubbing setup are correctly excluded from verification counts.
*   **Stateful Mocking:** Complete. The API (`whenStateIs`, `willSetStateTo`), state storage (`MockRegistry`), and state transition logic are implemented and thread-safe.
*   **Edge Case Tests:** Comprehensive tests cover concurrency, context isolation, state transitions, argument matchers, verification modes, exceptions, GC, and API misuse. Most core concurrency/stateful scenarios are tested and passing.
*   **Documentation:** Javadoc added to core APIs. A detailed `USAGE.md` guide is available. `DESIGN.md` and `IMPLEMENTATION_PLAN.md` document the architecture and development process.
*   **Build & Test:** Configured with Maven and includes a GitHub Actions workflow for automated building, testing, and packaging.

## Quick Start / Usage

ContextualMocker enables thread-safe, context-aware mocking for concurrent Java applications. The typical usage pattern is:

1. **Create a mock for your interface:**
   ```java
   MyService mock = ContextualMocker.mock(MyService.class);
   ```
2. **Set the context for the current thread:**
   ```java
   ContextHolder.setContext(contextId);
   ```
3. **Define context-specific stubbing:**
   ```java
   ContextualMocker.given(mock)
       .forContext(contextId)
       .when(() -> mock.someMethod(args))
       .thenReturn(result);
   ```
4. **Call methods and verify interactions:**
   ```java
   ContextualMocker.verify(mock)
       .forContext(contextId)
       .verify(times(1))
       .someMethod(args);
   ```

For comprehensive, real-world examples (including stateful mocking, argument matchers, and concurrency), see [USAGE.md](USAGE.md).

## Building and Testing

This project uses Apache Maven.

*   **Compile:** `mvn compile`
*   **Run Tests:** `mvn test`
*   **Package (create JAR):** `mvn package`
*   **Build All (Compile, Test, Package):** `mvn verify`

## Comparison to Other Frameworks

| Feature                        | ContextualMocker | Mockito         | EasyMock        | JMockit         | Spock           |
|-------------------------------|------------------|-----------------|-----------------|-----------------|-----------------|
Thread-safe stubbing/verification on shared mocks | **Yes**         | No              | Limited         | No              | Issues/Partial  |
Explicit context API            | **Yes**          | No              | No              | No              | No              |
Context-aware stubbing/verification | **Yes**      | Workarounds     | Workarounds     | Workarounds     | Workarounds     |
Stateful mocking (per context)  | **Yes**          | Workarounds     | Workarounds     | Workarounds     | Workarounds     |
Fluent, BDD-style API           | **Yes**          | Yes             | Partial         | Partial         | Yes             |
Argument matchers               | Yes              | Yes             | Yes             | Yes             | Yes             |
Stubbing rule expiration (TTL)  | **Yes**          | No              | No              | No              | No              |
Designed for parallel/concurrent tests | **Yes**   | No              | No              | No              | Partial         |
JavaDoc & onboarding docs       | **Yes**          | Yes             | Yes             | Yes             | Yes             |

**Key differences:**
- ContextualMocker is designed for thread safety and context-awareness from the ground up, making it uniquely suited for concurrent and multi-tenant applications.
- Other frameworks require workarounds or are not safe for parallel stubbing/verification on shared mocks.
- ContextualMocker provides explicit APIs for context and state, reducing test flakiness and improving clarity.

## Contributing

Contributions are welcome! Please refer to the design documents and implementation plan.
## Documentation

- [ONBOARDING.md](docs/ONBOARDING.md): Onboarding guide for new engineers and contributors.
- [USAGE.md](USAGE.md): Detailed usage guide with practical examples.
- [docs/HOW_IT_WORKS.md](docs/HOW_IT_WORKS.md): Technical walkthrough of the framework.
- [docs/DESIGN.md](docs/DESIGN.md): In-depth design and architecture documentation.
- [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md): Implementation phases, test plan, and status.
- [docs/DESIGN_DECISIONS.md](docs/DESIGN_DECISIONS.md): Rationale for major architectural choices.

## License

This project is licensed under the [Apache License 2.0](LICENSE).
- [docs/DESIGN_DECISIONS.md](docs/DESIGN_DECISIONS.md): Rationale for major architectural choices.

## License

This project is licensed under the [Apache License 2.0](LICENSE).
