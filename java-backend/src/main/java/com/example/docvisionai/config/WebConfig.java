package com.example.docvisionai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;



/**
 * Configuration class for setting up CORS (Cross-Origin Resource Sharing) in the application.
 * This class allows the application to handle requests from specified origins, enabling
 * interaction with the backend from external frontend applications.
 */
@Configuration
public class WebConfig {



    /**
     * Configures CORS settings for the application. This method defines the allowed origins, HTTP methods,
     * headers, and credentials for cross-origin requests.
     *
     * @return A WebMvcConfigurer bean that applies the CORS configuration.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://192.168.1.12:8000", "https://www.docvisionai.com")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "HEAD")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

}
