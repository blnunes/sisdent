package br.com.itbn.sisdent.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigurationTest {

    @Test
    void requiresCsrfForUnsafeRequestsAuthenticatedByASessionCookie() {
        MockHttpServletRequest request = request(HttpMethod.POST);
        request.setCookies(new Cookie("JSESSIONID", "session"));

        assertThat(SecurityConfiguration.requiresCsrfProtection(request)).isTrue();
    }

    @Test
    void doesNotRequireCsrfForBearerApiRequestsOrSafeMethods() {
        assertThat(SecurityConfiguration.requiresCsrfProtection(request(HttpMethod.PUT))).isFalse();

        MockHttpServletRequest request = request(HttpMethod.GET);
        request.setCookies(new Cookie("JSESSIONID", "session"));
        assertThat(SecurityConfiguration.requiresCsrfProtection(request)).isFalse();
    }

    private static MockHttpServletRequest request(HttpMethod method) {
        return new MockHttpServletRequest(method.name(), "/api/account/settings");
    }
}
