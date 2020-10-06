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

	private static final SocketType DEFAULT_SOCKET_TYPE = SocketType.SUB;
	private static final Duration DEFAULT_CONSUME_DELAY = Duration.ofSeconds(1);
	private static final List<SocketType> VALID_SOCKET_TYPES =
			Arrays.asList(SocketType.PAIR, SocketType.PULL, SocketType.SUB);

	private final AtomicInteger bindPort = new AtomicInteger();

	/**
	 * The {@link SocketType} the connection should make.
	 *
	 * Allowable values:
	 * - PAIR
	 * - PULL
	 * - SUB
	 */
	private SocketType socketType = DEFAULT_SOCKET_TYPE;

	/**
	 * Connection URL for {@link org.zeromq.ZMQ.Socket#connect(String)}.
	 * Mutually exclusive with the {@link #bindPort}.
	 */
	private String connectUrl;

	/**
	 * A {@link Duration} to delay consumption when no data received.
	 */
	private Duration consumeDelay = DEFAULT_CONSUME_DELAY;

	/**
	 * Topics the {@link SocketType#SUB} socket is going to use for subscription.
	 * It is ignored for all other {@link SocketType}s supported.
	 */
	private String[] topics = {""};

	/**
	 * @param socketType the {@link SocketType} to establish,
	 *                      defaults to {@link #DEFAULT_SOCKET_TYPE} if empty
	 */
	@NotNull(message = "'socketType' is required")
	public void setSocketType(SocketType socketType) {
		Assert.state(VALID_SOCKET_TYPES.contains(socketType),
				() -> "'socketType' can only be one of the: " + VALID_SOCKET_TYPES);
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

	public Duration getConsumeDelay() {
		return consumeDelay;
	}

	/**
	 * @param bindPort The TCP Port to bind to on all interfaces
	 *
	 * @see org.springframework.integration.zeromq.inbound.ZeroMqMessageProducer#setBindPort(int)
	 */
	@Range(min = 0, message = "'bindPort' must not be zero or negative")
	public void setBindPort(int bindPort) {
		this.bindPort.set(bindPort);
	}

	public int getBoundPort() {
		return bindPort.get();
	}

	/**
	 * Specify a {@link Duration} to delay consumption when no data received.
	 * @param consumeDelay the {@link Duration} to delay consumption when empty;
	 *                     defaults to {@link #DEFAULT_CONSUME_DELAY}.
	 */
	@NotNull(message = "'consumeDelay' must not be null")
	public void setConsumeDelay(Duration consumeDelay) {
		this.consumeDelay = consumeDelay;
	}

	@NotNull(message = "topics(s) are required")
	@Size(min = 1, message = "At least one topic is required")
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
