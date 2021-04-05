/*
 * Copyright 2021-2021 the original author or authors.
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

package org.springframework.cloud.stream.app.processor.filter;

import java.util.function.Function;

import reactor.core.publisher.Flux;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.CompositeMessageConverter;

/**
 * Since the commit mentioned below in Spring Cloud Stream removed the logic of
 * converting the incoming text data back to byte[], we are adding an extra
 * processing function to do that convrsion.
 *
 * https://github.com/spring-cloud/spring-cloud-stream/commit/5d9de8ad579d3464d1503d1a5d1390168bccbdb9
 *
 * @author Soby Chacko
 */
@Configuration
public class FilterPostProcessConfiguration {

	@Bean
	public Function<Flux<Message<?>>, Flux<Message<?>>> filterPostProcessFunction(
			CompositeMessageConverter messageConverter) {

		return flux -> flux.map(message -> {
			@SuppressWarnings("unchecked")
			Message<byte[]> outboundMessage = message.getPayload() instanceof byte[]
					? (Message<byte[]>) message : (Message<byte[]>) messageConverter
					.toMessage(message.getPayload(), message.getHeaders());
			if (outboundMessage == null) {
				throw new IllegalStateException("Failed to convert message: '" + message
						+ "' to outbound message.");
			}
			return outboundMessage;
		});
	}
}
