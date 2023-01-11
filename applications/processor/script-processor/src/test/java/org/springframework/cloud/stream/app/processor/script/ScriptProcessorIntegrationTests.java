/*
 * Copyright 2015-2023 the original author or authors.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for the Script Processor.
 *
 * @author Andy Clement
 * @author Artem Bilan
 * @author Gary Russell
 * @author Chris Schaefer
 * @author Soby Chacko
 */
public class ScriptProcessorIntegrationTests {

	@Test
	public void testJavascriptFunctions() throws IOException {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(ScriptProcessorTestConfiguration.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=scriptProcessorFunction",
						"--script-processor.script=function add(a,b) { return a+b;}; add(1,3)",
						"--script-processor.language=js")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			processorInput.send(new GenericMessage<>("hello world"));
			Message<byte[]> sourceMessage = processorOutput.receive(10000, "scriptProcessorFunction-out-0");
			assertThat(new String(sourceMessage.getPayload())).matches(s -> s.equals("4") || s.equals("4.0"));
			ObjectMapper objectMapper = new ObjectMapper();
			final Integer deserializedValue = objectMapper.readValue(sourceMessage.getPayload(), Integer.class);
			assertThat(deserializedValue).matches(i -> i == 4 || i == 4.0);
		}
	}

	@Test
	public void testJavascriptVariableTake1() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(ScriptProcessorTestConfiguration.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=scriptProcessorFunction",
						"--script-processor.variables=foo=\\\40WORLD",
						"--script-processor.script=payload+foo",
						"--script-processor.language=js")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			processorInput.send(new GenericMessage<>("hello world"));
			Message<byte[]> sourceMessage = processorOutput.receive(10000, "scriptProcessorFunction-out-0");
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("hello world WORLD");
		}
	}

	@Test
	public void testJavascriptVariableTake2() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(ScriptProcessorTestConfiguration.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=scriptProcessorFunction",
						"--script-processor.variables=limit=5",
						"--script-processor.script=payload*limit",
						"--script-processor.language=js")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			processorInput.send(new GenericMessage<>(9));
			Message<byte[]> sourceMessage = processorOutput.receive(10000, "scriptProcessorFunction-out-0");
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("45");
		}
	}

	@Test
	public void testGroovyBasic() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(ScriptProcessorTestConfiguration.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=scriptProcessorFunction",
						"--script-processor.script=payload+foo",
						"--script-processor.language=groovy",
						"--script-processor.variables=foo=\\\40WORLD")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			processorInput.send(new GenericMessage<>("hello world"));
			Message<byte[]> sourceMessage = processorOutput.receive(10000, "scriptProcessorFunction-out-0");
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("hello world WORLD");
		}
	}

	@Test
	public void testGroovyComplex() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(ScriptProcessorTestConfiguration.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=scriptProcessorFunction",
						"--script-processor.script=payload.substring(0, limit as int) + foo",
						"--script-processor.language=groovy",
						"--script-processor.variables=limit=5 \n foo=\\\40WORLD")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			processorInput.send(new GenericMessage<>("hello world"));
			Message<byte[]> sourceMessage = processorOutput.receive(10000, "scriptProcessorFunction-out-0");
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("hello WORLD");
		}
	}

	@Test
	public void testRubyScript() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(ScriptProcessorTestConfiguration.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=scriptProcessorFunction",
						"--script-processor.script=return \"\"#{payload.upcase}\"\"",
						"--script-processor.language=ruby")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			processorInput.send(new GenericMessage<>("hello world"));
			Message<byte[]> sourceMessage = processorOutput.receive(10000, "scriptProcessorFunction-out-0");
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("HELLO WORLD");
		}
	}

	@Test
	public void testRubyScriptComplex() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(ScriptProcessorTestConfiguration.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=scriptProcessorFunction",
						"--script-processor.script=\"def foo(x)\\n  return x+5\\nend\\nfoo(payload)\\n\"",
						"--script-processor.language=ruby")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			processorInput.send(new GenericMessage<>(9));
			Message<byte[]> sourceMessage = processorOutput.receive(10000, "scriptProcessorFunction-out-0");
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("14");
		}
	}

	@Test
	public void testPython() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(ScriptProcessorTestConfiguration.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=scriptProcessorFunction",
						"--script-processor.script=\"def multiply(x,y):\\n  return x*y\\nanswer = multiply(payload,5)\\n\"",
						"--script-processor.language=python")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			processorInput.send(new GenericMessage<>(6));
			Message<byte[]> sourceMessage = processorOutput.receive(10000, "scriptProcessorFunction-out-0");
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("30");
		}
	}

	@Test
	public void testPythonComplex() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(ScriptProcessorTestConfiguration.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=scriptProcessorFunction",
						"--script-processor.script=\"def concat(x,y):\\n  return x+y\\nanswer = concat(\"\"hello \"\",payload)\\n\"",
						"--script-processor.language=python")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			processorInput.send(new GenericMessage<>("world"));
			Message<byte[]> sourceMessage = processorOutput.receive(10000, "scriptProcessorFunction-out-0");
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("hello world");
		}
	}

	@Test
	public void testGroovyToJavascript() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(ScriptProcessorTestConfiguration.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=scriptProcessorFunction",
						"--script-processor.script=\"@Grab('org.grooscript:grooscript:1.3.0')\\n import org.grooscript.GrooScript\\n GrooScript.convert(new String(payload))\\n\"",
						"--script-processor.language=groovy")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			processorInput.send(new GenericMessage<>("def age=18"));
			Message<byte[]> sourceMessage = processorOutput.receive(10000, "scriptProcessorFunction-out-0");
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("var age = 18;"
					+ System.getProperty("line.separator"));
		}
	}

	@EnableAutoConfiguration
	@Import(ScriptProcessorConfiguration.class)
	public static class ScriptProcessorTestConfiguration {
	}

}
