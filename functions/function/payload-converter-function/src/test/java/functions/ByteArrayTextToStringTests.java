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

package functions;

import java.util.Collections;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ByteArrayTextToStringTests {

	private static final String MESSAGE = "hello world";
	private static Function<Message<?>, Message<?>> converter;

	@BeforeAll
	static void before() {
		converter = new ByteArrayTextToString();
	}

	@Test
	public void testDefaultNoContentType() {
		Message<?> converted = converter.apply(new GenericMessage<>(MESSAGE.getBytes()));
		assertThat(converted).isNotNull().extracting(Message::getPayload).isEqualTo(MESSAGE);

		converted = converter.apply(new GenericMessage<>(MESSAGE)); // String
		assertThat(converted).isNotNull().extracting(Message::getPayload).isEqualTo(MESSAGE);
	}

	@Test
	public void testApplicationJsonContentType() {
		Message<?> converted = converter.apply(new GenericMessage<>(MESSAGE.getBytes(),
				Collections.singletonMap(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)));
		assertThat(converted).isNotNull().extracting(Message::getPayload).isEqualTo("hello world");
	}

	@Test
	public void testPlainTextContentType() {
		Message<?> converted = converter.apply(new GenericMessage<>(MESSAGE.getBytes(),
				Collections.singletonMap(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE)));
		assertThat(converted).isNotNull().extracting(Message::getPayload).isEqualTo(MESSAGE);
	}

	@Test
	public void testOctetContentType() {
		Message<?> converted = converter.apply(new GenericMessage<>(MESSAGE.getBytes(),
				Collections.singletonMap(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE)));
		assertThat(converted).isNotNull().extracting(Message::getPayload).isEqualTo(MESSAGE.getBytes());
	}

	@Test
	public void testRandomNonTextContentType() {
		Message<?> converted = converter.apply(new GenericMessage<>(MESSAGE.getBytes(),
				Collections.singletonMap(MessageHeaders.CONTENT_TYPE, "Random Content Type")));
		assertThat(converted).isNotNull().extracting(Message::getPayload).isEqualTo(MESSAGE.getBytes());
	}

}
