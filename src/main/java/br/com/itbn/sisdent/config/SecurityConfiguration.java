package br.com.itbn.sisdent.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import br.com.itbn.sisdent.controller.RestExceptionTranslator;
import br.com.itbn.sisdent.observability.RequestCorrelationFilter;

import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            AccountStateJwtFilter accountStateJwtFilter,
            RequestCorrelationFilter requestCorrelationFilter,
            RestExceptionTranslator exceptionTranslator) {
        http
                // JWTs are sent in Authorization headers. CSRF is nevertheless enabled for any
                // unsafe request that carries a session cookie, so a future cookie-based endpoint
                // cannot silently inherit the API's bearer-token threat model.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .requireCsrfProtectionMatcher(SecurityConfiguration::requiresCsrfProtection))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/auth/login",
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/configuration/**",
                                "/h2-console/**",
                                "/i18n/**").permitAll()
                        // Foundational catalogues are platform-wide, not organization-scoped.
                        // They require platform-administrator authority.
                        .requestMatchers("/api/specialities/**", "/api/addresses/**", "/api/countries/**",
                                "/api/administrative-divisions/**", "/api/states/**",
                                "/api/platform/catalog-translations/**")
                        .hasAuthority("ROLE_PLATFORM_ADMIN")
                        // GraphQL contains both platform and organization-scoped reads. Individual
                        // resolvers delegate to services, which remain the authorization authority.
                        .requestMatchers("/graphql")
                        .authenticated()
                        // Single-page application shell, static assets, and client-side routes.
                        // The SPA bundle contains no secrets; data authorization is enforced on
                        // the /api/** matchers above. Client-side route protection is handled by
                        // the Angular authGuard/adminGuard.
                        .requestMatchers(HttpMethod.GET,
                                "/",
                                "/index.html",
                                "/favicon.ico",
                                "/{path:^(?!api|actuator).*$}",
                                "/{path:^(?!api|actuator).*$}/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint((request, response, exception) -> exceptionTranslator.authentication(request, response)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> exceptionTranslator.authentication(request, response))
                        .accessDeniedHandler((request, response, exception) -> exceptionTranslator.authorization(request, response)))
                .addFilterBefore(requestCorrelationFilter, SecurityContextHolderFilter.class)
                .addFilterAfter(accountStateJwtFilter, BearerTokenAuthenticationFilter.class)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    FilterRegistrationBean<AccountStateJwtFilter> disableContainerRegistration(
            AccountStateJwtFilter filter) {
        FilterRegistrationBean<AccountStateJwtFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<RequestCorrelationFilter> disableCorrelationContainerRegistration(
            RequestCorrelationFilter filter) {
        FilterRegistrationBean<RequestCorrelationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    SecretKey jwtSecretKey(@Value("${sisdent.security.jwt.secret}") String secret) {
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT secret must contain at least 32 characters");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey secretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey secretKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtIssuerValidator("sisdent")));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("authorities");
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter authenticationConverter =
                new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return authenticationConverter;
    }

    static boolean requiresCsrfProtection(jakarta.servlet.http.HttpServletRequest request) {
        if (HttpMethod.GET.matches(request.getMethod()) || HttpMethod.HEAD.matches(request.getMethod())
                || HttpMethod.OPTIONS.matches(request.getMethod()) || request.getCookies() == null) {
            return false;
        }
        return java.util.Arrays.stream(request.getCookies())
                .anyMatch(cookie -> "JSESSIONID".equals(cookie.getName()));
    }
}
