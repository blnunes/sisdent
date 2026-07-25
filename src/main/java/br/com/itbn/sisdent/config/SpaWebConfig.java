package br.com.itbn.sisdent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/{path:^(?!api|actuator|assets).*$}")
                .setViewName("forward:/index.html");
        registry.addViewController("/{path:^(?!api|actuator|assets).*$}/**")
                .setViewName("forward:/index.html");
    }
}