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

package org.springframework.cloud.stream.app.processor.script;

import java.util.Properties;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the Scriptable Transform Processor module.
 *
 * @author Andy Clement
 */
@ConfigurationProperties("script-processor")
@Validated
public class ScriptProcessorProperties {

	/**
	 * Language of the text in the script property. Supported: groovy, javascript, ruby, python.
	 */
	@NotNull
	private String language;

	/*
	 * Extra notes on the script parameter. The UI will typically look after encoding
	 * newlines and double quotes when packaging the value to pass to the script
	 * property. If not using the UI, attempting to define
	 * a script directly in the shell for example, it is important to note:
	 * - newlines should be escaped (\\n)
	 * - a single " should be expressed in a pair "" - the DSL parser recognizes this pattern
	 * - If the script starts and ends with a " then they will be stripped off before treating what is
	 *   left as the script.
	 *
	 * Examples:
	 * ruby: --script="return ""#{payload.upcase}"""
	 * javascript: --script="function double(a) {\\n return a+"" + ""+a;\\n}\\ndouble(payload);"
	 */
	/**
	 * Text of the script.
	 */
	@NotNull
	private String script;

	/**
	 * Variable bindings as a new line delimited string of name-value pairs, e.g. 'foo=bar\n baz=car'.
	 */
	private Properties variables;

	/**
	 * The location of a properties file containing custom script variable bindings.
	 */
	private Resource variablesLocation;


	public String getLanguage() {
		return this.language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getScript() {
		return this.script;
	}

	public void setScript(String script) {
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
