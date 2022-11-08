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

package org.springframework.cloud.fn.common.ftp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.net.ftp.FTPClient;
import org.hibernate.validator.constraints.Range;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("ftp.factory")
@Validated
public class FtpSessionFactoryProperties {

	/**
	 * The port of the server.
	 */
	private int port = 21;

	/**
	 * The client mode to use for the FTP session.
	 */
	private ClientMode clientMode = ClientMode.PASSIVE;

	/**
	 * The host name of the server.
	 */
	private String host = "localhost";

	/**
	 * The username to use to connect to the server.
	 */

	private String username;
	/**
	 * The password to use to connect to the server.
	 */
	private String password;

	/**
	 * Cache sessions.
	 */
	private Boolean cacheSessions;

	@Range(min = 0, max = 65535)
	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@NotNull
	public ClientMode getClientMode() {
		return this.clientMode;
	}

	public void setClientMode(ClientMode clientMode) {
		this.clientMode = clientMode;
	}

	public enum ClientMode {

		/**
		 * Active client mode.
		 */
		ACTIVE(FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE),
		/**
		 * Passive client mode.
		 */
		PASSIVE(FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE);

		private final int mode;

		ClientMode(int mode) {
			this.mode = mode;
		}

		public int getMode() {
			return mode;
		}

	}

	@NotBlank
	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	@NotBlank
	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Boolean getCacheSessions() {
		return this.cacheSessions;
	}

	public void setCacheSessions(Boolean cacheSessions) {
		this.cacheSessions = cacheSessions;
	}

}
