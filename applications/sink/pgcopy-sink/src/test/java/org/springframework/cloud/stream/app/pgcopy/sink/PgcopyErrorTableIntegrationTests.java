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

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.app.pgcopy.test.PostgresTestSupport;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.is;

/**
 * Integration Tests for PgcopySink with error table. Only runs if PostgreSQL database is available.
 *
 * @author Thomas Risberg
 * @author Janne Valkealahti
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
		classes = PgcopyErrorTableIntegrationTests.PgcopySinkApplication.class)
@TestPropertySource(properties = { "pgcopy.tableName=names", "pgcopy.batch-size=3", "pgcopy.initialize=true",
		"pgcopy.columns=id,name,age", "pgcopy.format=CSV", "pgcopy.error-table=test_errors",
		"spring.datasource.initialization-mode=always", "spring.datasource.schema=classpath:error-table-ddl.sql",
		"spring.datasource.continue-on-error=true" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class PgcopyErrorTableIntegrationTests {

	@ClassRule
	public static PostgresTestSupport postgresAvailable = new PostgresTestSupport();

	@Autowired
	protected Sink channels;

	@Autowired
	protected JdbcOperations jdbcOperations;

	@Test
	public void testCopyCSV() {
		channels.input().send(MessageBuilder.withPayload("123,Nisse,25").build());
		channels.input().send(MessageBuilder.withPayload("GARBAGE").build());
		channels.input().send(MessageBuilder.withPayload("125,Bubba,22").build());
		int result = jdbcOperations.queryForObject("select count(*) from names", Integer.class);
		int errors = jdbcOperations.queryForObject("select count(*) from test_errors", Integer.class);
		Assert.assertThat(result, is(2));
		Assert.assertThat(errors, is(1));
	}

	@SpringBootApplication
	public static class PgcopySinkApplication {
		public static void main(String[] args) {
			SpringApplication.run(PgcopySinkApplication.class, args);
		}
	}
}
