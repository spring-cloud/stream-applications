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

package org.springframework.cloud.stream.app.integration.test.source.jdbc;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.app.test.integration.OutputMatcher;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.app.test.integration.junit.jupiter.BaseContainerExtension;
import org.springframework.cloud.stream.app.test.integration.kafka.KafkaConfig;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.integration.test.common.Configuration.DEFAULT_DURATION;
import static org.springframework.cloud.stream.app.test.integration.AppLog.appLog;
@Tag("integration")
@ExtendWith(BaseContainerExtension.class)
abstract class JdbcSourceTests {

	@Autowired
	private OutputMatcher outputMatcher;

	private static StreamAppContainer source;

	@Container
	private static MySQLContainer mySQL = new MySQLContainer<>(DockerImageName.parse("mysql:5.7"))
			.withUsername("test")
			.withPassword("secret")
			.withExposedPorts(3306)
			.withNetwork(KafkaConfig.kafka.getNetwork())
			.withNetworkAliases("mysql-for-source")
			.withLogConsumer(appLog("mysql-for-source"))
			.withClasspathResourceMapping("init.sql", "/init.sql", BindMode.READ_ONLY)
			.withCommand("--init-file", "/init.sql")
			.withStartupTimeout(Duration.ofSeconds(120))
			.withStartupAttempts(3);

	@BeforeAll
	protected static void configureSource() {
		source = BaseContainerExtension.containerInstance()
				.withEnv("JDBC_SUPPLIER_QUERY", "SELECT * FROM People WHERE deleted='N'")
				.withEnv("JDBC_SUPPLIER_UPDATE", "UPDATE People SET deleted='Y' WHERE id=:id")
				.withEnv("SPRING_DATASOURCE_USERNAME", "test")
				.withEnv("SPRING_DATASOURCE_PASSWORD", "secret")
				.withEnv("SPRING_DATASOURCE_DRIVER_CLASS_NAME", "org.mariadb.jdbc.Driver")
				.withEnv("SPRING_DATASOURCE_URL", "jdbc:mysql://mysql-for-source:3306/test?permitMysqlScheme");
		startSource();
	}

	private static void startSource() {
		await().atMost(DEFAULT_DURATION).until(() -> mySQL.isRunning());
		source.start();
	}

	@Test
	void test() {
		await().atMost(DEFAULT_DURATION)
				.until(outputMatcher.payloadMatches((String s) -> s.contains("Bart Simpson")));
	}

	@AfterAll
	private static void cleanUp() {
		source.stop();
	}
}
