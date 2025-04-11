# ContextualMocker

A parallel-safe, context-aware Java mocking framework for reliable testing of shared dependencies in concurrent applications.

## Overview

ContextualMocker provides robust, thread-safe mocking for shared instances, with a fluent API for defining context-specific behavior and verification. It is designed for developers building and testing concurrent Java applications who need reliable and expressive mocking tools for shared dependencies operating under varying contexts.

## Features

- Parallel-safe stubbing and verification for shared mocks
- Explicit context management (e.g., per user, request, or tenant)
- Fluent, readable API inspired by Mockito and BDD patterns
- Support for argument matchers and custom answers
- Extensible architecture with SPIs for integration
- Designed for performance and scalability

## Quick Start

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

See [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md) for the implementation roadmap and [docs/DESIGN.md](docs/DESIGN.md) for the full design document.

## Documentation

- [Implementation Plan](docs/IMPLEMENTATION_PLAN.md)
- [Design Document](docs/DESIGN.md)

## Contributing

Contributions are welcome! Please see the [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md) for current priorities and open tasks. To contribute, fork the repository, create a feature branch, and submit a pull request. For questions or suggestions, open an issue.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
