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

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import org.springframework.util.Assert;

import static org.springframework.cloud.stream.app.test.integration.AppLog.appLog;

/**
 * Extends {@link org.testcontainers.containers.GenericContainer} to support dockerized
 * Spring Cloud Stream applications. Currently this only supports apps with single I/O
 * destinations. This configures standard input and output destination bindings.
 * @author David Turanski
 * @author Corneil du Plessis
 */
public abstract class StreamAppContainer extends GenericContainer<StreamAppContainer> {

	protected Logger logger = LoggerFactory.getLogger(this.getClass());

	protected final GenericContainer messageBrokerContainer;

	private String inputDestination;

	private String outputDestination;

	/**
	 * Create a TestContainer with standard Spring Cloud Stream input and output bindings. For
	 * function based apps, you need to alias the function endpoints to "input" and "output".
	 * The output destination is set to
	 * {@code TestTopicListener.STREAM_APPLICATIONS_TEST_TOPIC}. The input destination is a
	 * random value.
	 * @param imageName the name of the image.
	 * @param messageBrokerContainer a message broker TestContainer. Typically, it is a
	 *     singleton TestContainer created in a static initializer.
	 */
	public StreamAppContainer(String imageName, GenericContainer messageBrokerContainer) {
		super(DockerImageName.parse(imageName));
		Assert.notNull(messageBrokerContainer, "A Message broker container is required.");
		Assert.isTrue(messageBrokerContainer.isRunning(), "Message broker container must be started first.");
		this.messageBrokerContainer = messageBrokerContainer;
		this.withNetwork(Network.SHARED).dependsOn(this.messageBrokerContainer)
				.withOutputDestination(TestTopicListener.STREAM_APPLICATIONS_TEST_TOPIC)
				.withInputDestination(TestTopicListener.STREAM_APPLICATIONS_TEST_TOPIC + "_IN_"
						+ UUID.randomUUID().toString().substring(0, 8))
				.withBinderProperties();
		if (logger.isDebugEnabled()) {
			this.log();
		}
	}

	/**
	 * @return the input destination.
	 */
	public String getInputDestination() {
		return inputDestination;
	}

	/**
	 * @return the output destination.
	 */
	public String getOutputDestination() {
		return outputDestination;
	}

	/**
	 * Enable container logging. This is invoked if the class logger is set to DEBUG.
	 * @return the instance.
	 */
	public StreamAppContainer log() {
		this.withLogConsumer(appLog(this.getImage().get()));
		return this;
	}

	/**
	 * Assign a destination name to the standard input.
	 * @param destination the destination name.
	 * @return this.
	 */
	public StreamAppContainer withInputDestination(String destination) {
		Assert.hasText(destination, "'destination' is required.");
		withEnv("SPRING_CLOUD_STREAM_BINDINGS_INPUT_DESTINATION", destination);
		withEnv("SPRING_CLOUD_STREAM_BINDINGS_INPUT_GROUP", TestTopicListener.STREAM_APPLICATIONS_TEST_TOPIC + "_GROUP_"
				+ UUID.randomUUID().toString().substring(0, 8));
		inputDestination = destination;
		return this;
	}

	private StreamAppContainer withOutputDestination(String destination) {
		Assert.hasText(destination, "'destination' is required.");
		withEnv("SPRING_CLOUD_STREAM_BINDINGS_OUTPUT_DESTINATION", destination);
		outputDestination = destination;
		return this;
	}

	protected abstract StreamAppContainer withBinderProperties();
}
