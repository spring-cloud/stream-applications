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

package org.springframework.cloud.fn.supplier.mqtt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Properties for the Mqtt Source.
 *
 * @author Janne Valkealahti
 * @author Soby Chacko
 *
 */
@Validated
@ConfigurationProperties("mqtt.supplier")
public class MqttSupplierProperties {

	/**
	 * identifies the client.
	 */
	private String clientId = "stream.client.id.source";

	/**
	 * the topic(s) (comma-delimited) to which the source will subscribe.
	 */
	private String[] topics = new String[] { "stream.mqtt" };

	/**
	 * the qos; a single value for all topics or a comma-delimited list to match the topics.
	 */
	private int[] qos = new int[] { 0 };

	/**
	 * true to leave the payload as bytes.
	 */
	private boolean binary = false;

	/**
	 * the charset used to convert bytes to String (when binary is false).
	 */
	private String charset = "UTF-8";

	@NotBlank
	@Size(min = 1, max = 23)
	public String getClientId() {
		return this.clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String[] getTopics() {
		return this.topics;
	}

	public void setTopics(String[] topics) {
		this.topics = topics;
	}

	public int[] getQos() {
		return this.qos;
	}

	public void setQos(int[] qos) {
		this.qos = qos;
	}

	public String getCharset() {
		return this.charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public boolean isBinary() {
		return this.binary;
	}

	public void setBinary(boolean binary) {
		this.binary = binary;
	}

}
