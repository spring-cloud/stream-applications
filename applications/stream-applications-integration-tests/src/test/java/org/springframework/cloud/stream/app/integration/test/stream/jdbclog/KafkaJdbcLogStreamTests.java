/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.stream.app.integration.test.stream.jdbclog;

import java.time.Duration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import org.springframework.cloud.stream.app.test.integration.LogMatcher;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainerTestUtils;
import org.springframework.cloud.stream.app.test.integration.StreamApps;
import org.springframework.cloud.stream.app.test.integration.junit.jupiter.KafkaStreamAppTest;
import org.springframework.cloud.stream.app.test.integration.kafka.KafkaConfig;
import org.springframework.cloud.stream.app.test.integration.kafka.KafkaStreamAppContainer;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.integration.test.common.Configuration.DEFAULT_DURATION;
import static org.springframework.cloud.stream.app.integration.test.common.Configuration.VERSION;
import static org.springframework.cloud.stream.app.test.integration.AppLog.appLog;
import static org.springframework.cloud.stream.app.test.integration.kafka.KafkaStreamApps.kafkaStreamApps;

@Tag("integration")
@KafkaStreamAppTest
public class KafkaJdbcLogStreamTests {

	private static LogMatcher logMatcher = LogMatcher.contains("Bart Simpson");

	@Container
	private static MySQLContainer mySQL = new MySQLContainer<>(DockerImageName.parse("mysql:5.7"))
		.withUsername("test")
		.withPassword("secret")
		.withExposedPorts(3306)
		.withNetwork(KafkaConfig.kafka.getNetwork())
		.withNetworkAliases("mysql-for-stream")
		.withLogConsumer(appLog("mySQL"))
		.withClasspathResourceMapping("init.sql", "/init.sql", BindMode.READ_ONLY)
		.withCommand("--init-file", "/init.sql")
		.withStartupTimeout(Duration.ofSeconds(120))
		.withStartupAttempts(3);

	/**
	 * Nested test whose purpose is to delay the starting of the stream app containers until the MySQL container
	 * in the outer test has started.
	 * <p>Nested tests can not be static and therefore the inner stream app container is not static. This is not an issue
	 * because there is only a single test.
	 */
	@Nested
	class StartAfterMySql {

		@Container
		private final StreamApps streamApp = kafkaStreamApps(KafkaJdbcLogStreamTests.class.getSimpleName(),
				KafkaConfig.kafka)
				.withSourceContainer(
						new KafkaStreamAppContainer(StreamAppContainerTestUtils.imageName(
								"jdbc-source-kafka",
								VERSION))
								.withEnv("JDBC_SUPPLIER_QUERY", "SELECT * FROM People WHERE deleted='N'")
								.withEnv("JDBC_SUPPLIER_UPDATE",
										"UPDATE People SET deleted='Y' WHERE id=:id")
								.withEnv("SPRING_DATASOURCE_PASSWORD", "secret")
								.withEnv("SPRING_DATASOURCE_USERNAME", "test")
								.withEnv("SPRING_DATASOURCE_DRIVER_CLASS_NAME", "org.mariadb.jdbc.Driver")
								.withEnv("SPRING_DATASOURCE_URL",
										"jdbc:mysql://mysql-for-stream:3306/test?permitMysqlScheme"))
				.withSinkContainer(
						new KafkaStreamAppContainer(StreamAppContainerTestUtils.imageName(
								"log-sink-kafka", VERSION)).withLogConsumer(logMatcher))
				.build();

		@Test
		void test() {
			await().atMost(DEFAULT_DURATION).until(logMatcher.matches());
		}
	}
}
