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

package org.springframework.cloud.fn.common.xmpp;

import jakarta.validation.constraints.NotEmpty;
import org.jivesoftware.smack.roster.Roster;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 *
 * @author Daniel Frey
 * @since 4.0.0
 */
@ConfigurationProperties("xmpp.factory")
@Validated
public class XmppConnectionFactoryProperties {

	/**
	 * The Resource to bind to on the XMPP Host.
	 *   - Can be empty, server will generate one if not set
	 */
	private String resource;

	/**
	 * The User the connection should connect as.
	 */
	private String user;

	/**
	 * The Password for the connected user.
	 */
	private String password;

	/**
	 * The Service Name to set for the XMPP Domain.
	 */
	private String serviceName;

	/**
	 * XMPP Host server to connect to.
	 */
	private String host;

	/**
	 * Port for connecting to the host.
	 *   - Default Client Port: 5222
	 */
	private int port = 5222;

	private Roster.SubscriptionMode subscriptionMode = Roster.getDefaultSubscriptionMode();

	private boolean autoStartup = true;

	/**
	 * @param resource the XMPP to bind to.
	 */
	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getResource() {
		return resource;
	}

	/**
	 * @param user the user to connect as.
	 */
	public void setUser(String user) {
		this.user = user;
	}

	@NotEmpty(message = "user is required")
	public String getUser() {
		return user;
	}

	/**
	 * @param password the user's password.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	@NotEmpty(message = "port is required")
	public String getPassword() {
		return password;
	}

	/**
	 * @param serviceName the xmpp domain to set.
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getServiceName() {
		return serviceName;
	}

	/**
	 * @param host the XMPP host to connect to.
	 *
	 */
	public void setHost(String host) {
		this.host = host;
	}

	@NotEmpty(message = "host is required")
	public String getHost() {
		return host;
	}

	/**
	 * @param port the XMPP port to connect to.
	 */
	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}

	/**
	 * @param subscriptionMode the {@link Roster.SubscriptionMode} to use.
	 */
	public void setSubscriptionMode(Roster.SubscriptionMode subscriptionMode) {
		this.subscriptionMode = subscriptionMode;
	}

	public Roster.SubscriptionMode getSubscriptionMode() {
		return subscriptionMode;
	}

	/**
	 * @param autoStartup to enable/disable auto-startup of the connection factory.
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public boolean isAutoStartup() {
		return autoStartup;
	}

}
