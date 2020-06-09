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

package org.springframework.cloud.fn.consumer.websocket;

import io.netty.handler.logging.LogLevel;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Oliver Moser
 * @author Gary Russell
 */
@ConfigurationProperties("websocket.consumer")
public class WebsocketConsumerProperties {

	/**
	 * Default log level.
	 */
	public static final String DEFAULT_LOGLEVEL = LogLevel.WARN.toString();

	/**
	 * Default path.
	 */
	public static final String DEFAULT_PATH = "/websocket";

	/**
	 * Default number of threads.
	 */
	public static final int DEFAULT_THREADS = 1;

	/**
	 * Default port.
	 */
	public static final int DEFAULT_PORT = 9292;

	/**
	 * whether or not to create a {@link io.netty.handler.ssl.SslContext}.
	 */
	boolean ssl;

	/**
	 * the port on which the Netty server listens. Default is <tt>9292</tt>
	 */
	int port = DEFAULT_PORT;

	/**
	 * the number of threads for the Netty {@link io.netty.channel.EventLoopGroup}. Default is <tt>1</tt>
	 */
	int threads = DEFAULT_THREADS;

	/**
	 * the logLevel for netty channels. Default is <tt>WARN</tt>
	 */
	String logLevel = DEFAULT_LOGLEVEL;

	/**
	 * the path on which a WebsocketSink consumer needs to connect. Default is <tt>/websocket</tt>
	 */
	String path = DEFAULT_PATH;

	public boolean isSsl() {
		return ssl;
	}

	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getThreads() {
		return threads;
	}

	public void setThreads(int threads) {
		this.threads = threads;
	}

	public String getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
