package com.idempotence.idempotencelib.spring.web;

import com.idempotence.idempotencelib.spring.properties.IdempotenceProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

public class CachedBodyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CachedBodyFilter.class);

    private final IdempotenceProperties properties;

    public CachedBodyFilter(IdempotenceProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request instanceof CachedBodyHttpServletRequest) {
            filterChain.doFilter(request, response);
            return;
        }
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        long maxBodySize = properties.getMaxBodySize();
        if (maxBodySize > 0 && cachedRequest.getCachedBodyAsString().getBytes().length > maxBodySize) {
            log.debug("Request body exceeds maxBodySize ({} bytes); skipping body caching for {}",
                    maxBodySize, request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }
        filterChain.doFilter(cachedRequest, response);
    }
}
