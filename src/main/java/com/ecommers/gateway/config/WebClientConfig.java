package com.ecommers.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /** Cliente HTTP hacia el microservicio de usuarios (validacion de credenciales y registro). */
    @Bean
    public WebClient userServiceWebClient(
            WebClient.Builder builder,
            @Value("${services.user-url:http://localhost:8082}") String userUrl) {
        return builder.baseUrl(userUrl).build();
    }
}
