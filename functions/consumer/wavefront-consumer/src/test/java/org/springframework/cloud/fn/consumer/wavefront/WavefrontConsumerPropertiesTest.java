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

package org.springframework.cloud.fn.consumer.wavefront;

import java.util.Arrays;
import java.util.List;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Timo Salm
 */
public class WavefrontConsumerPropertiesTest {

	private final Expression testExpression = new SpelExpressionParser().parseExpression("#jsonPath(payload,'$')");
	private Validator validator;

	@BeforeEach
	public void setUp() {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@Test
	void testRequiredProperties() {
		final WavefrontConsumerProperties properties = new WavefrontConsumerProperties("v", "v", testExpression, null, null, null, null, "proxy");
		final List<String> emptyValues = Arrays.asList(null, "");

		emptyValues.forEach(emptyValue -> {
			assertThat(validator.validate(properties).isEmpty()).isTrue();
			properties.setMetricName(emptyValue);
			assertThat(validator.validate(properties).isEmpty()).isFalse();
			properties.setMetricName("v");

			assertThat(validator.validate(properties).isEmpty()).isTrue();
			properties.setSource(emptyValue);
			assertThat(validator.validate(properties).isEmpty()).isFalse();
			properties.setSource("v");
		});
		assertThat(validator.validate(properties).isEmpty()).isTrue();
		properties.setMetricExpression(null);
		assertThat(validator.validate(properties).isEmpty()).isFalse();
		properties.setMetricExpression(testExpression);
	}

	@Test
	void testValidMetricNameValues() {
		final WavefrontConsumerProperties properties = new WavefrontConsumerProperties("v", "v", testExpression, null, null, null, null, "proxy");
		final List<String> validMetricNameValues = Arrays.asList("b", "B", "2", ".", "/", "_", ",", "-", "c.8W-2h_dE_,J-h/");
		assertThat(validator.validate(properties).isEmpty()).isTrue();

		validMetricNameValues.forEach(validMetricNameValue -> {
			properties.setMetricName(validMetricNameValue);
			assertThat(validator.validate(properties).isEmpty()).isTrue();
		});

		final List<String> invalidMetricNameValues = Arrays.asList(" ", ":", "a B", "#");
		invalidMetricNameValues.forEach(invalidMetricNameValue -> {
			properties.setMetricName(invalidMetricNameValue);
			assertThat(validator.validate(properties).isEmpty()).isFalse();
		});
	}

	@Test
	void testValidSourceValues() {
		final WavefrontConsumerProperties properties = new WavefrontConsumerProperties("v", "v", testExpression, null, null, null, null, "proxy");
		final List<String> validSourceValues = Arrays.asList("b", "B", "2", ".", "_", "-", "c.8W-2h_dE_J-h",
				createStringOfLength(128));
		assertThat(validator.validate(properties).isEmpty()).isTrue();

		validSourceValues.forEach(validSourceValue -> {
			properties.setSource(validSourceValue);
			assertThat(validator.validate(properties).isEmpty()).isTrue();
		});

		final List<String> invalidSourceValues = Arrays.asList(" ", ":", "a B", "#", "/", ",", createStringOfLength(129));
		invalidSourceValues.forEach(invalidSourceValue -> {
			properties.setSource(invalidSourceValue);
			assertThat(validator.validate(properties).isEmpty()).isFalse();
		});
	}

	private String createStringOfLength(int length) {
		char[] chars = new char[length];
		Arrays.fill(chars, 'a');
		return new String(chars);
	}
}
