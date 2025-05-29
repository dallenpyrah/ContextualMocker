package com.contextualmocker.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark fields that should be automatically wrapped with spy objects.
 * 
 * <p>When used with {@link ContextualMockerExtension}, fields annotated with @Spy
 * will be automatically wrapped with spy objects before each test method.
 * The field must be initialized with a real object instance.
 * 
 * <p>Example usage:
 * <pre>{@code
 * @ExtendWith(ContextualMockerExtension.class)
 * class MyTest {
 *     @Spy
 *     EmailService emailService = new EmailServiceImpl();
 *     
 *     @Test
 *     void testSomething() {
 *         // emailService is automatically wrapped with a spy
 *         // Can stub specific methods while keeping real behavior for others
 *     }
 * }
 * }</pre>
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Spy {
}