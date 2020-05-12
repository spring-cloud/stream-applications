/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.stream.app.source.load.generator;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Holds configuration options for the LoadGenerator source.
 *
 * @author Glenn Renfro
 */
@ConfigurationProperties("load-generator")
public class LoadGeneratorSourceProperties {

	/**
	 * Number of producers.
	 */
	private int producers = 1;

	/**
	 * Message size.
	 */
	private int messageSize = 1000;

	/**
	 * Message count.
 	 */
	private int messageCount = 1000;

	/**
	 * Whether timestamp generated.
	 */
	private boolean generateTimestamp = false;

	public int getProducers() {
		return producers;
	}

	public void setProducers(int producers) {
		this.producers = producers;
	}

	public int getMessageSize() {
		return messageSize;
	}

	public void setMessageSize(int messageSize) {
		this.messageSize = messageSize;
	}

	public int getMessageCount() {
		return messageCount;
	}

	public void setMessageCount(int messageCount) {
		this.messageCount = messageCount;
	}

	public boolean isGenerateTimestamp() {
		return generateTimestamp;
	}

	public void setGenerateTimestamp(boolean generateTimestamp) {
		this.generateTimestamp = generateTimestamp;
	}
}
