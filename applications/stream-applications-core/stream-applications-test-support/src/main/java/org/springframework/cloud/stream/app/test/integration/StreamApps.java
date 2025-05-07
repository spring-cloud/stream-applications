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

package org.springframework.cloud.stream.app.test.integration;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startable;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * The base class used for testing end-to-end Stream applications.
 *
 * @author David Turanski
 * @author Corneil du Plessis
 * @see org.springframework.cloud.stream.app.test.integration.kafka.KafkaStreamApps
 * @see org.springframework.cloud.stream.app.test.integration.rabbitmq.RabbitMQStreamApps
 */
@SuppressWarnings("resource")
public abstract class StreamApps implements AutoCloseable, Startable {

	protected Logger logger = LoggerFactory.getLogger(this.getClass());

	private final GenericContainer sourceContainer;

	private final GenericContainer sinkContainer;

	private List<GenericContainer> processorContainers;

	protected StreamApps(GenericContainer sourceContainer, List<GenericContainer> processorContainers,
			GenericContainer sinkContainer) {
		this.sourceContainer = sourceContainer;
		this.sinkContainer = sinkContainer;
		this.processorContainers = processorContainers;
	}

	public GenericContainer sourceContainer() {
		return sourceContainer;
	}

	public GenericContainer sinkContainer() {
		return sinkContainer;
	}

	public List<GenericContainer> processorContainers() {
		return processorContainers;
	}

	public void start() {
		if (logger.isDebugEnabled()) {
			logDebugInfo();
		}

		sinkContainer.start();
		processorContainers.forEach(GenericContainer::start);
		sourceContainer.start();
	}

	public void stop() {
		sinkContainer.stop();
		processorContainers.forEach(GenericContainer::stop);
		sourceContainer.stop();
	}

	@SuppressWarnings("unchecked")
	private void logDebugInfo() {
		logger.debug("Starting apps...");
		logger.debug("Source container environment for {} :", sourceContainer().getImage().get());
		sourceContainer().getEnv().forEach((Consumer<String>) env -> logger.debug(env));

		if (!CollectionUtils.isEmpty(processorContainers)) {
			logger.debug("\nProcessor containers environment:");
			processorContainers().forEach(container -> {
				logger.debug("Processor container environment for {}", container.getImage().get());
				container.getEnv().forEach((Consumer<String>) env -> logger.debug(env));
			});
		}
		logger.debug("\nSink container environment for {} :", sinkContainer().getImage().get());
		sinkContainer().getEnv().forEach((Consumer<String>) env -> logger.debug(env));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static abstract class Builder<S extends StreamApps> {
		private final String streamName;

		private GenericContainer source;

		private GenericContainer sink;

		private final List<GenericContainer> processors = new LinkedList<>();

		protected final GenericContainer messageBrokerContainer;

		protected Builder(String streamName, GenericContainer messageBrokerContainer) {
			Assert.hasText(streamName, "Stream name is required");
			Assert.notNull(messageBrokerContainer, "A Message broker container is required.");
			Assert.isTrue(messageBrokerContainer.isRunning(), "Message broker container must be started first.");
			this.messageBrokerContainer = messageBrokerContainer;
			this.streamName = streamName;
		}

		public Builder withSourceContainer(GenericContainer sourceContainer) {
			this.source = sourceContainer;
			return this;
		}

		public Builder withSinkContainer(GenericContainer sinkContainer) {
			this.sink = sinkContainer;
			return this;
		}

		public Builder withProcessorContainer(GenericContainer processorContainer) {
			this.processors.add(processorContainer);
			return this;
		}

		public S build() {

			Assert.notNull(source, "A Source container is required.");
			Assert.notNull(sink, "A Sink container is required.");

			return doBuild(setupSourceContainer(), setupProcessorContainers(), setupSinkContainer());
		}

		protected abstract Map<String, String> binderProperties();

		protected abstract S doBuild(GenericContainer sourceContainer,
				List<GenericContainer> processorContainers, GenericContainer sinkContainer);

		private GenericContainer setupSourceContainer() {
			return source.withNetwork(messageBrokerContainer.getNetwork())
				.withEnv("SPRING_CLOUD_STREAM_BINDINGS_OUTPUT_DESTINATION", sourceOutputDestination())
				.withEnv(binderProperties())
				.dependsOn(messageBrokerContainer);
		}

		private GenericContainer setupSinkContainer() {
			return sink
				.withNetwork(messageBrokerContainer.getNetwork())
				.withEnv("SPRING_CLOUD_STREAM_BINDINGS_INPUT_DESTINATION", sinkInputDestination())
				.withEnv("SPRING_CLOUD_STREAM_BINDINGS_INPUT_GROUP", streamName)
				.withEnv(binderProperties())
				.dependsOn(messageBrokerContainer);
		}

		private List<GenericContainer> setupProcessorContainers() {
			IntStream.range(0, processors.size())
				.forEach(i -> processors.get(i).withNetwork(messageBrokerContainer.getNetwork())
					.withEnv("SPRING_CLOUD_STREAM_BINDINGS_INPUT_DESTINATION",
						i == 0 ? sourceOutputDestination() : "processor_ " + i)
					.withEnv("SPRING_CLOUD_STREAM_BINDINGS_OUTPUT_DESTINATION",
						i == (processors.size() - 1) ? sinkInputDestination()
							: "processor_" + (i + 1))
					.withEnv("SPRING_CLOUD_STREAM_BINDINGS_INPUT_GROUP", streamName)
					.withEnv(binderProperties())
					.dependsOn(messageBrokerContainer));
			return processors;
		}

		private String sourceOutputDestination() {
			return CollectionUtils.isEmpty(processors) ? streamName : "processor_0";
		}

		private String sinkInputDestination() {
			return (CollectionUtils.isEmpty(processors) || processors.size() <= 1) ? streamName
				: "processor_" + (processors.size() - 1);
		}
	}
}
