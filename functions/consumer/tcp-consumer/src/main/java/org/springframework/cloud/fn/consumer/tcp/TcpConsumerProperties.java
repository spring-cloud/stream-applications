/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.cloud.fn.consumer.tcp;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.fn.common.tcp.Encoding;
import org.springframework.validation.annotation.Validated;

/**
 * Properties for the TCP Consumer.
 *
 * @author Gary Russell
 * @author Christian Tzolov
 *
 */
@ConfigurationProperties("tcp.consumer")
@Validated
public class TcpConsumerProperties {

	/**
	 * The host to which this sink will connect.
	 */
	private String host;

	/**
	 * The encoder to use when sending messages.
	 */
	private Encoding encoder = Encoding.CRLF;

	/**
	 * The charset used when converting from bytes to String.
	 */
	private String charset = "UTF-8";

	/**
	 * Whether to close the socket after each message.
	 */
	private boolean close;

	@NotNull
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	@NotNull
	public Encoding getEncoder() {
		return this.encoder;
	}

	public void setEncoder(Encoding encoder) {
		this.encoder = encoder;
	}

	@NotNull
	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public boolean isClose() {
		return close;
	}

	public void setClose(boolean close) {
		this.close = close;
	}
}
