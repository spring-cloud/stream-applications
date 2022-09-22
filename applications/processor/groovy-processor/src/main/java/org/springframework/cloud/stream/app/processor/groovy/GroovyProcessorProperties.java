/*
 * Copyright 2015-2016 the original author or authors.
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

import java.util.Properties;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the Groovy Transform Processor module.
 *
 * @author Eric Bottard
 */
@ConfigurationProperties("groovy-processor")
@Validated
public class GroovyProcessorProperties {

	/**
	 * Reference to a script used to process messages.
	 */
	private Resource script;

	/**
	 * Variable bindings as a new line delimited string of name-value pairs, e.g. 'foo=bar\n baz=car'.
	 */
	private Properties variables;

	/**
	 * The location of a properties file containing custom script variable bindings.
	 */
	private Resource variablesLocation;

	@NotNull
	public Resource getScript() {
		return script;
	}

	public void setScript(Resource script) {
		this.script = script;
	}


	public Properties getVariables() {
		return variables;
	}

	public void setVariables(Properties variables) {
		this.variables = variables;
	}

	public Resource getVariablesLocation() {
		return variablesLocation;
	}

	public void setVariablesLocation(Resource variablesLocation) {
		this.variablesLocation = variablesLocation;
	}
}
