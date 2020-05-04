/*
 * Copyright 2015-2016 the original author or authors.
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
package org.springframework.cloud.stream.app.file.remote;

import org.hibernate.validator.constraints.NotBlank;

/**
 * Common properties for remote servers (e.g. (S)FTP).
 *
 * @deprecated - properties are flattened.
 *
 * @author David Turanski
 * @author Gary Russell
 *
 */
@Deprecated
public abstract class AbstractRemoteServerProperties {

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
	 * Cache sessions
	 */
	private Boolean cacheSessions;

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
