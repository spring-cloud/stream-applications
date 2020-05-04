/*
 * Copyright (c) 2011-2020 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.fn.spel;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spel.function.expression=payload.toUpperCase()")
@DirtiesContext
public class SpelFunctionApplicationTests {

	@Autowired
	Function<Message<?>, Message<?>> transformer;

	@Test
	public void testTransform() {
		final Message<?> transformed = this.transformer.apply(new GenericMessage<>("hello,world"));
		assertThat(transformed.getPayload()).isEqualTo("HELLO,WORLD");
	}

	@Test
	public void testJson() {
		Message<?> message = MessageBuilder.withPayload("{\"foo\":\"bar\"}")
				.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON).build();
		final Message<?> transformed = this.transformer.apply(message);
		assertThat(transformed.getPayload()).isEqualTo("{\"FOO\":\"BAR\"}");
	}

	@SpringBootApplication
	static class TestApplication {

	}
}
