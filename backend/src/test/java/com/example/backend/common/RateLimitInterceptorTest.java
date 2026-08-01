package com.example.backend.common;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RateLimitInterceptorTest {

    @Test
    void usesAuthenticatedPrincipalInsteadOfClientSuppliedUserId() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(new com.fasterxml.jackson.databind.ObjectMapper());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(42L, null, java.util.List.of()));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ai/chat");
        request.addHeader("X-User-Id", "attacker");
        MockHttpServletResponse response = new MockHttpServletResponse();

        for (int i = 0; i < 10; i++) {
            assertTrue(interceptor.preHandle(request, response, mock(Object.class)));
        }
        assertFalse(interceptor.preHandle(request, response, mock(Object.class)));
        assertEquals(429, response.getStatus());
        SecurityContextHolder.clearContext();
    }
}
