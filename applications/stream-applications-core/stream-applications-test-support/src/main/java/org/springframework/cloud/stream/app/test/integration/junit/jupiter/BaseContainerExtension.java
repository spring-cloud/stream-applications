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

package org.springframework.cloud.stream.app.test.integration.junit.jupiter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainerTestUtils;
import org.springframework.cloud.stream.app.test.integration.kafka.KafkaStreamAppContainer;
import org.springframework.cloud.stream.app.test.integration.rabbitmq.RabbitMQStreamAppContainer;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * A junit Jupiter extension used to discover {@link StreamAppContainer}s annotated with
 * {@link BaseContainer}.
 * @author David Turanski
 */
public class BaseContainerExtension implements ExecutionCondition {
	private static StreamAppContainer baseContainer;

	private static Logger logger = LoggerFactory.getLogger(BaseContainerExtension.class);

	public static StreamAppContainer containerInstance() {
		return baseContainer;
	}

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {

		BaseContainer annotation = AnnotatedElementUtils.getMergedAnnotation(extensionContext.getRequiredTestClass(),
				BaseContainer.class);
		if (annotation == null) {
			throw new ExtensionConfigurationException("@BaseContainer is required for this extension");
		}

		String version = getVersion(annotation);

		switch (annotation.binder()) {
		case Kafka:
			baseContainer = new KafkaStreamAppContainer(
					StreamAppContainerTestUtils.imageName(annotation.repository(), annotation.name(), version));
			break;
		case RabbitMQ:
			baseContainer = new RabbitMQStreamAppContainer(
					StreamAppContainerTestUtils.imageName(annotation.repository(), annotation.name(), version));
			break;
		default:
			throw new ExtensionConfigurationException(
					"the binder type " + annotation.binder().name() + " is not supported");
		}
		logger.debug("StreamAppContainer created using base container image " + baseContainer.getDockerImageName());

		return ConditionEvaluationResult
				.enabled("StreamAppContainer created using base container image " + baseContainer.getDockerImageName());
	}

	private String getVersion(BaseContainer annotation) {
		String version = null;
		if (annotation.version().isEmpty() && annotation.versionSupplier().equals(NullVersionSupplier.class)) {
			throw new ExtensionConfigurationException(
					"either 'version' or 'versionSupplier' must be set in @BaseContainer");
		}
		if (!annotation.version().isEmpty() && !annotation.versionSupplier().equals(NullVersionSupplier.class)) {
			throw new ExtensionConfigurationException(
					"only one of 'version' or 'versionSupplier' must be set in @BaseContainer");
		}
		if (annotation.version().isEmpty()) {
			try {
				Constructor<? extends Supplier<String>> constructor = annotation.versionSupplier().getConstructor();
				version = constructor.newInstance().get();
			}
			catch (NoSuchMethodException | InstantiationException | IllegalAccessException
					| InvocationTargetException e) {
				throw new ExtensionConfigurationException(
						"No accessible default constructor found for " + annotation.versionSupplier().getName());
			}
		}
		else {
			version = annotation.version();
		}
		return version;
	}
}
