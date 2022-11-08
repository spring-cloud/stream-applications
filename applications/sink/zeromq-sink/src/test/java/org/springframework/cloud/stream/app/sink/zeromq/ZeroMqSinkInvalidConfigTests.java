/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.stream.app.sink.zeromq;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.fn.consumer.zeromq.ZeroMqConsumerProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for ZeroMqSink with invalid config.
 *
 * @author Daniel Frey
 * @since 3.1.0
 */
public class ZeroMqSinkInvalidConfigTests {

	@Test
	public void testEmptyConnectionUrl() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> {

					AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
					TestPropertyValues.of("zeromq.consumer.connectUrl:").applyTo(context);
					context.register(Config.class);
					context.refresh();

				})
				.withMessageContaining("Error creating bean with name 'zeromq.consumer-org.springframework.cloud.fn.consumer.zeromq.ZeroMqConsumerProperties': Could not bind properties to 'ZeroMqConsumerProperties'")
				.havingRootCause()
				.withMessageContaining("Binding validation errors on zeromq.consumer")
				.withMessageContaining("Field error in object 'zeromq.consumer' on field 'connectUrl': rejected value []");
	}

	@Test
	public void testInvalidTopicExpression() {
		assertThatExceptionOfType(ConfigurationPropertiesBindException.class)
				.isThrownBy(() -> {

					AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
					TestPropertyValues
							.of(
									"zeromq.consumer.connectUrl: tcp://localhost:5678",
									"zeromq.consumer.topic: test-")
							.applyTo(context);
					context.register(Config.class);
					context.refresh();

				})
				.withMessageContaining("Error creating bean with name 'zeromq.consumer-org.springframework.cloud.fn.consumer.zeromq.ZeroMqConsumerProperties': Could not bind properties to 'ZeroMqConsumerProperties'")
				.havingCause()
				.withMessageContaining("Failed to bind properties under 'zeromq.consumer.topic' to org.springframework.expression.Expression");
	}

	@Configuration
	@EnableConfigurationProperties(ZeroMqConsumerProperties.class)
	@Import(org.springframework.cloud.stream.config.SpelExpressionConverterConfiguration.class)
	static class Config {
	}

}
