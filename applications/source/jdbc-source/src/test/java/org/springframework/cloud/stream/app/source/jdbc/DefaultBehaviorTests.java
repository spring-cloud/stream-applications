/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.stream.app.source.jdbc;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Soby Chacko
 * @author Chris Bono
 */
@TestPropertySource(properties = "jdbc.supplier.query=select id, name from test order by id")
class DefaultBehaviorTests extends JdbcSourceIntegrationTests {

	@Test
	void testExtraction() {
		Message<?> received = receiveMessage(10_000);
		Map<?, ?> payload = extractPayload(received, Map.class);
		assertThat(payload.get("ID")).isEqualTo(1);

		received = receiveMessage(10_000);
		payload = extractPayload(received, Map.class);
		assertThat(payload.get("ID")).isEqualTo(2);

		received = receiveMessage(10_000);
		payload = extractPayload(received, Map.class);
		assertThat(payload.get("ID")).isEqualTo(3);
	}
}
