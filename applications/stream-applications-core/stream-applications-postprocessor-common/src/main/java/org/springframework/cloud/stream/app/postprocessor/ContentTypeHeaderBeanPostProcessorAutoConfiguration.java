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

package org.springframework.cloud.stream.app.postprocessor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.stream.messaging.DirectWithAttributesChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;

@AutoConfiguration
public class ContentTypeHeaderBeanPostProcessorAutoConfiguration {

	private static Log log = LogFactory.getLog(ContentTypeHeaderBeanPostProcessorAutoConfiguration.class);

	@Bean
	BeanPostProcessor contentTypeHeaderBeanPostProcessor() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				if (bean instanceof DirectWithAttributesChannel) {
					DirectWithAttributesChannel directWithAttributesChannel = (DirectWithAttributesChannel) bean;
					if ("input".equals(directWithAttributesChannel.getAttribute("type"))) {
						directWithAttributesChannel.addInterceptor(new ChannelInterceptor() {
							@Override
							public Message<?> preSend(Message<?> message, MessageChannel channel) {
								Message<?> newMessage = message;
								if (message.getHeaders().containsKey("originalContentType")) {
									if (log.isDebugEnabled()) {
										log.debug("Copying 'originalContentType' header value "
												+ message.getHeaders().get("originalContentType") + " to "
												+ MessageHeaders.CONTENT_TYPE + " header");
									}
									newMessage = MessageBuilder.fromMessage(message)
											.setHeader(MessageHeaders.CONTENT_TYPE,
													message.getHeaders().get("originalContentType"))
											.build();
								}
								return newMessage;
							}
						});
					}
				}
				return bean;
			}
		};
	}

}
