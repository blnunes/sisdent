package br.com.itbn.sisdent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward client-side route paths to the SPA shell. Routes such as
        // "/login" or "/users" have no file extension, so they are matched and
        // forwarded to /index.html. Hashed static assets (main-*.js, styles-*.css,
        // favicon.ico) keep their extensions and are served directly by the static
        // resource handler. API and actuator paths are excluded so they reach their
        // own controllers. Matching only extension-less paths also avoids an
        // infinite forward loop: /index.html has a dot, so it is not re-captured.
        registry.addViewController("/{path:^(?!api|actuator|swagger-ui|swagger-resources|v3|webjars|configuration)[^.]*$}")
                .setViewName("forward:/index.html");
        // Match the first path segment here. Putting /** first allows it to consume
        // "/api" and incorrectly forwards unknown API endpoints such as
        // "/api/procedures" to Angular with HTTP 200.
        registry.addViewController("/{path:^(?!api$|actuator$|i18n$|swagger-ui$|swagger-resources$|v3$|webjars$|configuration$)[^.]+}/**")
                .setViewName("forward:/index.html");
    }
}
