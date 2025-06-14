package io.github.dallenpyrah.contextualmocker.core;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Enhanced exception thrown when mock verification fails.
 * Provides detailed information about expected vs actual invocations,
 * context information, and helpful debugging details.
 */
public class VerificationFailureException extends AssertionError {
    private final Object mock;
    private final ContextID contextId;
    private final Method method;
    private final Object[] expectedArgs;
    private final int expectedCount;
    private final int actualCount;
    private final String verificationMode;
    private final List<InvocationRecord> actualInvocations;

    public VerificationFailureException(
            Object mock,
            ContextID contextId, 
            Method method,
            Object[] expectedArgs,
            int expectedCount,
            int actualCount,
            String verificationMode,
            List<InvocationRecord> actualInvocations) {
        super(buildDetailedMessage(mock, contextId, method, expectedArgs, expectedCount, actualCount, verificationMode, actualInvocations));
        this.mock = mock;
        this.contextId = contextId;
        this.method = method;
        this.expectedArgs = expectedArgs;
        this.expectedCount = expectedCount;
        this.actualCount = actualCount;
        this.verificationMode = verificationMode;
        this.actualInvocations = actualInvocations;
    }

    private static String buildDetailedMessage(
            Object mock,
            ContextID contextId,
            Method method,
            Object[] expectedArgs,
            int expectedCount,
            int actualCount,
            String verificationMode,
            List<InvocationRecord> actualInvocations) {
        
        StringBuilder message = new StringBuilder();
        
        // Header with verification failure summary
        message.append("\n").append("=".repeat(80)).append("\n");
        message.append("VERIFICATION FAILURE\n");
        message.append("=".repeat(80)).append("\n");
        
        // Expected vs Actual
        message.append(String.format("Expected: %s (%d invocations)\n", verificationMode, expectedCount));
        message.append(String.format("Actual:   %d invocations\n", actualCount));
        message.append("\n");
        
        // Method and context details
        message.append("Target Method:\n");
        message.append(String.format("  %s.%s(%s)\n", 
            method.getDeclaringClass().getSimpleName(),
            method.getName(),
            formatMethodParameters(method)));
        message.append("\n");
        
        // Expected arguments
        message.append("Expected Arguments:\n");
        if (expectedArgs != null && expectedArgs.length > 0) {
            message.append(String.format("  %s\n", formatArguments(expectedArgs)));
        } else {
            message.append("  (no arguments)\n");
        }
        message.append("\n");
        
        // Context information
        message.append("Context:\n");
        message.append(String.format("  %s\n", contextId != null ? contextId.toString() : "No context"));
        message.append("\n");
        
        // Mock information
        message.append("Mock Object:\n");
        message.append(String.format("  %s@%s\n", 
            mock.getClass().getSimpleName(), 
            Integer.toHexString(System.identityHashCode(mock))));
        message.append("\n");
        
        // Actual invocations details
        if (actualCount == 0) {
            message.append("Actual Invocations:\n");
            message.append("  NO INVOCATIONS RECORDED\n");
            message.append("  - Check if the method was actually called\n");
            message.append("  - Verify the correct context is set\n");
            message.append("  - Ensure the mock object is being used\n");
        } else {
            message.append(String.format("Actual Invocations (%d):\n", actualCount));
            for (int i = 0; i < actualInvocations.size() && i < 10; i++) { // Limit to first 10
                InvocationRecord record = actualInvocations.get(i);
                message.append(String.format("  %d. %s(%s) at %s\n",
                    i + 1,
                    record.getMethod().getName(),
                    formatArguments(record.getArguments()),
                    record.getTimestamp()));
            }
            if (actualInvocations.size() > 10) {
                message.append(String.format("  ... and %d more invocations\n", actualInvocations.size() - 10));
            }
        }
        message.append("\n");
        
        // Helpful suggestions
        message.append("Troubleshooting Tips:\n");
        if (actualCount == 0) {
            message.append("  - Ensure the method is called on the correct mock instance\n");
            message.append("  - Check that the context is properly set before the method call\n");
            message.append("  - Verify argument matchers are correct\n");
        } else if (actualCount < expectedCount) {
            message.append("  - Check if some calls were made in different contexts\n");
            message.append("  - Verify all expected method calls were executed\n");
        } else if (actualCount > expectedCount) {
            message.append("  - Look for unexpected additional method calls\n");
            message.append("  - Check if the method is called multiple times unintentionally\n");
        }
        
        message.append("=".repeat(80));
        
        return message.toString();
    }

    private static String formatMethodParameters(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length == 0) {
            return "";
        }
        
        StringBuilder params = new StringBuilder();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) params.append(", ");
            params.append(paramTypes[i].getSimpleName());
        }
        return params.toString();
    }

    private static String formatArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) formatted.append(", ");
            
            Object arg = args[i];
            if (arg == null) {
                formatted.append("null");
            } else if (arg instanceof String) {
                formatted.append("\"").append(arg).append("\"");
            } else if (arg instanceof Character) {
                formatted.append("'").append(arg).append("'");
            } else {
                formatted.append(arg.toString());
            }
        }
        return formatted.toString();
    }

    // Getters for programmatic access
    public Object getMock() { return mock; }
    public ContextID getContextId() { return contextId; }
    public Method getMethod() { return method; }
    public Object[] getExpectedArgs() { return expectedArgs; }
    public int getExpectedCount() { return expectedCount; }
    public int getActualCount() { return actualCount; }
    public String getVerificationMode() { return verificationMode; }
    public List<InvocationRecord> getActualInvocations() { return actualInvocations; }
}