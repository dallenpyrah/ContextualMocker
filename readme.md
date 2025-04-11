# ContextualMocker

A parallel-safe, context-aware Java mocking framework for reliable testing of shared dependencies in concurrent applications.

## Overview

ContextualMocker provides robust, thread-safe mocking for shared instances, with a fluent API for defining context-specific behavior and verification. It is designed for developers building and testing concurrent Java applications who need reliable and expressive mocking tools for shared dependencies operating under varying contexts.

## Features

- Parallel-safe stubbing and verification for shared mocks
- Explicit context management (e.g., per user, request, or tenant)
- Fluent, readable API inspired by Mockito and BDD patterns
- Support for argument matchers and custom answers
- Stateful mocking for realistic workflow and session-based scenarios
- Extensible architecture with SPIs for integration
- Designed for performance and scalability

## Installation

To use ContextualMocker in your project, add the following dependency to your `pom.xml` if you are using Maven:

```xml
<dependency>
    <groupId>com.contextualmocker</groupId>
    <artifactId>contextualmocker</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

For Gradle users, add this to your `build.gradle`:

```groovy
testImplementation 'com.contextualmocker:contextualmocker:1.0.0'
```

Ensure you have a compatible Java version (Java 8 or higher) as ContextualMocker leverages modern Java concurrency utilities for thread safety.

## Quick Start

Here's a basic example to get you started with ContextualMocker:

```java
import com.contextualmocker.*;

ContextID user1 = new StringContextId("user1");
UserService mockService = ContextualMocker.mock(UserService.class);

ContextualMocker.given(mockService)
    .forContext(user1)
    .when(service -> service.getUserData("dataKey"))
    .thenReturn("User1 Data");

String result = mockService.getUserData("dataKey"); // returns "User1 Data" for user1 context
```

## Usage Examples

### Context-Specific Stubbing

Define different behaviors for the same mock based on context:

```java
ContextID user1 = new StringContextId("user1");
ContextID user2 = new StringContextId("user2");
UserService mockService = ContextualMocker.mock(UserService.class);

ContextualMocker.given(mockService)
    .forContext(user1)
    .when(service -> service.getUserData("dataKey"))
    .thenReturn("User1 Data");

ContextualMocker.given(mockService)
    .forContext(user2)
    .when(service -> service.getUserData("dataKey"))
    .thenReturn("User2 Data");

// Test behavior for different contexts
String user1Result = mockService.getUserData("dataKey"); // returns "User1 Data"
String user2Result = mockService.getUserData("dataKey"); // returns "User2 Data"
```

### Verification with Context

Verify interactions within a specific context:

```java
ContextualMocker.verify(mockService)
    .forContext(user1)
    .verify(ContextualMocker.times(1))
    .getUserData("dataKey");
```

### Stateful Mocking

Simulate stateful behavior within a context:

```java
ContextID session1 = new StringContextId("session1");
SessionService mockSession = ContextualMocker.mock(SessionService.class);

ContextualMocker.given(mockSession)
    .forContext(session1)
    .whenStateIs(null)
    .when(mock -> mock.login("user", "pass"))
    .willSetStateTo("LOGGED_IN")
    .thenReturn(true);

ContextualMocker.given(mockSession)
    .forContext(session1)
    .whenStateIs("LOGGED_IN")
    .when(mock -> mock.getSecret())
    .thenReturn("top-secret");

// Test stateful behavior
boolean loggedIn = mockSession.login("user", "pass"); // returns true, state transitions to "LOGGED_IN"
String secret = mockSession.getSecret(); // returns "top-secret"
```

### Argument Matchers

Use argument matchers for flexible stubbing and verification:

```java
ContextualMocker.given(mockService)
    .forContext(user1)
    .when(service -> service.getUserData(ArgumentMatchers.any()))
    .thenReturn("Generic Data");

ContextualMocker.verify(mockService)
    .forContext(user1)
    .verify(ContextualMocker.atLeastOnce())
    .getUserData(ArgumentMatchers.eq("dataKey"));
```

## Project Structure

ContextualMocker's source code is organized into logical subfolders to enhance maintainability and clarity:

- **core**: Contains the central components like `MockRegistry` and primary mocking logic.
- **handlers**: Includes invocation handlers for managing mock interactions.
- **initiators**: Provides classes for initiating stubbing and verification processes with context awareness.
- **matchers**: Supports argument matching capabilities for flexible stubbing and verification.

This structure reflects the modular design of the framework, making it easier to extend and maintain.

## Documentation

- [Implementation Plan](docs/IMPLEMENTATION_PLAN.md)
- [Design Document](docs/DESIGN.md)

## Contributing

Contributions are welcome! Please see the [Implementation Plan](docs/IMPLEMENTATION_PLAN.md) for current priorities and open tasks. To contribute, fork the repository, create a feature branch, and submit a pull request. For questions or suggestions, open an issue.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
