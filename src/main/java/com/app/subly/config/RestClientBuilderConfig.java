package com.app.subly.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientBuilderConfig {

    @Bean
    public RestClient.Builder createRestClientBuilder(){
        return RestClient.builder();
    }
}
