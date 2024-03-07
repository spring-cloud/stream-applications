/*
 * Copyright 2019-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.app.security.common;

import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoints;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * @author Artem Bilan
 * @author David Turanski
 * @since 3.0
 */
@Conditional(OnHttpCsrfOrSecurityDisabled.class)
@AutoConfiguration
@ConditionalOnClass({Flux.class, EnableWebFluxSecurity.class, WebFilterChainProxy.class, WebFluxConfigurer.class})
@ConditionalOnMissingBean(WebFilterChainProxy.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@AutoConfigureBefore({ReactiveManagementWebSecurityAutoConfiguration.class,
		ReactiveSecurityAutoConfiguration.class,
		ReactiveUserDetailsServiceAutoConfiguration.class})
@EnableConfigurationProperties(AppStarterWebSecurityAutoConfigurationProperties.class)
public class AppStarterWebFluxSecurityAutoConfiguration {

	@Autowired
	private PathMappedEndpoints pathMappedEndpoints;

	@Bean
	@ConditionalOnMissingBean(ReactiveUserDetailsService.class)
	public MapReactiveUserDetailsService userDetailsService(SecurityProperties securityProperties,
			AppStarterWebSecurityAutoConfigurationProperties streamAppSecurityProperties) {
		UserDetails primaryUser = User.builder()
				.username(securityProperties.getUser().getName())
				.password(securityProperties.getUser().getPassword())
				.passwordEncoder(PasswordEncoderFactories.createDelegatingPasswordEncoder()::encode)
				.roles(securityProperties.getUser().getRoles()
						.toArray(new String[0]))
				.build();

		if (StringUtils.hasText(streamAppSecurityProperties.getAdminPassword()) &&
				StringUtils.hasText(streamAppSecurityProperties.getAdminUser())) {
			UserDetails user = User.builder()
					.username(streamAppSecurityProperties.getAdminUser())
					.password(streamAppSecurityProperties.getAdminPassword())
					.roles("ADMIN")
					.passwordEncoder(PasswordEncoderFactories.createDelegatingPasswordEncoder()::encode)
					.build();
			return new MapReactiveUserDetailsService(primaryUser, user);
		}
		return new MapReactiveUserDetailsService(primaryUser);
	}

	@Bean
	public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
			AppStarterWebSecurityAutoConfigurationProperties securityProperties) {

		String managementPath = pathMappedEndpoints.getBasePath() + "/**";
		if (!securityProperties.isCsrfEnabled()) {
			http.csrf(ServerHttpSecurity.CsrfSpec::disable);
		}
		else {
			/*
			 * See https://stackoverflow.com/questions/51079564/spring-security-antmatchers-not-being-
			 * applied-on-post-requests-and-only-works-wi/51088555
			 */
			http.csrf((csrfSpec) ->
					csrfSpec.requireCsrfProtectionMatcher(exchange ->
							new AntPathMatcher().match(managementPath, exchange.getRequest().getPath().value())
									? ServerWebExchangeMatcher.MatchResult.notMatch()
									: ServerWebExchangeMatcher.MatchResult.match()));
		}
		if (!securityProperties.isEnabled()) {
			http.authorizeExchange((authorizeExchangeSpec) -> authorizeExchangeSpec.anyExchange().permitAll());
		}
		else {
			http.authorizeExchange((authorizeExchangeSpec) -> authorizeExchangeSpec
							.pathMatchers(HttpMethod.POST, managementPath).hasRole("ADMIN")
							.pathMatchers(
									pathMappedEndpoints.getBasePath(),
									pathMappedEndpoints.getPath(EndpointId.of("health")),
									pathMappedEndpoints.getPath(EndpointId.of("info")),
									pathMappedEndpoints.getPath(EndpointId.of("bindings")))
							.permitAll().anyExchange().authenticated())
					.formLogin(Customizer.withDefaults())
					.httpBasic(Customizer.withDefaults());

		}
		return http.build();
	}

}
