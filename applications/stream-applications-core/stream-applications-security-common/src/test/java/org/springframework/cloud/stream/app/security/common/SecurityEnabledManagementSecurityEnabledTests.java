/*
 * Copyright 2019-2022 the original author or authors.
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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Christian Tzolov
 * @author Artem Bilan
 * @author David Turanski
 * @since 3.0
 */
@TestPropertySource(properties = {
		"spring.main.web-application-type=servlet",
		"management.info.env.enabled=true",
		"management.endpoints.web.discovery.enabled=true",
		"management.endpoints.web.exposure.include=health,info,env,bindings",
		"info.name=MY TEST APP"})
public class SecurityEnabledManagementSecurityEnabledTests extends AbstractSecurityCommonTests {

	@Test
	@SuppressWarnings("rawtypes")
	public void testHealthEndpoint() {
		ResponseEntity<Map> response = this.restTemplate.getForEntity("/actuator/health", Map.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();
		Map health = response.getBody();
		assertThat(health.get("status")).isEqualTo("UP");
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testInfoEndpoint() {
		ResponseEntity<Map> response = this.restTemplate.getForEntity("/actuator/info", Map.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testDiscoveryEndpoint() {
		ResponseEntity<Map> response = this.restTemplate.getForEntity("/actuator", Map.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	// The ManagementWebSecurityAutoConfiguration exposes only Info and Health endpoint not Env!
	@Test
	@SuppressWarnings("rawtypes")
	public void testEnvEndpoint() {
		ResponseEntity<Map> response = this.restTemplate.getForEntity("/actuator/env", Map.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(response.hasBody()).isTrue();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testBindingsEndpoint() {
		ResponseEntity<List> response = this.restTemplate.getForEntity("/actuator/bindings", List.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testPostBindingsEndpoint() {
		/*
		 * With no auth, this results in some weird RestTemplate error related to HttpRetry.
		 */
		assertThatThrownBy(() ->
		this.restTemplate.postForEntity("/actuator/bindings/upper-in-0",
				Collections.singletonMap("state", "STOPPED"), Object.class));
	}
}
