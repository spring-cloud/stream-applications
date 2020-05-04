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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
public class MapPayloadInsertTests extends JdbcConsumerApplicationTests {

	@Test
	public void testInsertion() {
		NamedParameterJdbcOperations namedParameterJdbcOperations = new NamedParameterJdbcTemplate(jdbcOperations);
		Map<String, Object> mapA = new HashMap<>();
		mapA.put("a", "hello1");
		mapA.put("b", 42);
		Map<String, Object> mapB = new HashMap<>();
		mapB.put("a", "hello2");
		mapB.put("b", null);
		Map<String, Object> mapC = new HashMap<>();
		mapC.put("a", "hello3");
		final Message<Map<String, Object>> message1 = MessageBuilder.withPayload(mapA).build();
		jdbcConsumer.accept(message1);
		final Message<Map<String, Object>> message2 = MessageBuilder.withPayload(mapB).build();
		jdbcConsumer.accept(message2);
		final Message<Map<String, Object>> message3 = MessageBuilder.withPayload(mapC).build();
		jdbcConsumer.accept(message3);
		assertThat(namedParameterJdbcOperations.queryForObject(
				"select count(*) from messages where a = :a and b = :b", mapA, Integer.class)).isEqualTo(1);
		assertThat(namedParameterJdbcOperations.queryForObject(
				"select count(*) from messages where a = :a and b IS NULL", mapB, Integer.class)).isEqualTo(1);
		assertThat(namedParameterJdbcOperations.queryForObject(
				"select count(*) from messages where a = :a and b IS NULL", mapC, Integer.class)).isEqualTo(1);
	}

}
