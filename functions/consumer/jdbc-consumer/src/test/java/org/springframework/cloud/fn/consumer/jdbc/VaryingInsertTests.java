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

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
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
public class VaryingInsertTests extends JdbcConsumerApplicationTests {

	@Test
	public void testInsertion() {
		Payload a = new Payload("hello", 42);
		Payload b = new Payload("world", 12);
		Payload c = new Payload("bonjour", null);
		Payload d = new Payload(null, 22);
		final Message<Payload> message1 = MessageBuilder.withPayload(a).build();
		jdbcConsumer.accept(message1);
		final Message<Payload> message2 = MessageBuilder.withPayload(b).build();
		jdbcConsumer.accept(message2);
		final Message<Payload> message3 = MessageBuilder.withPayload(c).build();
		jdbcConsumer.accept(message3);
		final Message<Payload> message4 = MessageBuilder.withPayload(d).build();
		jdbcConsumer.accept(message4);
		List<Payload> result = jdbcOperations
				.query("select a, b from messages", new BeanPropertyRowMapper<>(Payload.class));
		Assertions.assertThat(result).extracting("a").containsExactly("hello", "world", "bonjour", null);
		Assertions.assertThat(result).extracting("b").contains(42, 12, 22, null);
	}

}
