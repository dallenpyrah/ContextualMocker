# When and Why Use ContextualMocker

ContextualMocker is a Java mocking framework designed for scenarios where traditional mocking tools fall short—especially when your tests require precise control over mock behavior in different contexts. If your project involves complex logic that depends on execution context, or if you need to isolate mock behavior across parallel or stateful test runs, ContextualMocker provides the tools to make your tests robust, maintainable, and reliable.

## Typical Scenarios and Problems Solved

*   **Context-Dependent Logic:** When your code behaves differently based on context (such as user sessions, request scopes, or multi-tenant systems), ContextualMocker allows you to define and verify mock behavior specific to each context, avoiding cross-contamination between tests.
*   **Parallel and Concurrent Testing:** In environments where tests run in parallel or share resources, generic mocks can lead to flaky or brittle tests due to shared state. ContextualMocker enables context isolation, ensuring that stubbing and verification are scoped correctly, even under concurrency.
*   **Stateful and Sequential Interactions:** For systems where the sequence of interactions or the state of the mock matters, ContextualMocker provides mechanisms to capture, stub, and verify method calls with full awareness of the current context.
*   **Complex Verification Requirements:** When you need to verify that certain interactions happened only within a specific context or under certain conditions, ContextualMocker's context-aware verification modes make these assertions straightforward and reliable.

## Who Should Use ContextualMocker

*   **Teams Building Large or Complex Java Applications:** Especially those with layered architectures, microservices, or domain-driven designs where context is a first-class concern.
*   **Projects with High Concurrency or Parallelism:** Such as server-side applications, frameworks, or libraries that must be tested under concurrent conditions.
*   **Developers Needing Fine-Grained Mock Control:** If you find yourself fighting with global or static mocks, or needing to reset or reconfigure mocks between tests, ContextualMocker's context isolation will simplify your workflow.
*   **Organizations Seeking Reliable, Maintainable Tests:** By reducing test flakiness and making context explicit, ContextualMocker helps teams maintain a high-quality, trustworthy test suite.

## Unique Strengths and Differentiators

*   **Contextual Isolation:** Unlike generic mocking frameworks, ContextualMocker allows you to define, stub, and verify mocks within explicit contexts, preventing accidental leakage of behavior between tests.
*   **Advanced Stubbing and Verification:** Support for context-specific stubbing rules and verification modes enables precise, expressive tests for complex scenarios.
*   **Concurrency-Friendly Design:** Built with parallel and concurrent test execution in mind, minimizing issues related to shared state or race conditions.
*   **Extensible and Modular:** The framework's architecture (as seen in its core, handler, and matcher components) is designed for extensibility, making it suitable for advanced users and custom integrations.

## In Summary

Use ContextualMocker when you need more than just basic mocking—when your tests demand context awareness, isolation, and reliability, especially in complex or concurrent Java projects. Its unique approach to context management sets it apart from traditional mocking tools, making it an excellent choice for teams who value robust, maintainable, and precise tests.

