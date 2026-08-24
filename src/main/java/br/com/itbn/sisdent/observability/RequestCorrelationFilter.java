package br.com.itbn.sisdent.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/** Adds a request-scoped correlation ID and records only safe HTTP execution dimensions. */
@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestCorrelationFilter.class);
    public static final String TRANSPORT = "transport";
    private final MeterRegistry meterRegistry;

    public RequestCorrelationFilter(MeterRegistry meterRegistry) { this.meterRegistry = meterRegistry; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = CorrelationIds.from(request.getHeader(CorrelationIds.HEADER));
        long startedAt = System.nanoTime();
        MDC.put(CorrelationIds.MDC_KEY, correlationId);
        // Keep only a bounded, normalized value in MDC. Request URIs can contain identifiers.
        MDC.put(TRANSPORT, transport(request));
        response.setHeader(CorrelationIds.HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.nanoTime() - startedAt;
            String route = route(request);
            String transport = transport(request);
            String status = Integer.toString(response.getStatus());
            Timer.builder("sisdent.http.request.duration").description("HTTP request duration")
                    .tags(TRANSPORT, transport, "route", route, "status", status).register(meterRegistry)
                    .record(duration, TimeUnit.NANOSECONDS);
            meterRegistry.counter("sisdent.http.request.count", TRANSPORT, transport, "route", route, "status", status)
                    .increment();
            LOGGER.info("request_completed method={} route={} status={} durationMs={} correlationId={}",
                    request.getMethod(), route, status, TimeUnit.NANOSECONDS.toMillis(duration), correlationId);
            response.setHeader(CorrelationIds.HEADER, correlationId);
            MDC.remove(CorrelationIds.MDC_KEY);
            MDC.remove(TRANSPORT);
        }
    }

    private String transport(HttpServletRequest request) {
        return "/graphql".equals(request.getRequestURI()) ? "graphql" : "rest";
    }

    private String route(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern != null) return pattern.toString();
        String path = request.getRequestURI();
        if ("/graphql".equals(path) || "/actuator/health".equals(path)) return path;
        if (path.startsWith("/api/")) return "/api/**";
        if (path.startsWith("/actuator/")) return "/actuator/**";
        return "/other";
    }
}
