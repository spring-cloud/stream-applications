/*
 * Copyright 2018-2024 the original author or authors.
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

import java.util.Collections;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;

/**
 * @author Christian Tzolov
 * @author Artem Bilan
 * @author David Turanski
 *
 * @since 2.1
 */
@Conditional(OnHttpCsrfOrSecurityDisabled.class)
@AutoConfiguration
@ConditionalOnClass(SecurityFilterChain.class)
@ConditionalOnMissingBean(SecurityFilterChain.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@AutoConfigureBefore({ManagementWebSecurityAutoConfiguration.class, SecurityAutoConfiguration.class})
@EnableConfigurationProperties(AppStarterWebSecurityAutoConfigurationProperties.class)
@EnableWebSecurity
public class AppStarterWebSecurityAutoConfiguration {

	/*
	 * Spring security will discover this provider. It grants the ADMIN ROLE to the admin
	 * user, if configured, otherwise it passes through the current authentication.
	 */
	@Bean
	AuthenticationProvider adminAuthenticationProvider(
			AppStarterWebSecurityAutoConfigurationProperties securityProperties) {
		return new AuthenticationProvider() {
			@Override
			public Authentication authenticate(Authentication authentication) throws AuthenticationException {
				if (securityProperties.isEnabled() &&
						StringUtils.hasText(securityProperties.getAdminUser()) &&
						securityProperties.getAdminUser().equals(authentication.getName()) &&
						StringUtils.hasText(securityProperties.getAdminPassword()) &&
						authentication.getCredentials() != null &&
						securityProperties.getAdminPassword().equals(authentication.getCredentials().toString())) {

					return new UsernamePasswordAuthenticationToken(securityProperties.getAdminUser(),
							securityProperties.getAdminPassword(), Collections.singletonList(
							new SimpleGrantedAuthority("ROLE_ADMIN")));
				}
				return authentication;
			}

			@Override
			public boolean supports(Class<?> aClass) {
				return UsernamePasswordAuthenticationToken.class.isAssignableFrom(aClass);
			}
		};
	}

	@Bean
	SecurityFilterChain appStarterWebSecurityFilterChain(HttpSecurity http,
			AppStarterWebSecurityAutoConfigurationProperties securityProperties) throws Exception {

		if (!securityProperties.isCsrfEnabled()) {
			http.csrf(AbstractHttpConfigurer::disable);
		}
		else {
			/*
			 * See https://stackoverflow.com/questions/51079564/spring-security-antmatchers-not-being-
			 * applied-on-post-requests-and-only-works-wi/51088555
			 */
			http.csrf((csrfConfigurer) ->
					csrfConfigurer.ignoringRequestMatchers(MethodAwareEndpointRequest.toAnyEndpoint(HttpMethod.POST)));
		}
		if (securityProperties.isEnabled()) {
			http.authorizeHttpRequests((authorizeHttpRequests) ->
							authorizeHttpRequests
									.requestMatchers(MethodAwareEndpointRequest.toAnyEndpoint(HttpMethod.POST)).hasRole("ADMIN")
									.requestMatchers(EndpointRequest.toLinks()).permitAll()
									.requestMatchers(EndpointRequest.to("health", "info", "bindings")).permitAll()
									.requestMatchers(EndpointRequest.toAnyEndpoint()).authenticated()
					)
					.formLogin(Customizer.withDefaults())
					.httpBasic(Customizer.withDefaults());
		}
		else {
			http.authorizeHttpRequests((authorizeHttpRequests) ->
					authorizeHttpRequests.anyRequest().permitAll());
		}
		return http.build();
	}

	/**
	 * Extends {@link EndpointRequest} to allow HTTP methods to be specified on the request matcher.
	 */
	static class MethodAwareEndpointRequest {

		static RequestMatcher toAnyEndpoint(HttpMethod httpMethod) {
			final EndpointRequest.EndpointRequestMatcher matcher = EndpointRequest.toAnyEndpoint();
			return (request) -> {
				if (!httpMethod.toString().equals(request.getMethod())) {
					return false;
				}
				return matcher.matches(request);
			};
		}

	}

}
