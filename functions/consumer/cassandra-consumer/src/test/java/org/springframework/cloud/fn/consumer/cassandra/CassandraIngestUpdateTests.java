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

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.fn.consumer.cassandra.domain.Book;
import org.springframework.data.cassandra.core.WriteResult;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 */
@Disabled
@TestPropertySource(properties = {
		"cassandra.cluster.init-script=init-db.cql",
		"cassandra.ingest-query=" +
				"update book set inStock = :inStock, author = :author, pages = :pages, " +
				"saleDate = :saleDate, title = :title where isbn = :isbn",
		"cassandra.queryType=UPDATE" })
class CassandraIngestUpdateTests extends CassandraConsumerApplicationTests {

	@Test
	void testIngestQuery(@Autowired ObjectMapper objectMapper) throws Exception {
		List<Book> books = getBookList(5);

		Jackson2JsonObjectMapper mapper = new Jackson2JsonObjectMapper(objectMapper);

		Mono<? extends WriteResult> result =
				this.cassandraConsumer.apply(mapper.toJson(books));

		StepVerifier.create(result)
				.expectNextCount(1)
				.then(() ->
						assertThat(this.cassandraTemplate.query(Book.class)
								.count())
								.isEqualTo(5))
				.verifyComplete();
	}

}
