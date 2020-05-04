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
@TestPropertySource(properties = "jdbc.consumer.columns=a: new StringBuilder(payload.a).reverse().toString(), b")
public class UnqualifiableColumnExpressionTests extends JdbcConsumerApplicationTests {

	@Test
	public void doesNotFailParsingUnqualifiableExpression() {
		// if the app initializes, the test condition passes, but go ahead and apply the column expression anyway
		jdbcConsumer.accept(MessageBuilder.withPayload(new Payload("desrever", 123)).build());
		assertThat(jdbcOperations.queryForObject("select count(*) from messages where a = ? and b = ?",
				Integer.class, "reversed", 123)).isEqualTo(1);
	}

}
