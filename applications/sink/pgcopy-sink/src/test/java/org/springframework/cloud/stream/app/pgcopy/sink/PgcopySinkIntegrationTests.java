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
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for PgcopySink. Only runs if PostgreSQL database is available.
 *
 * @author Thomas Risberg
 * @author Chris Bono
 */
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		classes = PgcopyErrorTableIntegrationTests.PgcopySinkApplication.class,
		properties = {
				"spring.cloud.function.definition=pgcopyConsumer"
		})
@ExtendWith(PostgresAvailableExtension.class)
@DirtiesContext
public abstract class PgcopySinkIntegrationTests {

	@Autowired
	protected Consumer<Message<?>> pgcopyConsumer;

	@Autowired
	protected JdbcOperations jdbcOperations;

	@TestPropertySource(properties = {"pgcopy.table-name=test", "pgcopy.batch-size=1", "pgcopy.initialize=true"})
	public static class BasicPayloadCopyTests extends PgcopySinkIntegrationTests {

		@Test
		public void testBasicCopy() {
			String sent = "hello42";
			this.pgcopyConsumer.accept(MessageBuilder.withPayload(sent).build());
			String result = jdbcOperations.queryForObject("select payload from test", String.class);
			assertThat(result).isEqualTo("hello42");
		}
	}

	@TestPropertySource(properties = {"pgcopy.tableName=names", "pgcopy.batch-size=4", "pgcopy.initialize=true",
			"pgcopy.columns=id,name,age"})
	public static class PgcopyTextTests extends PgcopySinkIntegrationTests {

		@Test
		public void testCopyText() {
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("123\tNisse\t25").build());
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("124\tAnna\t21").build());
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("125\tBubba\t22").build());
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("126\tPelle\t32").build());
			int result = jdbcOperations.queryForObject("select count(*) from names", Integer.class);
			assertThat(result).isEqualTo(4);
		}
	}

	@TestPropertySource(properties = {"pgcopy.tableName=names", "pgcopy.batch-size=3", "pgcopy.initialize=true",
			"pgcopy.columns=id,name,age", "pgcopy.format=CSV"})
	public static class PgcopyCSVTests extends PgcopySinkIntegrationTests {

		@Test
		public void testCopyCSV() {
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("123,\"Nisse\",25").build());
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("124,\"Anna\",21").build());
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("125,\"Bubba\",22").build());
			int result = jdbcOperations.queryForObject("select count(*) from names", Integer.class);
			assertThat(result).isEqualTo(3);
		}
	}

	@TestPropertySource(properties = {"pgcopy.tableName=names", "pgcopy.batch-size=3", "pgcopy.initialize=true",
			"pgcopy.columns=id,name,age", "pgcopy.format=CSV"})
	public static class PgcopyNullTests extends PgcopySinkIntegrationTests {

		@Test
		public void testCopyCSV() {
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("123,\"Nisse\",25").build());
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("124,,21").build());
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("125,\"Bubba\",22").build());
			int result = jdbcOperations.queryForObject("select count(*) from names", Integer.class);
			int nulls = jdbcOperations.queryForObject("select count(*) from names where name is null", Integer.class);
			assertThat(result).isEqualTo(3);
			assertThat(nulls).isEqualTo(1);
		}
	}

	@TestPropertySource(properties = {"pgcopy.tableName=names", "pgcopy.batch-size=3", "pgcopy.initialize=true",
			"pgcopy.columns=id,name,age", "pgcopy.format=CSV", "pgcopy.null-string=null"})
	public static class PgcopyNullStringTests extends PgcopySinkIntegrationTests {

		@Test
		public void testCopyCSV() {
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("123,\"Nisse\",25").build());
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("124,null,21").build());
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("125,\"Bubba\",22").build());
			int result = jdbcOperations.queryForObject("select count(*) from names", Integer.class);
			int nulls = jdbcOperations.queryForObject("select count(*) from names where name is null", Integer.class);
			assertThat(result).isEqualTo(3);
			assertThat(nulls).isEqualTo(1);
		}
	}

	@TestPropertySource(properties = {"pgcopy.tableName=names", "pgcopy.batch-size=3", "pgcopy.initialize=true",
			"pgcopy.columns=id,name,age", "pgcopy.format=CSV", "pgcopy.delimiter=|"})
	public static class PgcopyDelimiterTests extends PgcopySinkIntegrationTests {

		@Test
		public void testCopyCSV() {
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("123|\"Nisse\"|25").build());
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("124|\"Anna\"|21").build());
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("125|\"Bubba\"|22").build());
			int result = jdbcOperations.queryForObject("select count(*) from names", Integer.class);
			assertThat(result).isEqualTo(3);
		}
	}

	@TestPropertySource(properties = {"pgcopy.tableName=names", "pgcopy.batch-size=3", "pgcopy.initialize=true",
			"pgcopy.columns=id,name,age", "pgcopy.format=CSV", "pgcopy.delimiter=\\t"})
	public static class PgcopyEscapedDelimiterTests extends PgcopySinkIntegrationTests {

		@Test
		public void testCopyCSV() {
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("123\t\"Nisse\"\t25").build());
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("124\t\"Anna\"\t21").build());
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("125\t\"Bubba\"\t22").build());
			int result = jdbcOperations.queryForObject("select count(*) from names", Integer.class);
			assertThat(result).isEqualTo(3);
		}
	}

	@TestPropertySource(properties = {"pgcopy.tableName=names", "pgcopy.batch-size=3", "pgcopy.initialize=true",
			"pgcopy.columns=id,name,age", "pgcopy.format=CSV", "pgcopy.quote='"})
	public static class PgcopyQuoteTests extends PgcopySinkIntegrationTests {

		@Test
		public void testCopyCSV() {
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("123,Nisse,25").build());
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("124,'Anna',21").build());
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("125,Bubba,22").build());
			int result = jdbcOperations.queryForObject("select count(*) from names", Integer.class);
			int quoted = jdbcOperations.queryForObject("select count(*) from names where name = 'Anna'", Integer.class);
			assertThat(result).isEqualTo(3);
			assertThat(quoted).isEqualTo(1);
		}
	}

	@TestPropertySource(properties = {"pgcopy.tableName=names", "pgcopy.batch-size=3", "pgcopy.initialize=true",
			"pgcopy.columns=id,name,age", "pgcopy.format=CSV", "pgcopy.escape=\\\\"})
	public static class PgcopyEscapeTests extends PgcopySinkIntegrationTests {

		@Test
		public void testCopyCSV() {
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("123,Nisse,25").build());
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("124,\"Anna\\\"\",21").build());
			this.pgcopyConsumer.accept(MessageBuilder.withPayload("125,Bubba,22").build());
			int result = jdbcOperations.queryForObject("select count(*) from names", Integer.class);
			int quoted = jdbcOperations.queryForObject("select count(*) from names where name = 'Anna\"'", Integer.class);
			assertThat(result).isEqualTo(3);
			assertThat(quoted).isEqualTo(1);
		}
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
