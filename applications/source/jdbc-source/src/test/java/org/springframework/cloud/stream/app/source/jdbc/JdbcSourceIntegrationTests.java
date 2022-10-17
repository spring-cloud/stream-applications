/*
 * Copyright 2020-2020 the original author or authors.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.supplier.jdbc.JdbcSupplierConfiguration;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Soby Chacko
 * @author Artem Bilan
 * @author Chris Bono
 */
@SpringBootTest(properties = "spring.cloud.function.definition=jdbcSupplier")
@DirtiesContext
public class JdbcSourceIntegrationTests {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	protected JdbcOperations jdbcOperations;

	@Autowired
	private OutputDestination outputDestination;

	@Nullable
	protected Message<?> receiveMessageMaybeNull(long timeoutMillis) {
		return this.outputDestination.receive(timeoutMillis, "jdbcSupplier-out-0");
	}

	protected Message<?> receiveMessage(long timeoutMillis) {
		Message<?> received = this.outputDestination.receive(timeoutMillis, "jdbcSupplier-out-0");
		assertThat(received).isNotNull();
		assertThat(received.getPayload().getClass()).isEqualTo(byte[].class);
		return received;
	}

	protected <T> T extractPayload(Message<?> message, Class<T> type) {
		try {
			return this.objectMapper.readValue(new String((byte[]) message.getPayload()), type);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	protected <T> T extractPayload(Message<?> message, JavaType type) {
		try {
			return this.objectMapper.readValue(new String((byte[]) message.getPayload()), type);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@SpringBootApplication
	@Import({ JdbcSupplierConfiguration.class, TestChannelBinderConfiguration.class })
	public static class JdbcSourceTestApplication {
	}
}
