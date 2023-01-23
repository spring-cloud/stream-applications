/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.cloud.stream.app.sink.router;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;

/**
 * @author Artem Bilan
 */
class BindingChannelResolver implements DestinationResolver<MessageChannel> {

	private final Map<String, MessageChannel> bindingChannelCache = new ConcurrentHashMap<>();

	private final BindingService bindingService;

	private final StreamBridge streamBridge;

	private final boolean resolutionRequired;

	BindingChannelResolver(BindingService bindingService, StreamBridge streamBridge, boolean resolutionRequired) {
		this.bindingService = bindingService;
		this.streamBridge = streamBridge;
		this.resolutionRequired = resolutionRequired;
	}

	@Override
	public MessageChannel resolveDestination(String name) throws DestinationResolutionException {
		return this.bindingChannelCache.computeIfAbsent(name, this::createMessageChannelProxyForBinding);
	}

	private MessageChannel createMessageChannelProxyForBinding(String bindingName) {
		if (this.resolutionRequired && this.bindingService.getProducerBinding(bindingName) == null) {
			throw new DestinationResolutionException("Binding for name [" + bindingName + "] is not provided.");
		}

		return (message, timeout) -> this.streamBridge.send(bindingName, message);
	}

}
