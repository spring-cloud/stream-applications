/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.stream.app.pgcopy.sink;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.app.pgcopy.test.PostgresAvailableExtension;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for PgcopySink with error table. Only runs if PostgreSQL database is available.
 *
 * @author Thomas Risberg
 * @author Janne Valkealahti
 * @author Chris Bono
 */
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		classes = PgcopyErrorTableIntegrationTests.PgcopySinkApplication.class,
		properties = {
				"spring.cloud.function.definition=pgcopyConsumer",
				"pgcopy.tableName=names", "pgcopy.batch-size=3", "pgcopy.initialize=true",
				"pgcopy.columns=id,name,age", "pgcopy.format=CSV", "pgcopy.error-table=test_errors",
				"spring.sql.init.mode=always", "spring.sql.init.schema-locations=classpath:error-table-ddl.sql",
				"spring.sql.init.continue-on-error=true"
		})
@ExtendWith(PostgresAvailableExtension.class)
@DirtiesContext
public class PgcopyErrorTableIntegrationTests {

	@Autowired
	private Consumer<Message<?>> pgcopyConsumer;

	@Autowired
	private JdbcOperations jdbcOperations;

	@Test
	public void testCopyCSV() {
		this.pgcopyConsumer.accept(MessageBuilder.withPayload("123,Nisse,25").build());
		this.pgcopyConsumer.accept(MessageBuilder.withPayload("GARBAGE").build());
		this.pgcopyConsumer.accept(MessageBuilder.withPayload("125,Bubba,22").build());

		int result = this.jdbcOperations.queryForObject("select count(*) from names", Integer.class);
		int errors = this.jdbcOperations.queryForObject("select count(*) from test_errors", Integer.class);

		assertThat(result).isEqualTo(2);
		assertThat(errors).isEqualTo(1);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import({ PgcopySinkConfiguration.class, TestChannelBinderConfiguration.class })
	public static class PgcopySinkApplication {
		public static void main(String[] args) {
			SpringApplication.run(PgcopySinkApplication.class, args);
		}
	}
}
