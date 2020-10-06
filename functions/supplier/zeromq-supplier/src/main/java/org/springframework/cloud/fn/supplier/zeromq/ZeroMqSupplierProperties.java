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

import java.util.Arrays;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.zeromq.SocketType;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("zeromq.supplier")
@Validated
public class ZeroMqSupplierProperties {

	private String connectUrl;
	private String[] topics = {""};

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
	public void setConnectUrl(String connectUrl) {
		this.connectUrl = connectUrl;
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

	public SocketType getReceiveSocketType() {
		return SocketType.SUB;
	}

	@Override
	public String toString() {
		return "ZeroMqSupplierProperties{" +
				"connectUrl='" + connectUrl + '\'' +
				", topics=" + Arrays.toString(topics) +
				'}';
	}

}
