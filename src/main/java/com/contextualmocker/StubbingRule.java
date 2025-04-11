package com.contextualmocker;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

class StubbingRule {
    private final Method method;
    private final Object[] expectedArguments; // For exact matching initially
    private final ContextualAnswer<?> answer;
    private final Object returnValue;
    private final Throwable throwable;
    private final boolean hasReturnValue;
    private final boolean hasThrowable;
    private final boolean hasAnswer;

    private StubbingRule(Method method, Object[] expectedArguments, ContextualAnswer<?> answer, Object returnValue, Throwable throwable) {
        this.method = Objects.requireNonNull(method, "Method cannot be null");
        this.expectedArguments = expectedArguments == null ? new Object[0] : expectedArguments.clone(); // Defensive copy
        this.answer = answer;
        this.returnValue = returnValue;
        this.throwable = throwable;

        this.hasAnswer = answer != null;
        this.hasReturnValue = returnValue != null && !hasAnswer; // Answer takes precedence
        this.hasThrowable = throwable != null && !hasAnswer && !hasReturnValue; // Lowest precedence

        if (!hasAnswer && !hasReturnValue && !hasThrowable) {
            // Default behavior if nothing else is specified might be null,
            // but let's require at least one action for clarity.
            // Or maybe default to null return? For now, let's assume one is set.
            // Revisit if default behavior needs refinement.
        }
    }

    static StubbingRule forReturnValue(Method method, Object[] expectedArguments, Object returnValue) {
        return new StubbingRule(method, expectedArguments, null, returnValue, null);
    }

    static StubbingRule forThrowable(Method method, Object[] expectedArguments, Throwable throwable) {
        return new StubbingRule(method, expectedArguments, null, null, throwable);
    }

    static StubbingRule forAnswer(Method method, Object[] expectedArguments, ContextualAnswer<?> answer) {
        return new StubbingRule(method, expectedArguments, answer, null, null);
    }

    boolean matches(Method invokedMethod, Object[] invokedArguments) {
        if (!method.equals(invokedMethod)) {
            return false;
        }
        // Simple exact argument matching for now
        return Arrays.equals(expectedArguments, invokedArguments);
        // TODO: Integrate more sophisticated argument matchers later
    }

    Object apply(ContextID contextId, Object mock, Method invokedMethod, Object[] invokedArguments) throws Throwable {
        if (hasAnswer) {
            return answer.answer(contextId, mock, invokedMethod, invokedArguments);
        } else if (hasThrowable) {
            throw throwable;
        } else if (hasReturnValue) {
            return returnValue;
        } else {
            // Default behavior if no rule action was defined (e.g., return null for objects)
            // This aligns with Mockito's default behavior.
            return getDefaultValue(method.getReturnType());
        }
    }

    private static Object getDefaultValue(Class<?> type) {
        if (type.isPrimitive()) {
            if (type == boolean.class) return false;
            if (type == byte.class) return (byte) 0;
            if (type == short.class) return (short) 0;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == float.class) return 0.0f;
            if (type == double.class) return 0.0d;
            if (type == char.class) return '\u0000';
        }
        return null; // Default for objects and void (though void won't use this return)
    }

    @Override
    public String toString() {
        String action;
        if (hasAnswer) action = "answer=" + answer;
        else if (hasThrowable) action = "throwable=" + throwable;
        else if (hasReturnValue) action = "returnValue=" + returnValue;
        else action = "default value";

        return "StubbingRule{" +
               "method=" + method.getName() +
               ", expectedArguments=" + Arrays.toString(expectedArguments) +
               ", action=" + action +
               '}';
    }
}