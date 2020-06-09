/*
 * Copyright 2018-2020 the original author or authors.
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

package org.springframework.cloud.fn.supplier.websocket;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties available for Websocket Supplier.
 *
 * @author Krishnaprasad A S
 * @author Artem Bilan
 *
 */
@ConfigurationProperties("websocket.supplier")
public class WebsocketSupplierProperties {

	/**
	 * Default path.
	 */
	public static final String DEFAULT_PATH = "/websocket";

	/**
	 * Default allowed origins.
	 */
	public static final String DEFAULT_ALLOWED_ORIGINS = "*";

	/**
	 * The path on which server WebSocket handler is exposed.
	 */
	private String path = DEFAULT_PATH;

	/**
	 * The allowed origins.
	 */
	private String allowedOrigins = DEFAULT_ALLOWED_ORIGINS;

	/**
	 * The SockJS options.
	 */
	private SockJs sockJs = new SockJs();

	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getAllowedOrigins() {
		return this.allowedOrigins;
	}

	public void setAllowedOrigins(String allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	public SockJs getSockJs() {
		return this.sockJs;
	}

	public void setSockJs(SockJs sockJs) {
		this.sockJs = sockJs;
	}

	public static class SockJs {

		/**
		 * Enable SockJS service on the server. Default is 'false'
		 */
		private boolean enable;

		public boolean getEnable() {
			return this.enable;
		}

		public void setEnable(boolean enable) {
			this.enable = enable;
		}

	}
}
