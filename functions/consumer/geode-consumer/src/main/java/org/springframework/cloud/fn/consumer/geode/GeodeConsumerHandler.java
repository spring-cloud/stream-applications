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

package org.springframework.cloud.fn.consumer.geode;

import java.util.function.Function;

import org.apache.geode.pdx.PdxInstance;

import org.springframework.cloud.fn.common.geode.JsonPdxFunctions;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;

/**
 * @author David Turanski
 * @author Christian Tzolov
 **/
class GeodeConsumerHandler implements Function<Message<?>, Message<?>> {

	private final Boolean convertToJson;

	private final Function<String, PdxInstance> transformer = JsonPdxFunctions.jsonToPdx();

	GeodeConsumerHandler(Boolean convertToJson) {
		this.convertToJson = convertToJson;
	}

	@Override
	public Message<?> apply(Message<?> message) {
		Message<?> transformedMessage = message;
		Object transformedPayload = message.getPayload();
		if (convertToJson) {

			Object payload = message.getPayload();

			if (payload instanceof byte[]) {
				transformedPayload = transformer.apply(new String((byte[]) payload));
			}
			else if (payload instanceof String) {
				transformedPayload = transformer.apply((String) payload);
			}
			else {
				throw new MessageConversionException(String.format(
						"Cannot convert object of type %s", payload.getClass()
								.getName()));
			}
		}

		return MessageBuilder
				.fromMessage(message)
				.withPayload(transformedPayload)
				.build();
	}
}
