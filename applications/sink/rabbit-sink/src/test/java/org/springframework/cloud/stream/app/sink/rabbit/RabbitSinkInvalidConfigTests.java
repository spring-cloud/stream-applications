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

package org.springframework.cloud.stream.app.sink.rabbit;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.context.properties.bind.validation.ValidationErrors;
import org.springframework.cloud.fn.consumer.rabbit.RabbitConsumerProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.FieldError;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for RabbitSource with invalid config.
 *
 * @author Gary Russell
 * @author Chris Schaefer
 */
public class RabbitSinkInvalidConfigTests {

	@Test
	public void testNoRoutingKey() {
		try {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			context.register(Config.class);
			context.refresh();
			context.close();
			fail("BeanCreationException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(BeanCreationException.class));

			BindValidationException bindValidationException = (BindValidationException) e.getCause().getCause();
			ValidationErrors validationErrors = bindValidationException.getValidationErrors();
			FieldError fieldError = (FieldError) validationErrors.getAllErrors().get(0);

			assertThat(fieldError.getDefaultMessage(), containsString("routingKey or routingKeyExpression is required"));
		}
	}

	@Configuration
	@EnableConfigurationProperties(RabbitConsumerProperties.class)
	static class Config {

	}
}
