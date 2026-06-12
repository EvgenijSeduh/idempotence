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

    public static final String BODY_TOO_LARGE_ATTRIBUTE =
            CachedBodyFilter.class.getName() + ".BODY_TOO_LARGE";

    private static final Logger log = LoggerFactory.getLogger(CachedBodyFilter.class);

    private final IdempotenceProperties properties;

    public CachedBodyFilter(IdempotenceProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (request instanceof CachedBodyHttpServletRequest) {
            filterChain.doFilter(request, response);
            return;
        }

        long maxBodySize = properties.getMaxBodySize();
        long contentLength = request.getContentLengthLong();

        if (maxBodySize > 0 && contentLength > maxBodySize) {
            request.setAttribute(BODY_TOO_LARGE_ATTRIBUTE, Boolean.TRUE);

            log.debug(
                    "Request body exceeds maxBodySize ({} bytes); idempotent request will be rejected: {}",
                    maxBodySize,
                    request.getRequestURI()
            );

            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);

        if (maxBodySize > 0 && cachedRequest.getCachedBodySize() > maxBodySize) {
            cachedRequest.setAttribute(BODY_TOO_LARGE_ATTRIBUTE, Boolean.TRUE);

            log.debug(
                    "Cached request body exceeds maxBodySize ({} bytes); idempotent request will be rejected: {}",
                    maxBodySize,
                    request.getRequestURI()
            );
        }

        filterChain.doFilter(cachedRequest, response);
    }
}
