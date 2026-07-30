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

import br.com.itbn.sisdent.model.Permission;

@Configuration
public class SecurityConfiguration {

    private static final String USER_RESOURCE = "/api/users/**";
    private static final String PATIENT_RESOURCE = "/api/patients/**";
    private static final String SPECIALITY_RESOURCE = "/api/specialities/**";
    private static final String MAINTAIN_USERS_PERMISSION = Permission.MAINTAIN_USERS.name();
    private static final String READ_USERS_PERMISSION = Permission.READ_USERS.name();
    private static final String MAINTAIN_PATIENTS_PERMISSION = Permission.MAINTAIN_PATIENTS.name();
    private static final String READ_PATIENTS_PERMISSION = Permission.READ_PATIENTS.name();
    private static final String MAINTAIN_SPECIALITIES_PERMISSION = Permission.MAINTAIN_SPECIALITIES.name();
    private static final String READ_PERMISSIONS_PERMISSION = Permission.READ_PERMISSIONS.name();
    private static final String MAINTAIN_PERMISSIONS_PERMISSION = Permission.MAINTAIN_PERMISSIONS.name();

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter) {
        http
                // This API is stateless and uses JWT Bearer tokens for auth, so CSRF protection is not required.
                // Keep CSRF disabled only while authentication relies on Authorization headers rather than cookies/sessions.
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
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/configuration/**",
                                "/h2-console/**",
                                "/i18n/**").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/users/me/password")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, USER_RESOURCE)
                        .access(hasAnyPermission(
                                READ_USERS_PERMISSION,
                                MAINTAIN_USERS_PERMISSION,
                                READ_PERMISSIONS_PERMISSION,
                                MAINTAIN_PERMISSIONS_PERMISSION))
                        .requestMatchers(HttpMethod.POST, USER_RESOURCE)
                        .access(hasAnyPermission(MAINTAIN_USERS_PERMISSION))
                        .requestMatchers(HttpMethod.PUT, "/api/users/*/permissions")
                        .access(hasAnyPermission(MAINTAIN_PERMISSIONS_PERMISSION))
                        .requestMatchers(HttpMethod.PUT, USER_RESOURCE)
                        .access(hasAnyPermission(MAINTAIN_USERS_PERMISSION))
                        .requestMatchers(HttpMethod.PATCH, USER_RESOURCE)
                        .access(hasAnyPermission(MAINTAIN_USERS_PERMISSION))
                        .requestMatchers(HttpMethod.DELETE, USER_RESOURCE)
                        .access(hasAnyPermission(MAINTAIN_USERS_PERMISSION))
                        .requestMatchers(HttpMethod.GET, PATIENT_RESOURCE)
                        .access(hasAnyPermission(READ_PATIENTS_PERMISSION, MAINTAIN_PATIENTS_PERMISSION))
                        .requestMatchers(HttpMethod.POST, PATIENT_RESOURCE)
                        .access(hasAnyPermission(MAINTAIN_PATIENTS_PERMISSION))
                        .requestMatchers(HttpMethod.PUT, PATIENT_RESOURCE)
                        .access(hasAnyPermission(MAINTAIN_PATIENTS_PERMISSION))
                        .requestMatchers(HttpMethod.DELETE, PATIENT_RESOURCE)
                        .access(hasAnyPermission(MAINTAIN_PATIENTS_PERMISSION))
                        .requestMatchers(HttpMethod.GET, SPECIALITY_RESOURCE)
                        .access(hasAnyPermission("READ_SPECIALITIES", MAINTAIN_SPECIALITIES_PERMISSION))
                        .requestMatchers(HttpMethod.POST, SPECIALITY_RESOURCE)
                        .access(hasAnyPermission(MAINTAIN_SPECIALITIES_PERMISSION))
                        .requestMatchers(HttpMethod.PUT, SPECIALITY_RESOURCE)
                        .access(hasAnyPermission(MAINTAIN_SPECIALITIES_PERMISSION))
                        .requestMatchers(HttpMethod.DELETE, SPECIALITY_RESOURCE)
                        .access(hasAnyPermission(MAINTAIN_SPECIALITIES_PERMISSION))
                        .requestMatchers(HttpMethod.GET, "/api/addresses/**")
                        .access(hasAnyPermission("READ_ADDRESSES", "MAINTAIN_ADDRESSES"))
                        .requestMatchers(HttpMethod.POST, "/api/addresses/**")
                        .access(hasAnyPermission("MAINTAIN_ADDRESSES"))
                        .requestMatchers(HttpMethod.PUT, "/api/addresses/**")
                        .access(hasAnyPermission("MAINTAIN_ADDRESSES"))
                        .requestMatchers(HttpMethod.DELETE, "/api/addresses/**")
                        .access(hasAnyPermission("MAINTAIN_ADDRESSES"))
                        .requestMatchers(HttpMethod.GET, "/api/countries/**")
                        .access(hasAnyPermission("READ_COUNTRIES", "MAINTAIN_COUNTRIES"))
                        .requestMatchers(HttpMethod.POST, "/api/countries/**")
                        .access(hasAnyPermission("MAINTAIN_COUNTRIES"))
                        .requestMatchers(HttpMethod.PUT, "/api/countries/**")
                        .access(hasAnyPermission("MAINTAIN_COUNTRIES"))
                        .requestMatchers(HttpMethod.DELETE, "/api/countries/**")
                        .access(hasAnyPermission("MAINTAIN_COUNTRIES"))
                        .requestMatchers(HttpMethod.GET, "/api/states/**")
                        .access(hasAnyPermission("READ_STATES", "MAINTAIN_STATES"))
                        .requestMatchers(HttpMethod.POST, "/api/states/**")
                        .access(hasAnyPermission("MAINTAIN_STATES"))
                        .requestMatchers(HttpMethod.PUT, "/api/states/**")
                        .access(hasAnyPermission("MAINTAIN_STATES"))
                        .requestMatchers(HttpMethod.DELETE, "/api/states/**")
                        .access(hasAnyPermission("MAINTAIN_STATES"))
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
