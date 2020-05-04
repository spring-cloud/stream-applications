/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.stream.app.ftp;

import javax.validation.constraints.NotNull;

import org.apache.commons.net.ftp.FTPClient;
import org.hibernate.validator.constraints.Range;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.stream.app.file.remote.AbstractRemoteServerProperties;
import org.springframework.validation.annotation.Validated;

/**
 * FTP {@code SessionFactory} properties.
 *
 * @author David Turanski
 * @author Gary Russell
 */
@ConfigurationProperties("ftp.factory")
@Validated
public class FtpSessionFactoryProperties extends AbstractRemoteServerProperties {

	/**
	 * The port of the server.
	 */
	private int port = 21;

	/**
	 * The client mode to use for the FTP session.
	 */
	private ClientMode clientMode = ClientMode.PASSIVE;

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

	public static enum ClientMode {

		ACTIVE(FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE),
		PASSIVE(FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE);

		private final int mode;

		private ClientMode(int mode) {
			this.mode = mode;
		}

		public int getMode() {
			return mode;
		}

	}

}
