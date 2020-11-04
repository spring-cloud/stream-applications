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

import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;

/**
 * An implementation of
 * {@link org.springframework.cloud.stream.app.test.integration.StreamAppContainer} for
 * kafka. This provides the required broker connection properties.
 * @author David Turanski
 */
public class KafkaStreamAppContainer extends StreamAppContainer {

	/**
	 * @param imageName the image name.
	 */
	public KafkaStreamAppContainer(String imageName) {
		super(imageName, KafkaConfig.kafka);
	}

	@Override
	protected StreamAppContainer withBinderProperties() {
		this.withEnv("SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS",
				messageBrokerContainer.getNetworkAliases().get(0) + ":9092");
		return this;
	}
}
