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

import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;

/**
 * @author Soby Chacko
 */
@SpringBootTest
@DirtiesContext
public class JdbcConsumerApplicationTests {

	@Autowired
	Consumer<Message<?>> jdbcConsumer;

	@Autowired
	JdbcOperations jdbcOperations;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@AfterEach
	public void cleanup() {
		jdbcOperations.execute("DROP TABLE MESSAGES IF EXISTS");
	}

	static class Payload {

		private String a;

		private Integer b;

		public Payload() {
		}

		public Payload(String a, Integer b) {
			this.a = a;
			this.b = b;
		}

		public String getA() {
			return a;
		}

		public Integer getB() {
			return b;
		}

		public void setA(String a) {
			this.a = a;
		}

		public void setB(Integer b) {
			this.b = b;
		}

		@Override
		public String toString() {
			return a + b;
		}

	}

	@SpringBootApplication
	static class TestApplication {}

}
