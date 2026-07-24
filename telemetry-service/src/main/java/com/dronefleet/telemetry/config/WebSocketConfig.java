package com.dronefleet.telemetry.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;

import com.dronefleet.telemetry.security.StompAuthChannelInterceptor;

/**
 * STOMP over SockJS, moved from simulator-service in Phase 3 - telemetry-service
 * is now the sole thing the frontend holds a WebSocket connection to, relaying
 * both {@code /topic/telemetry} and {@code /topic/alerts}.
 *
 * Since Phase 6: this endpoint sits outside the gateway (Spring Cloud
 * Gateway Server MVC can't proxy WebSocket upgrades), so the JWT is
 * checked directly on the STOMP CONNECT frame via
 * {@link StompAuthChannelInterceptor} instead of at the edge.
 */
@RequiredArgsConstructor
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

	@Value("${sarb.cors.allowed-origin}")
	private String allowedOrigin;

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws")
				.setAllowedOrigins(allowedOrigin)
				.withSockJS();
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic");
		registry.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(stompAuthChannelInterceptor);
	}
}
