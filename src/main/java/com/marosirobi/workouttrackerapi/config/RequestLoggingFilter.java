package com.marosirobi.workouttrackerapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID = "requestId";
    public static final String USER_ID = "userId";

    private static final long SLOW_REQUEST_MS = 1000;
    private static final List<String> SKIPPED_PATHS =
            List.of("/h2-console", "/swagger-ui", "/v3/api-docs");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return SKIPPED_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        MDC.put(REQUEST_ID, UUID.randomUUID().toString().substring(0, 8));

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            String userId = MDC.get(USER_ID);

            if (durationMs >= SLOW_REQUEST_MS) {
                log.warn("{} {} {} {}ms user={} (slow)", request.getMethod(), request.getRequestURI(),
                        response.getStatus(), durationMs, userId != null ? userId : "-");
            } else {
                log.info("{} {} {} {}ms user={}", request.getMethod(), request.getRequestURI(),
                        response.getStatus(), durationMs, userId != null ? userId : "-");
            }

            MDC.clear();
        }
    }
}
