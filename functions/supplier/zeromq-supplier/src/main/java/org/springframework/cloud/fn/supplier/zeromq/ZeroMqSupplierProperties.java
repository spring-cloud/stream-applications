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

package org.springframework.cloud.fn.supplier.zeromq;

import java.time.Duration;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;
import org.zeromq.SocketType;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 *
 * @author Daniel Frey
 * @since 3.1.0
 */
@ConfigurationProperties("zeromq.supplier")
@Validated
public class ZeroMqSupplierProperties {

	/**
	 * The Socket Type the connection should make.
	 */
	private SocketType socketType = SocketType.SUB;

	/**
	 * Connection URL for to the ZeroMQ Socket.
	 */
	private String connectUrl;

	/**
	 * Bind Port for creating a ZeroMQ Socket; 0 selects a random port.
	 */
	private int bindPort;

	/**
	 * The delay to consume from the ZeroMQ Socket when no data received.
	 */
	private Duration consumeDelay = Duration.ofSeconds(1);

	/**
	 * The Topics to subscribe to.
	 */
	private String[] topics = {""};

	/**
	 * @param socketType the {@link SocketType} to establish.
	 */
	public void setSocketType(SocketType socketType) {
		this.socketType = socketType;
	}

	@NotNull(message = "'socketType' is required")
	public SocketType getSocketType() {
		return socketType;
	}

	@NotEmpty(message = "connectUrl is required like tcp://server:port")
	public String getConnectUrl() {
		return connectUrl;
	}

	/**
	 *
	 * @param connectUrl The ZeroMQ server connect url
	 *
	 * @see org.springframework.integration.zeromq.inbound.ZeroMqMessageProducer#setConnectUrl(String)
	 */
	public void setConnectUrl(String connectUrl) {
		this.connectUrl = connectUrl;
	}

	@Range(min = 0, message = "'bindPort' must not be negative")
	public int getBindPort() {
		return bindPort;
	}

	/**
	 * @param bindPort The Port to bind to on all interfaces
	 *
	 * @see org.springframework.integration.zeromq.inbound.ZeroMqMessageProducer#setBindPort(int)
	 */
	public void setBindPort(int bindPort) {
		this.bindPort = bindPort;
	}

	@NotNull(message = "'consumeDelay' is required")
	public Duration getConsumeDelay() {
		return consumeDelay;
	}

	/**
	 * Specify a {@link Duration} to delay consumption when no data received.
	 * @param consumeDelay the {@link Duration} to delay consumption when empty.
	 */
	public void setConsumeDelay(Duration consumeDelay) {
		this.consumeDelay = consumeDelay;
	}

	public String[] getTopics() {
		return topics;
	}

	/**
	 *
	 * @param topics The ZeroMQ Topics to subscribe to
	 *
	 * @see org.springframework.integration.zeromq.inbound.ZeroMqMessageProducer#setTopics(String...)
	 */
	public void setTopics(String... topics) {
		this.topics = topics;
	}

}
