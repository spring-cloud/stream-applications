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

package org.springframework.cloud.fn.consumer.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.util.MimeType;

/**
 * @author Artem Bilan
 */
@SpringBootTest({ "log.name=foo", "log.level=warn", "log.expression=payload.toUpperCase()" })
@ExtendWith(OutputCaptureExtension.class)
class LogConsumerApplicationTests {

	@Autowired
	private Consumer<Message<?>> logConsumer;

	@Autowired
	@Qualifier("logConsumerFlow.logging-channel-adapter#0")
	private LoggingHandler loggingHandler;

	@Test
	public void testJsonContentType(CapturedOutput output) {
		String payload = "{\"foo\":\"bar\"}";
		Message<String> message = MessageBuilder.withPayload(payload)
				.setHeader("contentType", new MimeType("json"))
				.build();
		assertThat(this.loggingHandler.getLevel()).isEqualTo(LoggingHandler.Level.WARN);
		LogAccessor logAccessor = TestUtils.getPropertyValue(this.loggingHandler, "messageLogger", LogAccessor.class);
		assertThat(TestUtils.getPropertyValue(logAccessor.getLog(), "name")).isEqualTo("foo");
		this.logConsumer.accept(message);
		Awaitility.await().until(output::getOut, value -> value.contains(payload.toUpperCase()));
		this.loggingHandler.setLogExpressionString("#this");
		this.logConsumer.accept(message);
		Awaitility.await().until(output::getOut, value -> value.contains("payload=" + payload));
		assertThat(output.getOut()).contains("json/*");
	}

	@SpringBootApplication
	static class LogConsumerTestApplication {

	}

}
