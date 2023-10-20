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

package org.springframework.cloud.stream.app.test.integration.rabbitmq;

import java.time.Duration;

import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Initializes and starts a {@link RabbitMQContainer}.
 * @author David Turanski
 * @author Corneil du Plessis
 */
public abstract class RabbitMQConfig {
	/**
	 * The RabbitMQContainer.
	 */
	public static RabbitMQContainer rabbitmq;


	static {
		rabbitmq = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.8-management"))
			.withNetwork(Network.SHARED)
			.withNetworkAliases("rabbitmq")
			.withExposedPorts(5672, 15672)
			.withStartupTimeout(Duration.ofSeconds(120))
			.withStartupAttempts(3);
		rabbitmq.start();
	}
}
