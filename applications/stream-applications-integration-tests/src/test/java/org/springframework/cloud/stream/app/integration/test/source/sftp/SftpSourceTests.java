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

package org.springframework.cloud.stream.app.integration.test.source.sftp;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.app.test.integration.AppLog;
import org.springframework.cloud.stream.app.test.integration.OutputMatcher;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.app.test.integration.junit.jupiter.BaseContainerExtension;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.integration.test.common.Configuration.DEFAULT_DURATION;

@SuppressWarnings({"rawtypes", "unchecked"})
@Tag("integration")
@ExtendWith(BaseContainerExtension.class)
abstract class SftpSourceTests {
	private static final Logger logger = LoggerFactory.getLogger(SftpSourceTests.class);
	private StreamAppContainer source;

	@Container
	private static final GenericContainer sftp = new GenericContainer(DockerImageName.parse("atmoz/sftp"))
		.withExposedPorts(22)
		.withNetwork(Network.SHARED)
		.withNetworkAliases("sftp-host")
		.withCommand("user:pass:::remote")
		.withClasspathResourceMapping("sftp", "/home/user/remote", BindMode.READ_ONLY)
		.withStartupTimeout(DEFAULT_DURATION)
		.withLogConsumer(AppLog.appLog("sftp-host"));

	@BeforeEach
	void configureSource() {
		await().atMost(DEFAULT_DURATION).until(sftp::isRunning);
		source = BaseContainerExtension.containerInstance()
			.withNetwork(Network.SHARED)
			.withEnv("SFTP_SUPPLIER_FACTORY_ALLOW_UNKNOWN_KEYS", "true")
			.withEnv("SFTP_SUPPLIER_REMOTE_DIR", "/remote")
			.withEnv("SFTP_SUPPLIER_FACTORY_USERNAME", "user")
			.withEnv("SFTP_SUPPLIER_FACTORY_PASSWORD", "pass")
			.withEnv("SFTP_SUPPLIER_FACTORY_PORT", "22")
			.withEnv("SFTP_SUPPLIER_FACTORY_HOST", "sftp-host")
			.withEnv("LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_INTEGRATION", "DEBUG")
			.withEnv("LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_CLOUD", "DEBUG")
			.log();
	}

	@Autowired
	private OutputMatcher outputMatcher;

	@Test
	void testRef() {
		startContainer(Collections.singletonMap("FILE_CONSUMER_MODE", "ref"));

		await().atMost(DEFAULT_DURATION).until(outputMatcher.payloadMatches((String s) -> {
			logger.info("payload:{}", s);
		return s.equals("\"/tmp/sftp-supplier/data.txt\"");
		}));
	}
	@Test
	void testLines() {
		startContainer(Collections.singletonMap("FILE_CONSUMER_MODE", "lines"));

		await().atMost(DEFAULT_DURATION).until(outputMatcher.payloadMatches((String s) -> {
			logger.info("payload:{}", s);
			return s.contains("Bart Simpson");
		}));
	}

	private void startContainer(Map<String, String> environment) {
		source.withEnv(environment);
		source.start();
		environment.keySet().forEach(k -> source.getEnvMap().remove(k));
	}

	@AfterEach
	public void cleanUp() {
		source.stop();
	}

}
