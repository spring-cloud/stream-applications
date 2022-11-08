/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.cloud.fn.supplier.rabbit;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("rabbit.supplier")
@Validated
public class RabbitSupplierProperties {

	/**
	 * Whether rejected messages should be requeued.
	 */
	private boolean requeue = true;

	/**
	 * Whether the channel is transacted.
	 */
	private boolean transacted = false;

	/**
	 * The queues to which the source will listen for messages.
	 */
	private String[] queues;

	/**
	 * Headers that will be mapped.
	 */
	private String[] mappedRequestHeaders = {"STANDARD_REQUEST_HEADERS"};

	/**
	 * Initial retry interval when retry is enabled.
	 */
	private int initialRetryInterval = 1000;

	/**
	 * Max retry interval when retry is enabled.
	 */
	private int maxRetryInterval = 30000;

	/**
	 * Retry backoff multiplier when retry is enabled.
	 */
	private double retryMultiplier = 2.0;

	/**
	 * The maximum delivery attempts when retry is enabled.
	 */
	private int maxAttempts = 3;

	/**
	 * true to enable retry.
	 */
	private boolean enableRetry = false;

	/**
	 * When true, use a separate connection based on the boot properties.
	 */
	private boolean ownConnection;

	public boolean getRequeue() {
		return requeue;
	}

	public void setRequeue(boolean requeue) {
		this.requeue = requeue;
	}

	public boolean getTransacted() {
		return transacted;
	}

	public void setTransacted(boolean transacted) {
		this.transacted = transacted;
	}

	@NotNull(message = "queue(s) are required")
	@Size(min = 1, message = "At least one queue is required")
	public String[] getQueues() {
		return queues;
	}

	public void setQueues(String[] queues) {
		this.queues = queues;
	}

	@NotNull
	public String[] getMappedRequestHeaders() {
		return mappedRequestHeaders;
	}

	public void setMappedRequestHeaders(String[] mappedRequestHeaders) {
		this.mappedRequestHeaders = mappedRequestHeaders;
	}

	public int getInitialRetryInterval() {
		return initialRetryInterval;
	}

	public void setInitialRetryInterval(int initialRetryInterval) {
		this.initialRetryInterval = initialRetryInterval;
	}

	public int getMaxRetryInterval() {
		return maxRetryInterval;
	}

	public void setMaxRetryInterval(int maxRetryInterval) {
		this.maxRetryInterval = maxRetryInterval;
	}

	public double getRetryMultiplier() {
		return retryMultiplier;
	}

	public void setRetryMultiplier(double retryMultiplier) {
		this.retryMultiplier = retryMultiplier;
	}

	public int getMaxAttempts() {
		return maxAttempts;
	}

	public void setMaxAttempts(int maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	public boolean isEnableRetry() {
		return enableRetry;
	}

	public void setEnableRetry(boolean enableRetry) {
		this.enableRetry = enableRetry;
	}

	public boolean isOwnConnection() {
		return this.ownConnection;
	}

	public void setOwnConnection(boolean ownConnection) {
		this.ownConnection = ownConnection;
	}
}
