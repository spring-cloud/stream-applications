/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.cloud.fn.consumer.kafka;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;

/**
 * Properties for the Kafka Consumer function.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
@ConfigurationProperties("kafka.consumer")
public class KafkaConsumerProperties {

	/**
	 * Kafka topic - overridden by topicExpression, if supplied. Defaults to KafkaTemplate.getDefaultTopic()
	 */
	private String topic;

	/**
	 * A SpEL expression that evaluates to a Kafka topic.
	 */
	private Expression topicExpression;

	/**
	 * Kafka record key - overridden by keyExpression, if supplied.
	 */
	private String key;

	/**
	 * A SpEL expression that evaluates to a Kafka record key.
	 */
	private Expression keyExpression;

	/**
	 * Kafka topic partition - overridden by partitionExpression, if supplied.
	 */
	private Integer partition;

	/**
	 * A SpEL expression that evaluates to a Kafka topic partition.
	 */
	private Expression partitionExpression;

	/**
	 * Kafka record timestamp - overridden by timestampExpression, if supplied.
	 */
	private Long timestamp;

	/**
	 * A SpEL expression that evaluates to a Kafka record timestamp.
	 */
	private Expression timestampExpression;

	/**
	 * True if Kafka producer handler should operation in a sync mode.
	 */
	private boolean sync;

	/**
	 * How long Kafka producer handler should wait for send operation results. Defaults to 10 seconds.
	 */
	private Duration sendTimeout = Duration.ofSeconds(10);

	/**
	 * Headers that will be mapped.
	 */
	private String[] mappedHeaders = { "*" };

	/**
	 * Whether to use the template's message converter to create a Kafka record.
	 */
	private boolean useTemplateConverter;

	public String getTopic() {
		return this.topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public Expression getTopicExpression() {
		return this.topicExpression;
	}

	public void setTopicExpression(Expression topicExpression) {
		this.topicExpression = topicExpression;
	}

	public String getKey() {
		return this.key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Expression getKeyExpression() {
		return this.keyExpression;
	}

	public void setKeyExpression(Expression keyExpression) {
		this.keyExpression = keyExpression;
	}

	public Integer getPartition() {
		return this.partition;
	}

	public void setPartition(Integer partition) {
		this.partition = partition;
	}

	public Expression getPartitionExpression() {
		return this.partitionExpression;
	}

	public void setPartitionExpression(Expression partitionExpression) {
		this.partitionExpression = partitionExpression;
	}

	public Long getTimestamp() {
		return this.timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public Expression getTimestampExpression() {
		return this.timestampExpression;
	}

	public void setTimestampExpression(Expression timestampExpression) {
		this.timestampExpression = timestampExpression;
	}

	public boolean isSync() {
		return this.sync;
	}

	public void setSync(boolean sync) {
		this.sync = sync;
	}

	public Duration getSendTimeout() {
		return this.sendTimeout;
	}

	public void setSendTimeout(Duration sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public String[] getMappedHeaders() {
		return this.mappedHeaders;
	}

	public void setMappedHeaders(String[] mappedHeaders) {
		this.mappedHeaders = mappedHeaders;
	}

	public boolean isUseTemplateConverter() {
		return this.useTemplateConverter;
	}

	public void setUseTemplateConverter(boolean useTemplateConverter) {
		this.useTemplateConverter = useTemplateConverter;
	}

}
