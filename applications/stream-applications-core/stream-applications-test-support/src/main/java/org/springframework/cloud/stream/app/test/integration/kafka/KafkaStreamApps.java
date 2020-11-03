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

package org.springframework.cloud.stream.app.test.integration.kafka;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;

import org.springframework.cloud.stream.app.test.integration.StreamApps;
/**
 * Configures an end to end Stream (source, processor(s), sink) using
 * {@link org.springframework.cloud.stream.app.test.integration.kafka.KafkaStreamAppContainer}s.
 * @author David Turanski
 */
public class KafkaStreamApps extends StreamApps {

	protected KafkaStreamApps(GenericContainer sourceContainer, List<GenericContainer> processorContainers,
			GenericContainer sinkContainer) {
		super(sourceContainer, processorContainers, sinkContainer);
	}

	public static Builder<KafkaStreamApps> kafkaStreamApps(String streamName, GenericContainer messageBrokerContainer) {
		return new KafkaBuilder(streamName, messageBrokerContainer);
	}

	public static final class KafkaBuilder extends Builder<KafkaStreamApps> {

		protected KafkaBuilder(String streamName, GenericContainer messageBrokerContainer) {
			super(streamName, messageBrokerContainer);
		}

		protected Map<String, String> binderProperties() {
			return Collections.singletonMap("SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS",
					messageBrokerContainer.getNetworkAliases().get(0) + ":9092");
		}

		@Override
		protected KafkaStreamApps doBuild(GenericContainer sourceContainer,
				List<GenericContainer> processorContainers, GenericContainer sinkContainer) {
			return new KafkaStreamApps(sourceContainer, processorContainers, sinkContainer);
		}
	}
}
