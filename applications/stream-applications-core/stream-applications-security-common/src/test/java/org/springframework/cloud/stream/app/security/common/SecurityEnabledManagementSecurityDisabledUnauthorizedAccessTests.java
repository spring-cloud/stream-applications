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

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 * @author Artem Bilan
 * @Author David Turanski
 * @since 3.0
 */
@TestPropertySource(properties = {
		"spring.main.web-application-type=servlet",
		"spring.autoconfigure.exclude=" +
				"org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
				+ ",org.springframework.cloud.stream.app.security.common.AppStarterWebSecurityAutoConfiguration",
		"management.endpoints.web.exposure.include=health,info,bindings,env" })
public class SecurityEnabledManagementSecurityDisabledUnauthorizedAccessTests extends AbstractSecurityCommonTests {

	@Test
	@SuppressWarnings("rawtypes")
	public void testHealthEndpoint() {
		ResponseEntity<Map> response = this.restTemplate.getForEntity("/actuator/health", Map.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		//Response no longer contains an error message.
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testInfoEndpoint() {
		ResponseEntity<Map> response = this.restTemplate.getForEntity("/actuator/info", Map.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testBindingsEndpoint() {
		ResponseEntity<?> response = this.restTemplate.getForEntity("/actuator/bindings", Object.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testEnvEndpoint() {
		ResponseEntity<Map> response = this.restTemplate.getForEntity("/actuator/env", Map.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

}
