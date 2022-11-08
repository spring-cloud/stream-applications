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

package org.springframework.cloud.fn.consumer.mqtt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Range;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Properties for the Mqtt Consumer.
 *
 * @author Janne Valkealahti
 *
 */
@Validated
@ConfigurationProperties("mqtt.consumer")
public class MqttConsumerProperties {

	/**
	 * identifies the client.
	 */
	private String clientId = "stream.client.id.sink";

	/**
	 * the topic to which the sink will publish.
	 */
	private String topic = "stream.mqtt";

	/**
	 * the quality of service to use.
	 */
	private int qos = 1;

	/**
	 * whether to set the 'retained' flag.
	 */
	private boolean retained = false;

	/**
	 * the charset used to convert a String payload to byte[].
	 */
	private String charset = "UTF-8";

	/**
	 * whether or not to use async sends.
	 */
	private boolean async = false;

	@Range(min = 0, max = 2)
	public int getQos() {
		return this.qos;
	}

	public boolean isRetained() {
		return this.retained;
	}

	public void setQos(int qos) {
		this.qos = qos;
	}

	public void setRetained(boolean retained) {
		this.retained = retained;
	}

	@NotBlank
	@Size(min = 1, max = 23)
	public String getClientId() {
		return this.clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	@NotBlank
	public String getTopic() {
		return this.topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getCharset() {
		return this.charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public boolean isAsync() {
		return this.async;
	}

	public void setAsync(boolean async) {
		this.async = async;
	}
}
