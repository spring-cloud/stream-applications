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

package org.springframework.cloud.fn.supplier.jms;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Properties for the JMS Supplier.
 *
 * @author Gary Russell
 *
 */
@ConfigurationProperties(prefix = "jms.supplier")
@Validated
public class JmsSupplierProperties {

	/**
	 * True to enable transactions and select a DefaultMessageListenerContainer, false to
	 * select a SimpleMessageListenerContainer.
	 */
	private boolean sessionTransacted = true;

	/**
	 * Client id for durable subscriptions.
	 */
	private String clientId;

	/**
	 * The destination from which to receive messages (queue or topic).
	 */
	private String destination;

	/**
	 * The name of a durable or shared subscription.
	 */
	private String subscriptionName;

	/**
	 * A selector for messages.
	 */
	private String messageSelector = null;

	/**
	 * True for a durable subscription.
	 */
	private Boolean subscriptionDurable;

	/**
	 * True for a shared subscription.
	 */
	private Boolean subscriptionShared;

	public String getClientId() {
		return this.clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	@NotNull
	public String getDestination() {
		return this.destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public String getSubscriptionName() {
		return this.subscriptionName;
	}

	public void setSubscriptionName(String subscriptionName) {
		this.subscriptionName = subscriptionName;
	}

	public String getMessageSelector() {
		return this.messageSelector;
	}

	public void setMessageSelector(String messageSelector) {
		this.messageSelector = messageSelector;
	}

	public Boolean getSubscriptionDurable() {
		return this.subscriptionDurable;
	}

	public void setSubscriptionDurable(Boolean subscriptionDurable) {
		this.subscriptionDurable = subscriptionDurable;
	}

	public Boolean getSubscriptionShared() {
		return this.subscriptionShared;
	}

	public void setSubscriptionShared(Boolean subscriptionShared) {
		this.subscriptionShared = subscriptionShared;
	}

	public boolean isSessionTransacted() {
		return this.sessionTransacted;
	}

	public void setSessionTransacted(boolean sessionTransacted) {
		this.sessionTransacted = sessionTransacted;
	}
}
