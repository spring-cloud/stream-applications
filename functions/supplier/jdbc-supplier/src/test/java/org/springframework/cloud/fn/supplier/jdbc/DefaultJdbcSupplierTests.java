/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.cloud.fn.supplier.jdbc;

import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = "jdbc.supplier.query=select id, name from test order by id")
@DirtiesContext
public class DefaultJdbcSupplierTests {

	@Autowired
	Supplier<Flux<Message<?>>> jdbcSupplier;

	@Test
	void testExtraction() {
		final Flux<Message<?>> messageFlux = jdbcSupplier.get();
		StepVerifier stepVerifier =
				StepVerifier.create(messageFlux)
						.assertNext((message) ->
								assertThat(message)
										.satisfies((msg) -> assertThat(msg)
												.extracting(Message::getPayload)
												.matches(o -> {
													Map map = (Map)o;
													return map.get("ID").equals(1L) && map.get("NAME").equals("Bob");
												})
										))
						.assertNext((message) ->
								assertThat(message)
										.satisfies((msg) -> assertThat(msg)
												.extracting(Message::getPayload)
												.matches(o -> {
													Map map = (Map)o;
													return map.get("ID").equals(2L) && map.get("NAME").equals("Jane");
												})
										))
						.assertNext((message) ->
								assertThat(message)
										.satisfies((msg) -> assertThat(msg)
												.extracting(Message::getPayload)
												.matches(o -> {
													Map map = (Map)o;
													return map.get("ID").equals(3L) && map.get("NAME").equals("John");
												})
										))
						.thenCancel()
						.verifyLater();
		stepVerifier.verify();
	}

	@SpringBootApplication
	static class TestApplication {
	}
}
