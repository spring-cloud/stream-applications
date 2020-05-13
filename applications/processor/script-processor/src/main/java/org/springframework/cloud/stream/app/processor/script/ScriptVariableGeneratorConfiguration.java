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

package org.springframework.cloud.stream.app.processor.script;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.integration.scripting.DefaultScriptVariableGenerator;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.util.CollectionUtils;

/**
 * Configuration that provides a {@link ScriptVariableGenerator} to customize a script.
 *
 * @author David Turanski
 * @author Eric Bottard
 * @author Mark Fisher
 */
@Configuration
public class ScriptVariableGeneratorConfiguration {

	@Autowired
	private ScriptProcessorProperties properties;

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
