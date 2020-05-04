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

package org.springframework.cloud.fn.consumer.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Eric Bottard
 * @author Thomas Risberg
 * @author Artem Bilan
 * @author Robert St. John
 * @author Oliver Flasch
 * @author Soby Chacko
 * @author Szabolcs Stremler
 */
@TestPropertySource(properties = "jdbc.consumer.columns=a,b")
public class JsonStringPayloadInsertTests extends JdbcConsumerApplicationTests {

	@Test
	public void testInsertion() {
		String stringA = "{\"a\": \"hello1\", \"b\": 42}";
		String stringB = "{\"a\": \"hello2\", \"b\": null}";
		String stringC = "{\"a\": \"hello3\"}";
		final Message<String> message1 = MessageBuilder.withPayload(stringA).build();
		jdbcConsumer.accept(message1);
		final Message<String> message2 = MessageBuilder.withPayload(stringB).build();
		jdbcConsumer.accept(message2);
		final Message<String> message3 = MessageBuilder.withPayload(stringC).build();
		jdbcConsumer.accept(message3);
		assertThat(jdbcOperations.queryForObject(
				"select count(*) from messages where a = ? and b = ?",
				Integer.class, "hello1", 42)).isEqualTo(1);
		assertThat(jdbcOperations.queryForObject(
				"select count(*) from messages where a = ? and b IS NULL",
				Integer.class, "hello2")).isEqualTo(1);
		assertThat(jdbcOperations.queryForObject(
				"select count(*) from messages where a = ? and b IS NULL",
				Integer.class, "hello3")).isEqualTo(1);
	}

}
