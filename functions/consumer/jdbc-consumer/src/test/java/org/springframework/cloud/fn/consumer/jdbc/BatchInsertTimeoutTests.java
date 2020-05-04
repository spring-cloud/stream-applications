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

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Eric Bottard
 * @author Thomas Risberg
 * @author Artem Bilan
 * @author Robert St. John
 * @author Oliver Flasch
 * @author Soby Chacko
 * @author Szabolcs Stremler
 */
@TestPropertySource(properties = { "jdbc.consumer.batchSize=1000", "jdbc.consumer.idleTimeout=100" })
public class BatchInsertTimeoutTests extends JdbcConsumerApplicationTests {

	@Test
	public void testBatchInsertionTimeout() {
		final int numberOfInserts = 10;
		Payload sent = new Payload("hello", 42);
		for (int i = 0; i < numberOfInserts; i++) {
			final Message<Payload> message = MessageBuilder.withPayload(sent).build();
			jdbcConsumer.accept(message);
		}
		Awaitility.await().until(() -> jdbcOperations
				.queryForObject("select count(*) from messages", Integer.class), value -> value == numberOfInserts);
	}

}
