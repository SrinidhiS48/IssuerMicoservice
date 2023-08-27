package com.example.issuerms.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class UserConfiguration {
    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        // url of bookms to be added here
        return WebClient.builder().baseUrl("http://bookms");
    }

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }
}
