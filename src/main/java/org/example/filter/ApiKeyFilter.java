package org.example.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
@Component
@Order(1)
public class ApiKeyFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    // Endpoints that require API key authentication
    private static final Set<String> PROTECTED_PREFIXES = Set.of(
            "/api/v1/notify",
            "/api/v1/sse/broadcast"
    );

    @Value("${INTERNAL_API_KEY:}")
    private String internalApiKey;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        if (isProtected(path)) {
            if (internalApiKey.isBlank()) {
                // API key not configured — allow through but warn
                log.warn("INTERNAL_API_KEY not set, skipping auth for {}", path);
                chain.doFilter(request, response);
                return;
            }

            String providedKey = httpRequest.getHeader(API_KEY_HEADER);
            if (!internalApiKey.equals(providedKey)) {
                log.warn("Unauthorized access attempt to {} from {}", path, httpRequest.getRemoteAddr());
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"error\":\"Unauthorized\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isProtected(String path) {
        return PROTECTED_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
