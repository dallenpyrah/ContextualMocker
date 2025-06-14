package io.github.dallenpyrah.contextualmocker.junit5;

import io.github.dallenpyrah.contextualmocker.core.ContextHolder;
import io.github.dallenpyrah.contextualmocker.core.ContextID;
import io.github.dallenpyrah.contextualmocker.core.ContextualMocker;
import io.github.dallenpyrah.contextualmocker.core.StringContextId;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.UUID;

/**
 * JUnit 5 extension that provides automatic injection of mocks and context management.
 * 
 * <p>Usage:
 * <pre>{@code
 * @ExtendWith(ContextualMockerExtension.class)
 * class MyTest {
 *     @Mock
 *     UserService userService;
 *     
 *     @Spy
 *     EmailService emailService = new EmailServiceImpl();
 *     
 *     @ContextId
 *     ContextID testContext;
 *     
 *     @Test
 *     void testSomething() {
 *         // userService is automatically mocked
 *         // testContext is automatically set
 *         // Context is cleaned up after test
 *     }
 * }
 * }</pre>
 */
public class ContextualMockerExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Object testInstance = context.getRequiredTestInstance();
        Class<?> testClass = testInstance.getClass();
        
        // Inject mocks and spies
        for (Field field : testClass.getDeclaredFields()) {
            field.setAccessible(true);
            
            if (field.isAnnotationPresent(Mock.class)) {
                Object mock = ContextualMocker.mock(field.getType());
                field.set(testInstance, mock);
            } else if (field.isAnnotationPresent(Spy.class)) {
                Object existingValue = field.get(testInstance);
                if (existingValue == null) {
                    throw new IllegalStateException("@Spy field '" + field.getName() + "' must be initialized with a real object instance");
                }
                Object spy = ContextualMocker.spy(existingValue);
                field.set(testInstance, spy);
            } else if (field.isAnnotationPresent(ContextId.class)) {
                ContextId annotation = field.getAnnotationsByType(ContextId.class)[0];
                ContextID contextId;
                
                if (!annotation.value().isEmpty()) {
                    contextId = new StringContextId(annotation.value());
                } else {
                    // Generate unique context ID for this test
                    String testName = context.getDisplayName();
                    String uniqueId = testName + "-" + UUID.randomUUID().toString().substring(0, 8);
                    contextId = new StringContextId(uniqueId);
                }
                
                field.set(testInstance, contextId);
                
                // Auto-set context if requested
                if (annotation.autoSet()) {
                    ContextHolder.setContext(contextId);
                }
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        // Clean up context after each test
        ContextHolder.clearContext();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Parameter parameter = parameterContext.getParameter();
        return parameter.isAnnotationPresent(Mock.class) || 
               parameter.isAnnotationPresent(Spy.class) ||
               parameter.isAnnotationPresent(ContextId.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Parameter parameter = parameterContext.getParameter();
        
        if (parameter.isAnnotationPresent(Mock.class)) {
            return ContextualMocker.mock(parameter.getType());
        } else if (parameter.isAnnotationPresent(Spy.class)) {
            throw new IllegalStateException("@Spy parameters must provide a real object instance. Use field injection instead.");
        } else if (parameter.isAnnotationPresent(ContextId.class)) {
            ContextId annotation = parameter.getAnnotation(ContextId.class);
            
            if (!annotation.value().isEmpty()) {
                return new StringContextId(annotation.value());
            } else {
                // Generate unique context ID for this test
                String testName = extensionContext.getDisplayName();
                String uniqueId = testName + "-" + UUID.randomUUID().toString().substring(0, 8);
                return new StringContextId(uniqueId);
            }
        }
        
        throw new IllegalStateException("Unsupported parameter type: " + parameter.getType());
    }
}