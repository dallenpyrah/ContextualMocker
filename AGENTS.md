# ContextualMocker Agent Guidelines

## Build/Test Commands
- **Build**: `mvn clean compile`
- **Test all**: `mvn test`
- **Test single**: `mvn test -Dtest=ClassName#methodName`
- **Test with coverage**: `mvn clean test jacoco:report`
- **Package**: `mvn clean package`

## Code Style
- **Java version**: 11+ (source/target)
- **Package structure**: `io.github.dallenpyrah.contextualmocker.*`
- **Imports**: Group by package, static imports last (e.g., `import static org.junit.jupiter.api.Assertions.*`)
- **Test naming**: `ClassNameTest.java`, methods: `testFeatureDescription()`
- **Assertions**: Use JUnit 5 assertions, static import preferred
- **Mocking**: Use ContextualMocker's own API for tests
- **Logging**: SLF4J API with Logback implementation
- **Null handling**: Explicit null checks with descriptive exceptions
- **Documentation**: JavaDoc for all public APIs
- **Thread safety**: All public APIs must be thread-safe
- **Context management**: Always clear context in @BeforeEach/@AfterEach