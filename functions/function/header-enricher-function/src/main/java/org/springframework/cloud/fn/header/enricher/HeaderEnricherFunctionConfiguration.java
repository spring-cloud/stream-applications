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

package org.springframework.cloud.fn.header.enricher;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.integration.transformer.HeaderEnricher;
import org.springframework.integration.transformer.support.ExpressionEvaluatingHeaderValueMessageProcessor;
import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @author Christian Tzolov
 * @author Soby Chacko
 */
@Configuration
@EnableConfigurationProperties(HeaderEnricherFunctionProperties.class)
@ConditionalOnProperty(prefix = "header.enricher", value = "headers")
public class HeaderEnricherFunctionConfiguration {

	@Autowired
	private HeaderEnricherFunctionProperties properties;

	@Bean
	public Function<Message<?>, Message<?>> headerEnricherFunction() {
		return headerEnricher()::transform;
	}

	@Bean
	public HeaderEnricher headerEnricher() {
		Map<String, ExpressionEvaluatingHeaderValueMessageProcessor<?>> headersToAdd = new HashMap<>();
		Properties props = this.properties.getHeaders();
		Enumeration<?> enumeration = props.propertyNames();
		while (enumeration.hasMoreElements()) {
			String propertyName = (String) enumeration.nextElement();
			headersToAdd.put(propertyName, processor(props.getProperty(propertyName)));
		}
		HeaderEnricher headerEnricher = new HeaderEnricher(headersToAdd);
		headerEnricher.setDefaultOverwrite(this.properties.isOverwrite());
		return headerEnricher;
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE) // Need a new processor for each header
	public ExpressionEvaluatingHeaderValueMessageProcessor<?> processor(String expression) {
		return new ExpressionEvaluatingHeaderValueMessageProcessor<>(expression, null);
	}

}
