package io.github.dallenpyrah.contextualmocker.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark fields or parameters that should be automatically mocked.
 * 
 * <p>When used with {@link ContextualMockerExtension}, fields annotated with @Mock
 * will be automatically injected with mock objects before each test method.
 * 
 * <p>Example usage:
 * <pre>{@code
 * @ExtendWith(ContextualMockerExtension.class)
 * class MyTest {
 *     @Mock
 *     UserService userService;
 *     
 *     @Test
 *     void testSomething() {
 *         // userService is automatically mocked
 *     }
 * }
 * }</pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Mock {
}