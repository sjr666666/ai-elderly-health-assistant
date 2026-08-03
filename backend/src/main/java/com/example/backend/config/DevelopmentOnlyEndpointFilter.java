package com.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Blocks manual scheduler/test endpoints outside the local profile. */
@Component
public class DevelopmentOnlyEndpointFilter implements HandlerInterceptor {

    private final boolean enabled;

    public DevelopmentOnlyEndpointFilter(@Value("${app.development-endpoints-enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!enabled && request.getRequestURI().contains("/test/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }
        return true;
    }
}
