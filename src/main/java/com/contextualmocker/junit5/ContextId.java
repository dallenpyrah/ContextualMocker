package com.contextualmocker.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark fields or parameters that should be automatically injected
 * with ContextID objects.
 * 
 * <p>When used with {@link ContextualMockerExtension}, fields annotated with @ContextId
 * will be automatically injected with ContextID objects before each test method.
 * 
 * <p>Example usage:
 * <pre>{@code
 * @ExtendWith(ContextualMockerExtension.class)
 * class MyTest {
 *     @ContextId("test-context")  // Fixed context ID
 *     ContextID fixedContext;
 *     
 *     @ContextId(autoSet = true)  // Auto-generated and auto-set as current context
 *     ContextID autoContext;
 *     
 *     @ContextId  // Auto-generated unique context ID per test
 *     ContextID dynamicContext;
 *     
 *     @Test
 *     void testSomething() {
 *         // Context IDs are automatically injected
 *         // autoContext is automatically set as current context
 *     }
 * }
 * }</pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ContextId {
    /**
     * The context ID value. If empty, a unique ID will be generated for each test.
     */
    String value() default "";
    
    /**
     * Whether to automatically set this context as the current context before the test runs.
     * Defaults to false.
     */
    boolean autoSet() default false;
}