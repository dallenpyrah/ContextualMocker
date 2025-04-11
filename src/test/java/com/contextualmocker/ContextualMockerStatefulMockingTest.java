package com.contextualmocker;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class ContextualMockerStatefulMockingTest {

    @Test
    void testStatefulStubbingAndTransition() {
        StatefulService mock = ContextualMocker.mock(StatefulService.class);
        ContextID ctx = new StringContextId("CTX");

        ContextualMocker.given(mock)
            .forContext(ctx)
            .when(mock.login("user", "pass"))
            .whenStateIs(null)
            .willSetStateTo("LOGGED_IN")
            .thenReturn(true);

        ContextualMocker.given(mock)
            .forContext(ctx)
            .when(mock.getSecret())
            .whenStateIs("LOGGED_IN")
            .thenReturn("top-secret");

        ContextualMocker.given(mock)
            .forContext(ctx)
            .when(mock.logout())
            .whenStateIs("LOGGED_IN")
            .willSetStateTo("LOGGED_OUT")
            .thenReturn(true);

        ContextualMocker.given(mock)
            .forContext(ctx)
            .when(mock.getSecret())
            .whenStateIs("LOGGED_OUT")
            .thenThrow(new IllegalStateException("Not logged in"));

        ContextHolder.setContext(ctx);

        assertTrue(mock.login("user", "pass"));
        assertEquals("top-secret", mock.getSecret());
        assertTrue(mock.logout());
        Exception ex = assertThrows(IllegalStateException.class, () -> mock.getSecret());
        assertEquals("Not logged in", ex.getMessage());
    }

    @Test
    void testStateIsolationBetweenContexts() {
        StatefulService mock = ContextualMocker.mock(StatefulService.class);
        ContextID ctx1 = new StringContextId("CTX1");
        ContextID ctx2 = new StringContextId("CTX2");

        ContextualMocker.given(mock)
            .forContext(ctx1)
            .when(mock.login("a", "b"))
            .whenStateIs(null)
            .willSetStateTo("IN")
            .thenReturn(true);

        ContextualMocker.given(mock)
            .forContext(ctx2)
            .when(mock.login("a", "b"))
            .whenStateIs(null)
            .willSetStateTo("IN2")
            .thenReturn(true);

        ContextHolder.setContext(ctx1);
        assertTrue(mock.login("a", "b"));

        ContextualMocker.given(mock)
            .forContext(ctx1)
            .when(mock.getSecret())
            .whenStateIs("IN")
            .thenReturn("ctx1-secret");

        ContextualMocker.given(mock)
            .forContext(ctx2)
            .when(mock.getSecret())
            .whenStateIs(null)
            .thenReturn("ctx2-secret");

        ContextHolder.setContext(ctx1);
        assertEquals("ctx1-secret", mock.getSecret());
        
        ContextHolder.setContext(ctx2);
        assertEquals("ctx2-secret", mock.getSecret());
    }
}
