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

package org.springframework.cloud.stream.app.source.zeromq;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.context.properties.bind.validation.ValidationErrors;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.fn.supplier.zeromq.ZeroMqSupplierProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.FieldError;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for ZeroMqSource with invalid config.
 *
 * @author Daniel Frey
 * @author Gregory Green
 * @since 3.1.0
 */
@Disabled("This test fails consistently during a full build for some reason.")
public class ZeroMqSourceInvalidConfigTests {

	@Test
	public void testEmptyConnectionUrl() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {

			TestPropertyValues.of("zeromq.supplier.connectUrl:").applyTo(context);
			context.register(Config.class);
			context.refresh();
			fail("BeanCreationException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(BeanCreationException.class));
			assertThat(extractedValidationMessage(e), containsString("connectUrl is required like tcp://server:port"));
		}
	}

	@Test
	public void testNegativeBindPort() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {

			TestPropertyValues.of("zeromq.supplier.bindPort:-1").applyTo(context);
			context.register(Config.class);
			context.refresh();
			fail("BeanCreationException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(BeanCreationException.class));
			assertThat(extractedValidationMessage(e), containsString("'bindPort' must not be negative"));
		}
	}

	private String extractedValidationMessage(Exception e) {
		BindValidationException bindValidationException = (BindValidationException) e.getCause().getCause();
		ValidationErrors validationErrors = bindValidationException.getValidationErrors();
		FieldError fieldError = (FieldError) validationErrors.getAllErrors().get(0);

		return fieldError.getDefaultMessage();
	}

	@Configuration
	@EnableConfigurationProperties(ZeroMqSupplierProperties.class)
	static class Config {
	}
}
