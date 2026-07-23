package com.dronefleet.alert.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** CORS for the plain REST API (frontend fetches active alerts on mount, before Kafka events catch up). */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final String allowedOrigin;

	public WebConfig(@Value("${sarb.cors.allowed-origin}") String allowedOrigin) {
		this.allowedOrigin = allowedOrigin;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**").allowedOrigins(allowedOrigin);
	}
}
