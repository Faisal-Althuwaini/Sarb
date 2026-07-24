package com.dronefleet.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// The gateway is the ONLY service that talks to the browser now, so it's
// the only one that should set CORS headers. The proxied backend services
// (auth/alert/mission/rag) deliberately have no CORS config of their own -
// when they did, their response passed a second Access-Control-Allow-Origin
// header back through the gateway's own, and browsers reject responses with
// duplicate CORS headers (curl doesn't enforce CORS, so this only showed up
// once the frontend actually tried it).
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
