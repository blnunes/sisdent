package br.com.itbn.sisdent.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTest {
    @Test
    void acceptsValidInboundIdAndClearsMdcAfterCompletion() throws Exception {
        RequestCorrelationFilter filter = new RequestCorrelationFilter(new SimpleMeterRegistry());
        MockHttpServletRequest request = request();
        request.addHeader(CorrelationIds.HEADER, "edge-42.trace_1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                assertThat(MDC.get(CorrelationIds.MDC_KEY)).isEqualTo("edge-42.trace_1"));

        assertThat(response.getHeader(CorrelationIds.HEADER)).isEqualTo("edge-42.trace_1");
        assertThat(MDC.get(CorrelationIds.MDC_KEY)).isNull();
    }

    @Test
    void generatesUuidWhenInboundIdIsAbsent() throws Exception {
        RequestCorrelationFilter filter = new RequestCorrelationFilter(new SimpleMeterRegistry());
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request(), response, (ignoredRequest, ignoredResponse) -> { });
        assertThat(response.getHeader(CorrelationIds.HEADER))
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void replacesInvalidIdAndDoesNotUseSensitiveRequestDataAsMetricTags() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RequestCorrelationFilter filter = new RequestCorrelationFilter(registry);
        MockHttpServletRequest request = request();
        request.addHeader(CorrelationIds.HEADER, "bad value\nAuthorization: secret-token");
        request.setContent("password=secret-token&clinicalNote=private".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });

        assertThat(response.getHeader(CorrelationIds.HEADER))
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(registry.get("sisdent.http.request.count").counter().getId().getTags())
                .allSatisfy(tag -> assertThat(tag.getValue()).doesNotContain("secret-token", "private", "password"));
    }

    private MockHttpServletRequest request() { return new MockHttpServletRequest("POST", "/api/patients/123"); }
}
