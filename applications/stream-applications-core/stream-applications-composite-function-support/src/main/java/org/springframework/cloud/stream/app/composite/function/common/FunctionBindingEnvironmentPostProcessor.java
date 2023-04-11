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

package org.springframework.cloud.stream.app.composite.function.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Map default input and output to corresponding Spring Cloud Stream function binding
 * channels for declared function name.
 * @author David Turanski
 */

public class FunctionBindingEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private static Log log = LogFactory.getLog(FunctionBindingEnvironmentPostProcessor.class);

	private static final String DEFAULT_INPUT = "input";

	private static final String DEFAULT_OUTPUT = "output";

	private static final String OUT_0 = "-out-0";

	private static final String IN_0 = "-in-0";

	private static final String SPRING_CLOUD_STREAM_FUNCTION_BINDINGS_PREFIX = "spring.cloud.stream.function.bindings.";

	private static final String SPRING_CLOUD_STREAM_FUNCTION_DEFINITION = "spring.cloud.stream.function.definition";

	private static final String SPRING_CLOUD_FUNCTION_DEFINITION = "spring.cloud.function.definition";

	private static final String SPRING_CLOUD_STREAM_BINDINGS_PREFIX = "spring.cloud.stream.bindings";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		String functionDefinition = normalizeFunctionDefinition(environment);
		if (StringUtils.isEmpty(functionDefinition)) {
			return;
		}

		Binder binder = new Binder(ConfigurationPropertySources.get(environment));
		BindResult<HashMap> bindResult = binder.bind(SPRING_CLOUD_STREAM_BINDINGS_PREFIX, HashMap.class);
		if (bindResult.isBound()) {
			Map<String, Object> functionBindings = new HashMap<>();

			bindResult.get().keySet().forEach(bindingName -> {
				if (bindingName.equals(DEFAULT_OUTPUT)) {
					log.debug("Binding " + bindingName + " to output for function definition" + functionDefinition);
					String key = functionBindingKeyName(functionDefinition, OUT_0);
					functionBindings.put(key, bindingName);
				}
				if (bindingName.equals(DEFAULT_INPUT)) {
					log.debug("Binding " + bindingName + " to function input for function definition"
							+ functionDefinition);
					String key = functionBindingKeyName(functionDefinition, IN_0);
					functionBindings.put(key, bindingName);
				}
			});
			if (!functionBindings.isEmpty()) {
				environment.getPropertySources().addFirst(new MapPropertySource("function-bindings", functionBindings));
			}
		}
	}

	private String functionBindingKeyName(String functionDefinition, String suffix) {
		return SPRING_CLOUD_STREAM_FUNCTION_BINDINGS_PREFIX + functionDefinitionToChannelName(functionDefinition)
				+ suffix;
	}

	private String functionDefinitionToChannelName(String functionDefinition) {
		return functionDefinition.replace("|", "");
	}

	private String normalizeFunctionDefinition(ConfigurableEnvironment environment) {
		String functionDefinition = null;
		if (environment.containsProperty(SPRING_CLOUD_FUNCTION_DEFINITION)) {
			functionDefinition = sanitizeActualFunctionDefinition(
					environment.getProperty(SPRING_CLOUD_FUNCTION_DEFINITION));
		}
		if (environment.containsProperty(SPRING_CLOUD_STREAM_FUNCTION_DEFINITION)) {
			log.error("The property '" + SPRING_CLOUD_STREAM_FUNCTION_DEFINITION + "' is deprecated. Please use '"
					+ SPRING_CLOUD_FUNCTION_DEFINITION + "'");
			functionDefinition = sanitizeActualFunctionDefinition(
					environment.getProperty(SPRING_CLOUD_STREAM_FUNCTION_DEFINITION));
		}

		if (StringUtils.hasText(functionDefinition)) {
			if (!functionDefinition.equals(environment.getProperty(SPRING_CLOUD_FUNCTION_DEFINITION))) {
				environment.getPropertySources().addFirst(new MapPropertySource("spring-cloud-function-definition",
						Collections.singletonMap(SPRING_CLOUD_FUNCTION_DEFINITION, functionDefinition)));
			}
		}
		return environment.getProperty(SPRING_CLOUD_FUNCTION_DEFINITION);
	}

	private String sanitizeActualFunctionDefinition(String functionDefinition) {
		return functionDefinition.replaceAll("[\'\"]", "").replace(",", "|");
	}
}
