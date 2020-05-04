/*
 * Copyright 2019 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Christian Tzolov
 * @author Artem Bilan
 *
 * @since 3.0
 */
@TestPropertySource(properties = {
		"spring.main.web-application-type=servlet",
		"management.endpoints.web.exposure.include=health,info,env",
		"info.name=MY TEST APP" })
public class SecurityEnabledManagementSecurityEnabledTests extends AbstractSecurityCommonTests {

	@Test
	@SuppressWarnings("rawtypes")
	public void testHealthEndpoint() {
		ResponseEntity<Map> response = this.restTemplate.getForEntity("/actuator/health", Map.class);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(response.hasBody());
		Map health = response.getBody();
		assertEquals("UP", health.get("status"));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testInfoEndpoint() {
		ResponseEntity<Map> response = this.restTemplate.getForEntity("/actuator/info", Map.class);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(response.hasBody());
		Map info = response.getBody();
		assertEquals("MY TEST APP", info.get("name"));
	}

	// The ManagementWebSecurityAutoConfiguration exposes only Info and Health endpoint not Env!
	@Test
	@SuppressWarnings("rawtypes")
	public void testEnvEndpoint() {
		ResponseEntity<Map> response = this.restTemplate.getForEntity("/actuator/env", Map.class);
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
		assertTrue(response.hasBody());
	}

}
