/*
 * Copyright 2022-2022 the original author or authors.
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

package org.springframework.cloud.stream.app.sink.rabbit;

import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Provides a static RabbitMQ Container that can be shared across test classes.
 *
 * @author Chris Bono
 * @author Artem Bilan
 */
@Testcontainers(disabledWithoutDocker = true)
public interface RabbitMqTestContainerSupport {

	RabbitMQContainer RABBIT_MQ_CONTAINER = new RabbitMQContainer("rabbitmq:management")
			.withExposedPorts(5672)
			.withStartupTimeout(Duration.ofSeconds(120))
			.withStartupAttempts(3);

	@BeforeAll
	static void startContainer() {
		RABBIT_MQ_CONTAINER.start();
	}

	static Integer getPort() {
		return RABBIT_MQ_CONTAINER.getFirstMappedPort();
	}

}
