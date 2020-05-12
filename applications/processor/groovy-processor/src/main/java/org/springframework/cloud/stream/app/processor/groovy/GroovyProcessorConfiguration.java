/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.cloud.stream.app.processor.groovy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.integration.groovy.GroovyScriptExecutingMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.scripting.DefaultScriptVariableGenerator;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.messaging.Message;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.util.CollectionUtils;

/**
 * A Processor app that transforms messages using a Groovy script.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Gary Russell
 * @author Christian Tzolov
 * @author Soby Chacko
 */
@Configuration
@EnableConfigurationProperties(GroovyProcessorProperties.class)
public class GroovyProcessorConfiguration {

	@Autowired
	private GroovyProcessorProperties properties;

	@Bean
	public Function<Message<?>, Object> groovyProcessorFunction(ScriptVariableGenerator scriptVariableGenerator) {
		return message -> transformer(scriptVariableGenerator).processMessage(message);
	}

	@Bean
	public MessageProcessor<?> transformer(ScriptVariableGenerator scriptVariableGenerator) {
		return new GroovyScriptExecutingMessageProcessor(
				new ResourceScriptSource(properties.getScript()), scriptVariableGenerator);
	}

	@Bean(name = "variableGenerator")
	public ScriptVariableGenerator scriptVariableGenerator() throws IOException {
		Map<String, Object> variables = new HashMap<>();
		CollectionUtils.mergePropertiesIntoMap(properties.getVariables(), variables);
		if (properties.getVariablesLocation() != null) {
			CollectionUtils.mergePropertiesIntoMap(
					PropertiesLoaderUtils.loadProperties(properties.getVariablesLocation()), variables);
		}
		return new DefaultScriptVariableGenerator(variables);
	}
}
