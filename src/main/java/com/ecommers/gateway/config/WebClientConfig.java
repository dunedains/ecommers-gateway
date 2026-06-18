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
            @Value("${services.user-url:http://localhost:8082}") String userUrl) {
        // Se usa el factory estatico en vez de inyectar WebClient.Builder: ese bean
        // no esta autoconfigurado con este conjunto de dependencias en Spring Boot 4.
        return WebClient.builder().baseUrl(userUrl).build();
    }
}
