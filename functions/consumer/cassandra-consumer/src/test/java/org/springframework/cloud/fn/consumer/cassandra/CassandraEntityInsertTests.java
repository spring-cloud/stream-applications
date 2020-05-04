/*
 * Copyright 2019-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import org.springframework.cloud.fn.consumer.cassandra.domain.Book;
import org.springframework.data.cassandra.core.WriteResult;
import org.springframework.test.context.TestPropertySource;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author Artem Bilan
 */
@DisabledOnOs(OS.WINDOWS)
@TestPropertySource(properties = {
		"spring.data.cassandra.schema-action=RECREATE",
		"cassandra.cluster.entity-base-packages=io.pivotal.java.function.cassandra.consumer.domain" })
class CassandraEntityInsertTests extends CassandraConsumerApplicationTests {

	@Test
	@Disabled
	void testInsert() {
		Book book = new Book();
		book.setIsbn(UUID.randomUUID());
		book.setTitle("Spring Integration Cassandra");
		book.setAuthor("Cassandra Guru");
		book.setPages(521);
		book.setSaleDate(LocalDate.now());
		book.setInStock(true);

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
