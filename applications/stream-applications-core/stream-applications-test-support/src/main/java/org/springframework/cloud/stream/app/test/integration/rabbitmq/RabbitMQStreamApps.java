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

import java.util.List;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;

import org.springframework.cloud.stream.app.test.integration.StreamApps;

import static org.springframework.cloud.stream.app.test.integration.FluentMap.fluentStringMap;

/**
 * Configures an end to end Stream (source, processor(s), sink) using
 * {@link org.springframework.cloud.stream.app.test.integration.rabbitmq.RabbitMQStreamAppContainer}s.
 * @author David Turanski
 * @author Corneil du Plessis
 */
public class RabbitMQStreamApps extends StreamApps {

	protected RabbitMQStreamApps(GenericContainer sourceContainer, List<GenericContainer> processorContainers,
			GenericContainer sinkContainer) {
		super(sourceContainer, processorContainers, sinkContainer);
	}

	public static Builder<RabbitMQStreamApps> rabbitMQStreamApps(String streamName,
			GenericContainer messageBrokerContainer) {
		return new RabbitMQBuilder(streamName, messageBrokerContainer);
	}

	public static final class RabbitMQBuilder extends Builder<RabbitMQStreamApps> {

		protected RabbitMQBuilder(String streamName, GenericContainer messageBrokerContainer) {
			super(streamName, messageBrokerContainer);
		}

		protected Map<String, String> binderProperties() {

			return fluentStringMap()
					.withEntry("SPRING_RABBITMQ_HOST",
						(String) messageBrokerContainer.getNetworkAliases().get(0))
					.withEntry("SPRING_RABBITMQ_PORT", "5672");
		}

		@Override
		protected RabbitMQStreamApps doBuild(GenericContainer sourceContainer,
				List<GenericContainer> processorContainers, GenericContainer sinkContainer) {
			return new RabbitMQStreamApps(sourceContainer, processorContainers, sinkContainer);
		}
	}
}
