/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.fn.consumer.log;

import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.MimeType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Artem Bilan
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest({ "log.name=foo", "log.level=warn", "log.expression=payload.toUpperCase()" })
class LogConsumerApplicationTests {

	@Autowired
	private Consumer<Message<?>> logConsumer;

	@Autowired
	@Qualifier("logConsumerFlow.logging-channel-adapter#0")
	private LoggingHandler loggingHandler;

	@Test
	public void testJsonContentType() {
		Message<String> message = MessageBuilder.withPayload("{\"foo\":\"bar\"}")
				.setHeader("contentType", new MimeType("json"))
				.build();
		testMessage(message, "{\"foo\":\"bar\"}");
	}

	private void testMessage(Message<?> message, String expectedPayload) {
		assertThat(this.loggingHandler.getLevel()).isEqualTo(LoggingHandler.Level.WARN);
		Log logger = TestUtils.getPropertyValue(this.loggingHandler, "messageLogger", Log.class);
		assertThat(TestUtils.getPropertyValue(logger, "logger.name")).isEqualTo("foo");
		logger = spy(logger);
		new DirectFieldAccessor(this.loggingHandler).setPropertyValue("messageLogger", logger);
		this.logConsumer.accept(message);
		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		verify(logger).warn(captor.capture());
		assertThat(captor.getValue()).isEqualTo(expectedPayload.toUpperCase());
		this.loggingHandler.setLogExpressionString("#this");
		this.logConsumer.accept(message);
		verify(logger, times(2)).warn(captor.capture());

		Message<?> captorMessage = (Message<?>) captor.getAllValues().get(2);
		assertThat(captorMessage.getPayload()).isEqualTo(expectedPayload);

		MessageHeaders messageHeaders = captorMessage.getHeaders();
		assertThat(messageHeaders).hasSize(3);

		assertThat(messageHeaders)
				.containsEntry(MessageHeaders.CONTENT_TYPE, message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@SpringBootApplication
	static class TestApplication {}
}
