# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
- Initial changelog created to track progress on ContextualMocker.
- Project identified as a parallel-safe, context-aware Java mocking framework.
- Refactored public API to require explicit context passing using `forContext(contextId)`.
- Implemented core stubbing logic: `when`, `thenReturn`, `thenThrow`, `thenAnswer` for context-specific stubbing.
- Added thread-safe stubbing rule registration in `MockRegistry` for per-mock, per-context state management.
- Implemented verification API: `verify`, `forContext`, and verification modes (`times`, `never`, `atLeastOnce`, `atMost`).
- Added argument matcher support: `any()`, `eq()`, and matcher context for both stubbing and verification.
- Integrated thread-safe verification logic with `MockRegistry` and invocation records.
- Updated stubbing and verification flows to support both direct value and matcher-based argument matching.