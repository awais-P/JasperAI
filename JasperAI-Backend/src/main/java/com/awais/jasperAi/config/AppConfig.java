package com.awais.jasperAi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    // ADD THIS BEAN
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}