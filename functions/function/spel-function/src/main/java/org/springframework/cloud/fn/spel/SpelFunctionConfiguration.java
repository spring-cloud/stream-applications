/*
 * Copyright (c) 2020 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.fn.spel;

import java.util.function.Function;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.transformer.ExpressionEvaluatingTransformer;
import org.springframework.messaging.Message;

@Configuration
@EnableConfigurationProperties(SpelFunctionProperties.class)
public class SpelFunctionConfiguration {

	@Bean
	public Function<Message<?>, Message<?>> spelFunction(
			ExpressionEvaluatingTransformer expressionEvaluatingTransformer) {

		return message -> expressionEvaluatingTransformer.transform(message);
	}

	@Bean
	public ExpressionEvaluatingTransformer expressionEvaluatingTransformer(
			SpelFunctionProperties spelFunctionProperties) {

		return new ExpressionEvaluatingTransformer(new SpelExpressionParser()
				.parseExpression(spelFunctionProperties.getExpression()));
	}

}
