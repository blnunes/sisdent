package br.com.itbn.sisdent.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/auth/login",
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/users/me/password")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/**")
                        .access(hasAnyPermission("READ_USERS", "MAINTAIN_USERS"))
                        .requestMatchers(HttpMethod.POST, "/api/users/**")
                        .access(hasAnyPermission("MAINTAIN_USERS"))
                        .requestMatchers(HttpMethod.PUT, "/api/users/**")
                        .access(hasAnyPermission("MAINTAIN_USERS"))
                        .requestMatchers(HttpMethod.PATCH, "/api/users/**")
                        .access(hasAnyPermission("MAINTAIN_USERS"))
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**")
                        .access(hasAnyPermission("MAINTAIN_USERS"))
                        .requestMatchers(HttpMethod.GET, "/api/patients/**")
                        .access(hasAnyPermission("READ_PATIENTS", "MAINTAIN_PATIENTS"))
                        .requestMatchers(HttpMethod.POST, "/api/patients/**")
                        .access(hasAnyPermission("MAINTAIN_PATIENTS"))
                        .requestMatchers(HttpMethod.PUT, "/api/patients/**")
                        .access(hasAnyPermission("MAINTAIN_PATIENTS"))
                        .requestMatchers(HttpMethod.DELETE, "/api/patients/**")
                        .access(hasAnyPermission("MAINTAIN_PATIENTS"))
                        .requestMatchers(HttpMethod.GET, "/api/specialities/**")
                        .access(hasAnyPermission("READ_SPECIALITIES", "MAINTAIN_SPECIALITIES"))
                        .requestMatchers(HttpMethod.POST, "/api/specialities/**")
                        .access(hasAnyPermission("MAINTAIN_SPECIALITIES"))
                        .requestMatchers(HttpMethod.PUT, "/api/specialities/**")
                        .access(hasAnyPermission("MAINTAIN_SPECIALITIES"))
                        .requestMatchers(HttpMethod.DELETE, "/api/specialities/**")
                        .access(hasAnyPermission("MAINTAIN_SPECIALITIES"))
                        .requestMatchers(HttpMethod.GET, "/api/addresses/**")
                        .access(hasAnyPermission("READ_ADDRESSES", "MAINTAIN_ADDRESSES"))
                        .requestMatchers(HttpMethod.GET, "/api/countries/**")
                        .access(hasAnyPermission("READ_COUNTRIES", "MAINTAIN_COUNTRIES"))
                        .requestMatchers(HttpMethod.GET, "/api/states/**")
                        .access(hasAnyPermission("READ_STATES", "MAINTAIN_STATES"))
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
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
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey));
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

    private AuthorizationManager<RequestAuthorizationContext> hasAnyPermission(String... permissions) {
        return (authentication, context) -> {
            Authentication auth = authentication.get();
            if (auth == null || !auth.isAuthenticated()) {
                return new AuthorizationDecision(false);
            }

            Set<String> authorities = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            boolean admin = authorities.contains("ROLE_ADMIN");
            boolean allowed = admin || authorities.stream().anyMatch(authority -> Arrays.asList(permissions).contains(authority));
            return new AuthorizationDecision(allowed);
        };
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
}
