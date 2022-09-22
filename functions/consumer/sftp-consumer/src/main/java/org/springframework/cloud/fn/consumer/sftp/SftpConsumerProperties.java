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

package org.springframework.cloud.fn.consumer.sftp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.expression.Expression;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.validation.annotation.Validated;

/**
 * @author Gary Russell
 * @author Artem Bilan
 */
@ConfigurationProperties("sftp.consumer")
@Validated
public class SftpConsumerProperties {

	private final Factory factory = new Factory();

	/**
	 * A temporary directory where the file will be written if 'isUseTemporaryFilename()' is true.
	 */
	private String temporaryRemoteDir = "/";

	/**
	 * Whether or not to create the remote directory.
	 */
	private boolean autoCreateDir = true;

	/**
	 * Action to take if the remote file already exists.
	 */
	private FileExistsMode mode = FileExistsMode.REPLACE;

	/**
	 * Whether or not to write to a temporary file and rename.
	 */
	private boolean useTemporaryFilename = true;

	/**
	 * A SpEL expression to generate the remote file name.
	 */
	private String filenameExpression;

	/**
	 * The remote FTP directory.
	 */
	private String remoteDir = "/";

	/**
	 * The suffix to use while the transfer is in progress.
	 */
	private String tmpFileSuffix = ".tmp";

	/**
	 * The remote file separator.
	 */
	private String remoteFileSeparator = "/";

	@NotBlank
	public String getTemporaryRemoteDir() {
		return this.temporaryRemoteDir;
	}

	public void setTemporaryRemoteDir(String temporaryRemoteDir) {
		this.temporaryRemoteDir = temporaryRemoteDir;
	}

	public boolean isAutoCreateDir() {
		return this.autoCreateDir;
	}

	public void setAutoCreateDir(boolean autoCreateDir) {
		this.autoCreateDir = autoCreateDir;
	}

	@NotNull
	public FileExistsMode getMode() {
		return this.mode;
	}

	public void setMode(FileExistsMode mode) {
		this.mode = mode;
	}

	public boolean isUseTemporaryFilename() {
		return this.useTemporaryFilename;
	}

	public void setUseTemporaryFilename(boolean useTemporaryFilename) {
		this.useTemporaryFilename = useTemporaryFilename;
	}

	public String getFilenameExpression() {
		return this.filenameExpression;
	}

	public void setFilenameExpression(String filenameExpression) {
		this.filenameExpression = filenameExpression;
	}

	@NotBlank
	public String getRemoteDir() {
		return remoteDir;
	}

	public final void setRemoteDir(String remoteDir) {
		this.remoteDir = remoteDir;
	}

	@NotBlank
	public String getTmpFileSuffix() {
		return tmpFileSuffix;
	}

	public void setTmpFileSuffix(String tmpFileSuffix) {
		this.tmpFileSuffix = tmpFileSuffix;
	}

	@NotBlank
	public String getRemoteFileSeparator() {
		return remoteFileSeparator;
	}

	public void setRemoteFileSeparator(String remoteFileSeparator) {
		this.remoteFileSeparator = remoteFileSeparator;
	}

	public Factory getFactory() {
		return this.factory;
	}

	public static class Factory {

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

		/**
		 * The port of the server.
		 */
		private int port = 22;

		/**
		 * Resource location of user's private key.
		 */
		private Resource privateKey;

		/**
		 * Passphrase for user's private key.
		 */
		private String passPhrase = "";

		/**
		 * True to allow an unknown or changed key.
		 */
		private boolean allowUnknownKeys = false;

		/**
		 * A SpEL expression resolving to the location of the known hosts file.
		 */
		private Expression knownHostsExpression = null;


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

		@Range(min = 0, max = 65535)
		public int getPort() {
			return this.port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public Resource getPrivateKey() {
			return this.privateKey;
		}

		public void setPrivateKey(Resource privateKey) {
			this.privateKey = privateKey;
		}

		public String getPassPhrase() {
			return this.passPhrase;
		}

		public void setPassPhrase(String passPhrase) {
			this.passPhrase = passPhrase;
		}

		public boolean isAllowUnknownKeys() {
			return this.allowUnknownKeys;
		}

		public void setAllowUnknownKeys(boolean allowUnknownKeys) {
			this.allowUnknownKeys = allowUnknownKeys;
		}

		public Expression getKnownHostsExpression() {
			return this.knownHostsExpression;
		}

		public void setKnownHostsExpression(Expression knownHosts) {
			this.knownHostsExpression = knownHosts;
		}

	}
}
