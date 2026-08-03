package com.example.backend.common;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/** Adds a bounded request correlation ID to logs and responses. */
@Component
public class RequestIdFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader(HEADER);
        if (!StringUtils.hasText(requestId) || requestId.length() > 100) {
            requestId = UUID.randomUUID().toString();
        }
        try (MDC.MDCCloseable ignored = MDC.putCloseable("requestId", requestId)) {
            response.setHeader(HEADER, requestId);
            filterChain.doFilter(request, response);
        }
    }
}
