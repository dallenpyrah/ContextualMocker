# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands

- **Compile:** `mvn compile`
- **Run Tests:** `mvn test`
- **Run a Single Test:** `mvn test -Dtest=TestClassName#methodName`
- **Run Tests in a Package:** `mvn test -Dtest="com.contextualmocker.*Test"`
- **Package JAR:** `mvn package`
- **Full Build:** `mvn verify` (compile, test, package)
- **Code Coverage Report:** `mvn test jacoco:report`
- **Clean and Build:** `mvn clean verify`

## Architecture Overview

ContextualMocker is a Java mocking framework designed for thread-safe, context-aware mocking of objects in concurrent applications. The architecture follows these key components:

1. **Core Components:**
   - `ContextualMocker`: Main entry point with static methods for mock creation and verification
   - `MockRegistry`: Central registry for mock objects, invocations, and stubbing rules
   - `ContextHolder`: Manages the current context for thread-local operations
   - `CanonicalMockReference`: Safe weak-reference system for mocks to prevent memory leaks
   - `InvocationRecord`: Stores method call information with timestamps and arguments

2. **Context Management:**
   - `ContextID`: Interface for context identifiers (typically implemented by `StringContextId`)
   - `ContextScope`: Auto-closeable scopes for safe context handling (try-with-resources)

3. **Invocation Handlers:**
   - `ContextualInvocationHandler`: Base invocation handler using Java Dynamic Proxies
   - `MethodCaptureInvocationHandler`: Captures method calls for stubbing
   - `VerificationMethodCaptureHandler`: Captures method calls for verification
   - `SpyInvocationHandler`: Special handler for spy objects that can delegate to real implementations

4. **Stubbing and Verification:**
   - `StubbingRule`: Defines behavior for specific method calls in specific contexts
   - `VerificationFailureException`: Provides detailed error messages for verification failures
   - `ArgumentCaptor`: Captures arguments for later assertions during verification

5. **Matchers:**
   - Various matcher implementations (e.g., `EqMatcher`, `AnyMatcher`, `RegexMatcher`)
   - `ArgumentMatchers`: Static utility class for matcher creation

6. **JUnit 5 Integration:**
   - `ContextualMockerExtension`: JUnit 5 extension for auto-injection of mocks
   - Annotations: `@Mock`, `@Spy`, `@ContextId`, `@Captor`

## Design Guidelines

1. **Thread Safety First:**
   - All components must be thread-safe by design
   - Use concurrent collections for shared state
   - Context isolation must prevent cross-contamination between tests

2. **Context-Awareness:**
   - APIs should always consider the execution context
   - Easy context switching and isolation is a priority

3. **Memory Management:**
   - Automatic cleanup policies prevent memory leaks
   - Reference management strategies for long-running tests

4. **API Styles:**
   - Prefer the scoped context API style (try-with-resources)
   - Maintain backward compatibility with fluent APIs
   - Builder patterns for complex configurations