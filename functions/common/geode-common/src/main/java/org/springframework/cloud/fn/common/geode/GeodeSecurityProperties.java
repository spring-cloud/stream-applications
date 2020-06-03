/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.cloud.fn.common.geode;

import java.util.Properties;

import org.apache.geode.LogWriter;
import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.security.AuthInitialize;
import org.apache.geode.security.AuthenticationFailedException;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Geode username/password authentication.
 * @author David Turanski
 **/
@ConfigurationProperties("geode.security")
public class GeodeSecurityProperties {

	/**
	 * The cache username.
	 */
	private String username;

	/**
	 * The cache password.
	 */
	private String password;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@SuppressWarnings("unused")
	public static class UserAuthInitialize implements AuthInitialize {

		private LogWriter securitylog;
		private LogWriter systemlog;

		public static AuthInitialize create() {
			return new UserAuthInitialize();
		}

		@Override
		public void init(LogWriter systemLogger, LogWriter securityLogger) throws AuthenticationFailedException {
			this.systemlog = systemLogger;
			this.securitylog = securityLogger;
		}

		@Override
		public Properties getCredentials(Properties props, DistributedMember server, boolean isPeer) throws AuthenticationFailedException {

			String username = props.getProperty(SECURITY_USERNAME);
			if (username == null) {
				throw new AuthenticationFailedException("UserAuthInitialize: username not set.");
			}

			String password = props.getProperty(SECURITY_PASSWORD);
			if (password == null) {
				throw new AuthenticationFailedException("UserAuthInitialize: password not set.");
			}

			Properties properties = new Properties();
			properties.setProperty(SECURITY_USERNAME, username);
			properties.setProperty(SECURITY_PASSWORD, password);
			return properties;
		}

		@Override
		public void close() {
		}
	}
}
