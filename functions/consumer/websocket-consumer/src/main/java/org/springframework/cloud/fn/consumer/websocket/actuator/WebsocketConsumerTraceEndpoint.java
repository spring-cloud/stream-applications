/*
 * Copyright 2014-2020 the original author or authors.
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

package org.springframework.cloud.fn.consumer.websocket.actuator;

import java.util.List;

import jakarta.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.fn.consumer.websocket.trace.InMemoryTraceRepository;
import org.springframework.cloud.fn.consumer.websocket.trace.Trace;

/**
 * Simple Spring Boot Actuator {@link Endpoint} implementation that
 * provides access to Websocket messages last sent/received.
 *
 * @author Oliver Moser
 * @author Artem Bilan
 */
@ConfigurationProperties(prefix = "endpoints.websocketconsumertrace")
@Endpoint(id = "websocketconsumertrace")
public class WebsocketConsumerTraceEndpoint {

	private static final Log logger = LogFactory.getLog(WebsocketConsumerTraceEndpoint.class);

	private boolean enabled;

	private final InMemoryTraceRepository repository;

	public WebsocketConsumerTraceEndpoint(InMemoryTraceRepository repository) {
		this.repository = repository;
		logger.info(String.format("/websocketsinktrace enabled: %b", this.enabled));
	}

	@PostConstruct
	public void init() {

	}

	@ReadOperation
	public List<Trace> traces() {
		return this.repository.findAll();
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
