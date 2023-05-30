/*
 * Copyright 2023-2023 the original author or authors.
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

package functions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;

/**
 * The {@link Function} to deserialize {@code byte[]} payload into a Map
 * if {@link MessageHeaders#CONTENT_TYPE} header is JSON.
 * Otherwise, the message is returned as is.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
public class JsonBytesToMap implements Function<Message<?>, Message<?>> {

	private final ObjectMapper objectMapper;

	public JsonBytesToMap(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public Message<?> apply(Message<?> message) {
		if (message.getPayload() instanceof byte[] payload) {
			MessageHeaders headers = message.getHeaders();
			String contentType =
					headers.containsKey(MessageHeaders.CONTENT_TYPE)
							? headers.get(MessageHeaders.CONTENT_TYPE).toString()
							: MimeTypeUtils.APPLICATION_JSON_VALUE;

			if (contentType.contains("json")) {
				message = MessageBuilder.withPayload(payloadToMapIfCan(payload))
						.copyHeaders(message.getHeaders())
						.build();
			}
		}

		return message;
	}

	private Object payloadToMapIfCan(byte[] payload) {
		try {
			return this.objectMapper.readValue(payload, Map.class);
		}
		catch (IOException ex) {
			// Was not able to construct the map from byte[] -- returning as is
			return payload;
		}
	}

}
