/*
 * Copyright 2016-2024 the original author or authors.
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

import java.util.function.Function;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.integration.scripting.dsl.ScriptSpec;
import org.springframework.integration.scripting.dsl.Scripts;
import org.springframework.messaging.Message;

/**
 * A Processor module that transforms messages using a supplied script. The script
 * code is passed in directly via property. For more information on Spring script
 * processing, see
 * <a href=
 * "https://spring.io/blog/2011/12/08/spring-integration-scripting-support-part-1">
 * this blog article</a>.
 *
 * @author Andy Clement
 * @author Gary Russell
 * @author Chris Schaefer
 * @author Artme Bilan
 * @author Soby Chacko
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ScriptProcessorProperties.class)
@Import(ScriptVariableGeneratorConfiguration.class)
public class ScriptProcessorConfiguration {

	private static final String NEWLINE_ESCAPE = Matcher.quoteReplacement("\\n");

	private static final String DOUBLE_DOUBLE_QUOTE = Matcher.quoteReplacement("\"\"");

	private static final Log logger = LogFactory.getLog(ScriptProcessorConfiguration.class);


	private ScriptProcessorProperties properties;

	private ScriptVariableGenerator scriptVariableGenerator;

	public ScriptProcessorConfiguration(ScriptProcessorProperties properties, ScriptVariableGenerator scriptVariableGenerator) {
		this.properties = properties;
		this.scriptVariableGenerator = scriptVariableGenerator;
	}

	@Bean
	public Function<Message<?>, Object> scriptProcessorFunction(MessageProcessor<?> messageProcessor) {
		return messageProcessor::processMessage;
	}

	@Bean
	public ScriptSpec processor() {
		String language = this.properties.getLanguage();
		String script = this.properties.getScript();
		logger.info(String.format("Input script is '%s', language is '%s'", script, language));
		Resource scriptResource = new ByteArrayResource(decodeScript(script).getBytes());

		return Scripts.processor(scriptResource)
				.lang(language)
				.variableGenerator(scriptVariableGenerator);
	}

	private static String decodeScript(String script) {
		String toProcess = script;
		// If it has both a leading and trailing double quote, remove them
		if (toProcess.startsWith("\"") && toProcess.endsWith("\"")) {
			toProcess = script.substring(1, script.length() - 1);
		}
		return toProcess.replaceAll(NEWLINE_ESCAPE, "\n").replaceAll(DOUBLE_DOUBLE_QUOTE, "\"");
	}

}
