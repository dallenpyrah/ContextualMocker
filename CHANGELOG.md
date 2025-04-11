# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
- Fixed: Invocations made during stubbing setup are no longer counted toward verification, resolving overcounting in verification (e.g., "Expected 2 invocations but got 3" for `greet`).
- Workaround: Due to Java evaluation order, the stubbing invocation is removed after setup to ensure only real invocations are counted.
- Added robustness tests to ensure stubbing does not affect verification counts, including multiple stubbing, interleaved stubbing and invocation, and context separation.
- Implemented stateful mocking: per-mock, per-context state storage and transitions.
- Added API methods `whenStateIs(state)` and `willSetStateTo(newState)` for stateful stubbing.
- Stubbing rules can now be restricted to a specific state and trigger state transitions after invocation.
- State transitions and isolation are thread-safe and context-aware.
- Added tests for stateful mocking, state transitions, and per-context state isolation.
- Initial changelog created to track progress on ContextualMocker.
- Project identified as a parallel-safe, context-aware Java mocking framework.
- Refactored public API to require explicit context passing using `forContext(contextId)`.
- Implemented core stubbing logic: `when`, `thenReturn`, `thenThrow`, `thenAnswer` for context-specific stubbing.
- Added thread-safe stubbing rule registration in `MockRegistry` for per-mock, per-context state management.
- Implemented verification API: `verify`, `forContext`, and verification modes (`times`, `never`, `atLeastOnce`, `atMost`).
- Added argument matcher support: `any()`, `eq()`, and matcher context for both stubbing and verification.
- Integrated thread-safe verification logic with `MockRegistry` and invocation records.
- Updated stubbing and verification flows to support both direct value and matcher-based argument matching.