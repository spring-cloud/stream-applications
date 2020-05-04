/*
 * Copyright 2020 the original author or authors.
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

import java.util.function.Function;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;

/**
 *
 * @author Christian Tzolov
 */
public class ByteArrayTextToString implements Function<Message<?>, Message<?>> {

	@Override
	public Message<?> apply(Message<?> message) {

		if (message.getPayload() instanceof byte[]) {
			final MessageHeaders headers = message.getHeaders();
			String contentType = headers.containsKey(MessageHeaders.CONTENT_TYPE)
					? headers.get(MessageHeaders.CONTENT_TYPE).toString()
					: MimeTypeUtils.APPLICATION_JSON_VALUE;

			if (contentType.contains("text") || contentType.contains("json") || contentType.contains("x-spring-tuple")) {
				message = MessageBuilder.withPayload(new String(((byte[]) message.getPayload())))
						.copyHeaders(message.getHeaders())
						.build();
			}
		}

		return message;
	}
}

