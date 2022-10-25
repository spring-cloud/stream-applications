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

package org.springframework.cloud.fn.consumer.cassandra;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.fn.consumer.cassandra.domain.Book;
import org.springframework.data.cassandra.core.WriteResult;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 */
@TestPropertySource(properties = {
		"spring.cassandra.schema-action=RECREATE",
		"cassandra.cluster.entity-base-packages=org.springframework.cloud.fn.consumer.cassandra.domain" })
class CassandraEntityInsertTests extends CassandraConsumerApplicationTests {

	@Test
	void testInsert() {
		Book book =
				new Book(
						UUID.randomUUID(),
						"Spring Integration Cassandra",
						"Cassandra Guru",
						521,
						LocalDate.now(),
						true);

		Mono<? extends WriteResult> result = this.cassandraConsumer.apply(book);

		StepVerifier.create(result)
				.expectNextCount(1)
				.then(() ->
						assertThat(this.cassandraTemplate.query(Book.class)
								.count())
								.isEqualTo(1))
				.verifyComplete();
	}

}
