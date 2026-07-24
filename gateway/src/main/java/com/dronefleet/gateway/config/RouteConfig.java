package com.dronefleet.gateway.config;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import com.dronefleet.gateway.security.AuthFilter;

// Every backend REST API sits behind this gateway now: the frontend only
// ever talks to port 8080. /api/auth/** is the one public route (that's
// where a token comes from in the first place); everything else requires
// a valid Bearer JWT via AuthFilter. Note: the WebSocket/STOMP endpoint on
// telemetry-service is NOT routed here - Spring Cloud Gateway Server MVC
// (the servlet/non-reactive variant, chosen because the rest of this
// gateway is plain WebMVC) doesn't support WebSocket proxying, only the
// WebFlux gateway does. The frontend connects to telemetry-service's /ws
// directly instead, with the JWT checked at the STOMP CONNECT frame.
@Configuration
public class RouteConfig {

	@Value("${sarb.services.auth-service}")
	private String authServiceUri;

	@Value("${sarb.services.alert-service}")
	private String alertServiceUri;

	@Value("${sarb.services.mission-service}")
	private String missionServiceUri;

	@Value("${sarb.services.rag-service}")
	private String ragServiceUri;

	@Bean
	public RouterFunction<ServerResponse> authRoute() {
		return route("auth-service")
				.route(RequestPredicates.path("/api/auth/**"), http())
				.before(uri(authServiceUri))
				.build();
	}

	@Bean
	public RouterFunction<ServerResponse> alertRoute(AuthFilter authFilter) {
		return route("alert-service")
				.route(RequestPredicates.path("/api/alerts/**"), http())
				.filter(authFilter)
				.before(uri(alertServiceUri))
				.build();
	}

	@Bean
	public RouterFunction<ServerResponse> missionRoute(AuthFilter authFilter) {
		return route("mission-service")
				.route(RequestPredicates.path("/api/missions/**"), http())
				.filter(authFilter)
				.before(uri(missionServiceUri))
				.build();
	}

	@Bean
	public RouterFunction<ServerResponse> assistantRoute(AuthFilter authFilter) {
		return route("rag-service")
				.route(RequestPredicates.path("/api/assistant/**"), http())
				.filter(authFilter)
				.before(uri(ragServiceUri))
				.build();
	}
}
