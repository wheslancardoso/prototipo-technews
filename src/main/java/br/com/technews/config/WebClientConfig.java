package br.com.technews.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
            .build()
            .mutate();
    }

    @Bean
    public WebClient newsCollectionWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
            .defaultHeader("User-Agent", "TechNews-Bot/1.0 (News Collection Service)")
            .defaultHeader("Accept", "application/rss+xml, application/xml, text/xml, application/json")
            .build();
    }
}