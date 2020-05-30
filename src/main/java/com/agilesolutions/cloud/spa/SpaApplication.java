package com.agilesolutions.cloud.spa;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@SpringBootApplication
public class SpaApplication {
	
	@Value("${app.services.httpbin.url:https://httpbin.org/status}")
	private String httpbinUrl;
	

	@RequestMapping("/hystrixfallback")
	public String hystrixfallback() {
		return "This is a fallback";
	}
	
	/**
	 * All httpbin traffic goes here, make your own gateway here if you can not deal with webflux
	 * 
	 */
	@RequestMapping(value = { "/status/**" }, method = { RequestMethod.GET, RequestMethod.PUT, RequestMethod.POST })
	public ResponseEntity<?> partyRequests(@RequestBody(required = false) String body, HttpMethod method,
			HttpServletRequest request, HttpServletResponse response) {
		
		RestTemplate restTemplate = new RestTemplate();
		
		try {
			// compose new forwardable URL to down-stream service
			URI uri = UriComponentsBuilder.fromUri(new URI(partyServiceUrl)).path(request.getRequestURI()).query(request.getQueryString()).build(true).toUri();
			
			HttpHeaders headers = new HttpHeaders();

			// add Kerberos Constrained delegated ticket
			headers.add(HttpHeaders.AUTHORIZATION,"Negotiate " + "YOURTICKET");
			
			HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
			
			return restTemplate.exchange(uri, method, httpEntity, String.class);
		
		} catch (RestClientException | URISyntaxException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}

	}

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		//@formatter:off
		return builder.routes()
				// https://dzone.com/articles/spring-cloud-gateway-configuring-a-simple-route
				.route("java_route", r -> r.path("/java/**")
						.filters(f -> f.stripPrefix(1))
						.uri("http://httpbin.org/"))
				.route("path_route", r -> r.path("/get")
						.uri("http://httpbin.org"))
				.route("host_route", r -> r.host("*.myhost.org")
						.uri("http://httpbin.org"))
				.route("rewrite_route", r -> r.host("*.rewrite.org")
						.filters(f -> f.rewritePath("/foo/(?<segment>.*)",
								"/${segment}"))
						.uri("http://httpbin.org"))
				.route("hystrix_route", r -> r.host("*.hystrix.org")
						.filters(f -> f.hystrix(c -> c.setName("slowcmd")))
								.uri("http://httpbin.org"))
				.route("hystrix_fallback_route", r -> r.host("*.hystrixfallback.org")
						.filters(f -> f.hystrix(c -> c.setName("slowcmd").setFallbackUri("forward:/hystrixfallback")))
								.uri("http://httpbin.org"))
				.route("limit_route", r -> r
					.host("*.limited.org").and().path("/anything/**")
						.filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter())))
					.uri("http://httpbin.org"))
				.route("websocket_route", r -> r.path("/echo")
					.uri("ws://localhost:9000"))
				.build();
		//@formatter:on
	}

	@Bean
	RedisRateLimiter redisRateLimiter() {
		return new RedisRateLimiter(1, 2);
	}

	@Bean
	SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http) throws Exception {
		return http.httpBasic().and()
				.csrf().disable()
				.authorizeExchange()
				.pathMatchers("/anything/**").authenticated()
				.anyExchange().permitAll()
				.and()
				.build();
	}

	@Bean
	public MapReactiveUserDetailsService reactiveUserDetailsService() {
		UserDetails user = User.withDefaultPasswordEncoder().username("user").password("password").roles("USER").build();
		return new MapReactiveUserDetailsService(user);
	}

	public static void main(String[] args) {
		SpringApplication.run(SpaApplication.class, args);
	}

}
