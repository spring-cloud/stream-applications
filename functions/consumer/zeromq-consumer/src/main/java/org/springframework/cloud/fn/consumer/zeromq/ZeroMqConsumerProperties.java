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

package org.springframework.cloud.fn.consumer.zeromq;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.zeromq.SocketType;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.validation.annotation.Validated;

/**
 *
 * @author Daniel Frey
 * @since 3.1.0
 */
@ConfigurationProperties("zeromq.consumer")
@Validated
public class ZeroMqConsumerProperties {

	/**
	 * The Socket Type the connection should establish.
	 */
	private SocketType socketType = SocketType.PUB;

	/**
	 * Connection URL for connecting to the ZeroMQ Socket.
	 */
	private String connectUrl;

	/**
	 * A Topic SpEL expression to evaluate a topic before sending messages to subscribers.
	 */
	private Expression topic;

	@NotNull(message = "'socketType' is required")
	public SocketType getSocketType() {
		return socketType;
	}

	/**
	 * @param socketType the {@link SocketType} to establish.
	 */
	public void setSocketType(SocketType socketType) {
		this.socketType = socketType;
	}

	@NotEmpty(message = "connectUrl is required like protocol://server:port")
	public String getConnectUrl() {
		return connectUrl;
	}

	/**
	 * @param connectUrl The ZeroMQ socket to expose
	 */
	public void setConnectUrl(String connectUrl) {
		this.connectUrl = connectUrl;
	}

	public Expression getTopic() {
		return topic;
	}

	/**
	 * @param topic The 'topic' SpEL expression to set
	 */
	public void setTopic(Expression topic) {
		this.topic = topic;
	}

}
