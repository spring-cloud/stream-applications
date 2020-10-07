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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Range;
import org.zeromq.SocketType;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;
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
	 * The {@link SocketType} the connection should make.
	 */
	private SocketType socketType;

	/**
	 * Connection URL for to the ZeroMQ TCP Server.
	 */
	private String connectUrl;

	/**
	 * Bind Port for connecting to the ZeroMQ TCP Server.
	 */
	private Integer bindPort;

	/**
	 * The delay to consume from the ZeroMQ TCP Server when no data received.
	 */
	private Duration consumeDelay;

	/**
	 * The Topics to subscribe to.
	 */
	private String[] topics = {""};

	/**
	 * @param socketType the {@link SocketType} to establish.
	 */
	@NotNull(message = "'socketType' is required")
	public void setSocketType(SocketType socketType) {
		this.socketType = socketType;
	}

	public SocketType getSocketType() {
		return socketType;
	}

	@NotEmpty(message = "connectUrl is required like tcp://server:port")
	public String getConnectUrl() {
		return connectUrl;
	}

	/**
	 *
	 * @param connectUrl The ZeroMQ TCP server connect url
	 *
	 * @see org.springframework.integration.zeromq.inbound.ZeroMqMessageProducer#setConnectUrl(String)
	 */
	@NotEmpty(message = "'connectUrl' must not be empty")
	public void setConnectUrl(String connectUrl) {
		this.connectUrl = connectUrl;
	}

	public int getBindPort() {
		return bindPort;
	}

	/**
	 * @param bindPort The TCP Port to bind to on all interfaces
	 *
	 * @see org.springframework.integration.zeromq.inbound.ZeroMqMessageProducer#setBindPort(int)
	 */
	@Range(min = 0, message = "'bindPort' must not be negative")
	public void setBindPort(Integer bindPort) {
		this.bindPort = bindPort;
	}

	public Duration getConsumeDelay() {
		return consumeDelay;
	}

	/**
	 * Specify a {@link Duration} to delay consumption when no data received.
	 * @param consumeDelay the {@link Duration} to delay consumption when empty.
	 */
	@NotNull(message = "'consumeDelay' must not be null")
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
