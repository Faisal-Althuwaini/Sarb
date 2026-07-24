package com.dronefleet.telemetry.security;

import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

// The WebSocket/STOMP endpoint bypasses the gateway entirely (see
// WebSocketConfig's javadoc), so the JWT has to be checked here instead, on
// the STOMP CONNECT frame - throwing from preSend rejects the connection
// with a STOMP ERROR frame before it ever reaches the broker.
@RequiredArgsConstructor
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtValidator jwtValidator;

	@Override
	public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
			return message;
		}

		String header = accessor.getFirstNativeHeader("Authorization");
		if (header == null || !header.startsWith(BEARER_PREFIX)
				|| jwtValidator.validate(header.substring(BEARER_PREFIX.length())).isEmpty()) {
			throw new MessagingException(message, "Missing or invalid JWT on STOMP CONNECT");
		}

		return message;
	}
}
