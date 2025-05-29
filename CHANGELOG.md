# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added

-   **Enhanced Error Messages:** Detailed verification failure messages with context information, expected vs actual invocations, invocation history with timestamps, and troubleshooting tips for easier debugging.
-   **Automatic Memory Management:** Built-in cleanup policies to prevent memory leaks with configurable age-based and size-based cleanup strategies, background cleanup scheduler, and memory usage monitoring.
-   **Comprehensive Spy Support:** Partial mocking with `spy()` method allows selective stubbing while delegating unstubbed methods to real implementations for legacy code integration.
-   **JUnit 5 Integration:** Automatic dependency injection with `@Mock`, `@Spy`, and `@ContextId` annotations via `@ExtendWith(ContextualMockerExtension.class)`.
-   **Extended Argument Matchers:** Rich set of matchers including `anyString()`, `contains()`, `startsWith()`, `endsWith()`, `regex()`, `anyCollection()`, `range()`, and custom predicates.
-   **Improved API Design:** Added `scopedContext()` for automatic context management, direct methods for simple cases, builder pattern for multiple operations, and convenience methods like `verifyOnce()` and `verifyNever()`.

### Changed

-   **Verification System:** Enhanced `ContextualVerificationMode` interface with `verifyCountWithContext()` method that provides detailed error information using `VerificationFailureException`.
-   **MockRegistry Architecture:** Expanded with `CleanupConfiguration`, `MemoryUsageStats`, and `CleanupStats` classes for comprehensive memory management.

## [1.0.0] - 2025-04-13

### Added

-   **Core Mocking API:** Introduced the main `ContextualMocker` API for creating mocks (`mock()`), defining context-specific behavior (`given(...).forContext(...).when(...).thenReturn/thenThrow/thenAnswer(...)`), and performing context-specific verification (`verify(...).forContext(...).verify(...)`).
-   **Context Awareness:** All stubbing and verification operations now explicitly require a `ContextID` via the `forContext()` method, ensuring isolation between different operational contexts.
-   **Stateful Mocking:** Added API methods (`whenStateIs(state)`, `willSetStateTo(newState)`) to define mock behavior based on the current state within a context and trigger state transitions upon invocation.
-   **Argument Matchers:** Implemented common argument matchers (`ArgumentMatchers.any()`, `ArgumentMatchers.eq()`, etc.) for flexible stubbing and verification. Matcher usage is context-aware.
-   **Verification Modes:** Added standard verification modes (`times(n)`, `never()`, `atLeastOnce()`, `atLeast(n)`, `atMost(n)`) for precise assertion of interaction counts within a context.
-   **Additional Verification Methods:** Added `ContextualMocker.verifyNoMoreInteractions(mock)` and `ContextualMocker.verifyNoInteractions(mock)` for comprehensive verification.
-   **Stubbing Rule Expiration (TTL):** Introduced `ttlMillis(long)` method in the stubbing API to allow automatic expiration of stubbing rules after a specified duration.
-   **Class Mocking Support (Experimental):** Added initial support for mocking concrete classes (interfaces remain the primary target). Note limitations around final classes/methods.
-   **Comprehensive Documentation:**
    -   Added `docs/ONBOARDING.md`, `docs/DESIGN.md`, `docs/USAGE.md`, `docs/USE_CASES.md`.
    -   Enhanced JavaDocs for all public APIs.
    -   Improved `README.md`.
-   **Logging System:** Integrated SLF4J with Logback for internal logging. Debug logs (`CONTEXTUAL_MOCKER_DEBUG=true` or `-DCONTEXTUAL_MOCKER_DEBUG=true`) are disabled by default.
-   **Extensive Test Suite:** Added comprehensive tests covering core features, concurrency, stateful mocking, argument matchers, edge cases, and resource management.
-   **GitHub Actions CI/CD Workflow:** Implemented automated build, test, packaging, versioning (using Conventional Commits), and release generation workflow.

### Changed

-   **API Design:** Refactored the public API to consistently require explicit context passing using `forContext(contextId)`.
-   **Internal State Management:** Reworked internal state (stubbing rules, invocation records, argument matchers, state) to be strictly per-mock and per-context, ensuring thread safety and context isolation.
-   **Improved Verification Error Messages:** Made verification failure messages more informative, including method name and arguments.

### Fixed

-   **Verification Accuracy:** Invocations made during stubbing setup (`when(...)` calls) are no longer incorrectly counted towards verification totals.
-   **Argument Matcher Context:** Argument matcher storage is now correctly isolated per context, preventing potential leakage between contexts within the same thread.
-   **Concurrency Safety:** Addressed potential race conditions in concurrent stubbing, verification, and state management through thread-safe data structures and atomic operations.

### Removed

-   **Implicit Context Handling:** Removed any reliance on implicit thread-local state for core stubbing and verification logic in favor of explicit context passing.
-   **Redundant Internal Logging:** Replaced `System.out` debug statements with structured SLF4J logging.

[Keep a Changelog]: https://keepachangelog.com/en/1.0.0/
[1.0.0]: https://github.com/dallenpyrah/contextualmocker/releases/tag/v1.0.0