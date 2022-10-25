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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.consumer.cassandra.domain.Book;
import org.springframework.context.annotation.Import;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.WriteResult;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * @author Artem Bilan
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"spring.cassandra.keyspace-name=" + CassandraConsumerApplicationTests.CASSANDRA_KEYSPACE,
				"cassandra.cluster.createKeyspace=true" })
@DirtiesContext
abstract class CassandraConsumerApplicationTests implements CassandraContainerTest {

	static final String CASSANDRA_KEYSPACE = "test";

	@Autowired
	protected CassandraOperations cassandraTemplate;

	@Autowired
	protected Function<Object, Mono<? extends WriteResult>> cassandraConsumer;


	@DynamicPropertySource
	static void registerConfigurationProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.cassandra.localDatacenter", () -> CASSANDRA_CONTAINER.getLocalDatacenter());
		registry.add("spring.cassandra.contactPoints", () ->
			Optional.of(CASSANDRA_CONTAINER.getContactPoint())
					.map(contactPoint -> contactPoint.getAddress().getHostAddress() + ':' + contactPoint.getPort())
					.get());
	}

	@AfterEach
	void tearDown() {
		this.cassandraTemplate.truncate(Book.class);
	}

	protected static List<Book> getBookList(int numBooks) {

		List<Book> books = new ArrayList<>();
		for (int i = 0; i < numBooks; i++) {
			books.add(
					new Book(
							UUID.randomUUID(),
							"Spring Cloud Data Flow Guide",
							"SCDF Guru",
							i * 10 + 5,
							LocalDate.now(),
							true));
		}

		return books;
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import(CassandraConsumerConfiguration.class)
	static class CassandraConsumerTestApplication {

	}

}
