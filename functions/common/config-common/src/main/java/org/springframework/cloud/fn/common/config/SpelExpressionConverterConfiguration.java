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

package org.springframework.cloud.fn.common.config;

import java.beans.Introspector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.config.IntegrationConverter;
import org.springframework.integration.expression.SpelPropertyAccessorRegistrar;
import org.springframework.integration.json.JsonPropertyAccessor;

@AutoConfiguration
@AutoConfigureAfter(name = "org.springframework.cloud.stream.config.SpelExpressionConverterConfiguration")
@ConditionalOnMissingClass("org.springframework.cloud.stream.config.SpelExpressionConverterConfiguration")
public class SpelExpressionConverterConfiguration {

	/**
	 * Specific Application Context name to be used as Bean qualifier when the {@link EvaluationContext} is injected.
	 */
	public static final String INTEGRATION_EVALUATION_CONTEXT = "integrationEvaluationContext";

	@Bean
	public static SpelPropertyAccessorRegistrar spelPropertyAccessorRegistrar() {
		return (new SpelPropertyAccessorRegistrar())
				.add(Introspector.decapitalize(JsonPropertyAccessor.class.getSimpleName()), new JsonPropertyAccessor());
	}

	@Bean
	@ConfigurationPropertiesBinding
	@IntegrationConverter
	public Converter<String, Expression> spelConverter() {
		return new SpelExpressionConverterConfiguration.SpelConverter();
	}

	public static class SpelConverter implements Converter<String, Expression> {
		private SpelExpressionParser parser = new SpelExpressionParser();

		@Autowired
		@Qualifier(INTEGRATION_EVALUATION_CONTEXT)
		@Lazy
		private EvaluationContext evaluationContext;

		public SpelConverter() {
		}

		public Expression convert(String source) {
			try {
				Expression expression = this.parser.parseExpression(source);
				if (expression instanceof SpelExpression) {
					((SpelExpression) expression).setEvaluationContext(this.evaluationContext);
				}

				return expression;
			}
			catch (ParseException var3) {
				throw new IllegalArgumentException(
						String.format("Could not convert '%s' into a SpEL expression", source), var3);
			}
		}
	}
}
