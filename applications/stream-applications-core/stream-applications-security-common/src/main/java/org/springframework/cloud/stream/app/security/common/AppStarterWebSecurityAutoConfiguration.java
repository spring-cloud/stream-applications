/*
 * Copyright 2018-2022 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
@Configuration
@ConditionalOnClass(WebSecurityConfigurerAdapter.class)
@ConditionalOnMissingBean(WebSecurityConfigurerAdapter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@AutoConfigureBefore({ ManagementWebSecurityAutoConfiguration.class, SecurityAutoConfiguration.class })
@EnableConfigurationProperties(AppStarterWebSecurityAutoConfigurationProperties.class)
@EnableWebSecurity
public class AppStarterWebSecurityAutoConfiguration {

	@Autowired
	private WebEndpointProperties webEndpointProperties;

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
	WebSecurityConfigurerAdapter appStarterWebSecurityConfigurerAdapter(
			AppStarterWebSecurityAutoConfigurationProperties securityProperties) {

		return new WebSecurityConfigurerAdapter() {
			@Override
			protected void configure(HttpSecurity http) throws Exception {
				if (!securityProperties.isCsrfEnabled()) {
					http.csrf().disable();
				}
				else {
					/*
					 * See https://stackoverflow.com/questions/51079564/spring-security-antmatchers-not-being-
					 * applied-on-post-requests-and-only-works-wi/51088555
					 */
					http.csrf().ignoringRequestMatchers(MethodAwareEndpointRequest.toAnyEndpoint(HttpMethod.POST));
				}
				if (securityProperties.isEnabled()) {
					http.authorizeRequests()
							.requestMatchers(MethodAwareEndpointRequest.toAnyEndpoint(HttpMethod.POST)).hasRole("ADMIN")
							.requestMatchers(EndpointRequest.toLinks()).permitAll()
							.requestMatchers(EndpointRequest.to("health", "info", "bindings")).permitAll()
							.requestMatchers(EndpointRequest.toAnyEndpoint()).authenticated()
							.and().formLogin().and().httpBasic();
				}
				else {
					http.authorizeRequests().anyRequest().permitAll();

				}
			}
		};
	}
	/**
	 * Extends {@link EndpointRequest} to allow HTTP methods to be specified on the request matcher.
	 */
	static class MethodAwareEndpointRequest {

		/**
		 * Returns a matcher that includes the specified {@link Endpoint actuator endpoints} and http method.
		 * For example: <pre class="code">
		 * EndpointRequest.to(HttpMethod.POST, "loggers")
		 * </pre>
		 * @param httpMethod the http method to include
		 * @param endpoints the endpoints to include
		 * @return the configured {@link RequestMatcher}
		 */
		static RequestMatcher to(HttpMethod httpMethod, String... endpoints) {
			final EndpointRequest.EndpointRequestMatcher matcher = EndpointRequest.to(endpoints);
			return (request) -> {
				if (!httpMethod.toString().equals(request.getMethod())) {
					return false;
				}
				return matcher.matches(request);
			};
		}

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
