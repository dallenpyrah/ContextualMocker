# Contributing to ContextualMocker

Thank you for your interest in contributing to ContextualMocker! This document provides guidelines and instructions for contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [How to Contribute](#how-to-contribute)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Documentation](#documentation)
- [Release Process](#release-process)

## Code of Conduct

By participating in this project, you agree to abide by our code of conduct:

- Be respectful and inclusive
- Welcome newcomers and help them get started
- Focus on constructive criticism
- Respect differing viewpoints and experiences
- Show empathy towards other community members

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/dallenpyrah/ContextualMocker.git
   cd ContextualMocker
   ```
3. **Add the upstream remote**:
   ```bash
   git remote add upstream https://github.com/dallenpyrah/ContextualMocker.git
   ```

## Development Setup

### Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- Git

### Building the Project

```bash
mvn clean install
```

### Running Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=ContextualMockerCoreTest

# Run tests with coverage
mvn clean test jacoco:report
```

### Debug Logging

Enable debug logging by setting the environment variable:
```bash
export CONTEXTUAL_MOCKER_DEBUG=true
```

Or as a JVM property:
```bash
mvn test -DCONTEXTUAL_MOCKER_DEBUG=true
```

## How to Contribute

### Reporting Bugs

Before creating a bug report, please check existing issues to avoid duplicates. When creating a bug report, include:

- A clear and descriptive title
- Steps to reproduce the issue
- Expected behavior vs actual behavior
- Code samples or test cases
- Your environment (Java version, OS, etc.)

### Suggesting Enhancements

Enhancement suggestions are welcome! Please provide:

- A clear and descriptive title
- Detailed description of the proposed feature
- Use cases and examples
- Any relevant code samples

### Contributing Code

1. **Create a feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes**:
   - Write clean, well-documented code
   - Add tests for new functionality
   - Update documentation as needed

3. **Commit your changes**:
   ```bash
   git commit -m "feat: add new feature"
   ```
   
   We use [Conventional Commits](https://www.conventionalcommits.org/):
   - `feat:` for new features
   - `fix:` for bug fixes
   - `docs:` for documentation changes
   - `test:` for test additions/changes
   - `refactor:` for code refactoring
   - `style:` for formatting changes
   - `chore:` for maintenance tasks

4. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

5. **Create a Pull Request** on GitHub

## Pull Request Process

1. **Ensure all tests pass** locally before submitting
2. **Update the CHANGELOG.md** with your changes in the "Unreleased" section
3. **Update documentation** if you've changed APIs or added features
4. **Write a clear PR description** explaining your changes
5. **Link related issues** using GitHub's issue linking
6. **Be responsive to feedback** and make requested changes promptly

### PR Checklist

- [ ] Tests pass locally (`mvn test`)
- [ ] New tests added for new functionality
- [ ] Documentation updated
- [ ] CHANGELOG.md updated
- [ ] Code follows project style guidelines
- [ ] Commit messages follow conventional commits format

## Coding Standards

### Java Style Guide

- Use 4 spaces for indentation (no tabs)
- Maximum line length: 120 characters
- Use meaningful variable and method names
- Document public APIs with JavaDoc
- Keep methods focused and small
- Prefer composition over inheritance

### Code Organization

- Place new features in appropriate packages
- Follow existing package structure:
  - `core/` - Core functionality
  - `api/` - Public API interfaces
  - `handlers/` - Invocation handlers
  - `matchers/` - Argument matchers
  - `junit5/` - JUnit 5 integration

### Best Practices

- Write self-documenting code
- Use `final` for immutability where appropriate
- Handle null values explicitly
- Use appropriate data structures
- Consider thread safety
- Avoid premature optimization

## Testing Guidelines

### Test Structure

- One test class per production class
- Use descriptive test method names
- Use `@DisplayName` for better test descriptions
- Group related tests using nested classes
- Follow the Arrange-Act-Assert pattern

### Test Coverage

- Aim for high test coverage (>80%)
- Test edge cases and error conditions
- Include integration tests for complex features
- Test concurrent behavior where applicable

### Example Test

```java
@Test
@DisplayName("should return mocked value for specific context")
void testContextSpecificMocking() {
    // Arrange
    Service mock = mock(Service.class);
    ContextID context = new StringContextId("test-context");
    
    // Act
    given(mock).forContext(context)
        .when(() -> mock.doSomething())
        .thenReturn("mocked");
    
    // Assert
    ContextHolder.setContext(context);
    assertEquals("mocked", mock.doSomething());
    ContextHolder.clearContext();
}
```

## Documentation

### JavaDoc

- Document all public classes and methods
- Include `@param`, `@return`, and `@throws` tags
- Provide usage examples in class-level JavaDoc
- Link to related classes using `@see`

### Markdown Documentation

- Update README.md for user-facing changes
- Add detailed explanations in docs/ folder
- Include code examples
- Keep documentation up-to-date with code changes

## Release Process

Releases are automated through GitHub Actions:

1. Merge PR to main branch
2. CI automatically:
   - Determines version bump based on commits
   - Updates version numbers
   - Creates git tag
   - Generates release notes
   - Publishes to Maven repository

### Version Numbering

We follow [Semantic Versioning](https://semver.org/):
- MAJOR: Breaking API changes
- MINOR: New features (backwards compatible)
- PATCH: Bug fixes (backwards compatible)

## Questions?

If you have questions, feel free to:
- Open a GitHub issue
- Start a discussion in the GitHub Discussions tab
- Contact the maintainers

Thank you for contributing to ContextualMocker! 🎉 