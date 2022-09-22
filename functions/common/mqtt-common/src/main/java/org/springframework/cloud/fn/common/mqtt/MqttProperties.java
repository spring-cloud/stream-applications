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

package org.springframework.cloud.fn.common.mqtt;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Generic mqtt connection properties.
 *
 * @author Janne Valkealahti
 * @author Artem Bilan
 */
@Validated
@ConfigurationProperties("mqtt")
public class MqttProperties {

	/**
	 * location of the mqtt broker(s) (comma-delimited list).
	 */
	private String[] url = new String[] { "tcp://localhost:1883" };

	/**
	 * the username to use when connecting to the broker.
	 */
	private String username = "guest";

	/**
	 * the password to use when connecting to the broker.
	 */
	private String password = "guest";

	/**
	 * whether the client and server should remember state across restarts and reconnects.
	 */
	private boolean cleanSession = true;

	/**
	 * the connection timeout in seconds.
	 */
	private int connectionTimeout = 30;

	/**
	 * the ping interval in seconds.
	 */
	private int keepAliveInterval = 60;

	/**
	 * 'memory' or 'file'.
	 */
	private String persistence = "memory";

	/**
	 * Persistence directory.
	 */
	private String persistenceDirectory = "/tmp/paho";

	/**
	 * MQTT Client SSL properties.
	 */
	private final Map<String, String> sslProperties = new HashMap<>();

	@Size(min = 1)
	public String[] getUrl() {
		return url;
	}

	public void setUrl(String[] url) {
		this.url = url;
	}

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

	public boolean isCleanSession() {
		return cleanSession;
	}

	public void setCleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}

	public int getKeepAliveInterval() {
		return keepAliveInterval;
	}

	public void setKeepAliveInterval(int keepAliveInterval) {
		this.keepAliveInterval = keepAliveInterval;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public String getPersistence() {
		return persistence;
	}

	public void setPersistence(String persistence) {
		this.persistence = persistence;
	}

	public String getPersistenceDirectory() {
		return persistenceDirectory;
	}

	public void setPersistenceDirectory(String persistenceDirectory) {
		this.persistenceDirectory = persistenceDirectory;
	}

	public Map<String, String> getSslProperties() {
		return this.sslProperties;
	}

}
