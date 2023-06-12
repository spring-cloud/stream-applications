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

package org.springframework.cloud.fn.supplier.kafka;

import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;

/**
 * Auto-configuration properties for the Kafka Supplier.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
@ConfigurationProperties("kafka.supplier")
public class KafkaSupplierProperties {

	/**
	 * Apache Kafka topics to subscribe.
	 */
	private String[] topics;

	/**
	 * Apache Kafka topics pattern to subscribe.
	 */
	private Pattern topicPattern;

	/**
	 * Whether to acknowledge discarded records after 'RecordFilterStrategy'.
	 */
	private boolean ackDiscarded;

	/**
	 * SpEL expression for 'RecordFilterStrategy' with a 'ConsumerRecord' as a root object.
	 */
	Expression recordFilter;

	public String[] getTopics() {
		return this.topics;
	}

	public void setTopics(String[] topics) {
		this.topics = topics;
	}

	public Pattern getTopicPattern() {
		return this.topicPattern;
	}

	public void setTopicPattern(Pattern topicPattern) {
		this.topicPattern = topicPattern;
	}

	public boolean isAckDiscarded() {
		return this.ackDiscarded;
	}

	public void setAckDiscarded(boolean ackDiscarded) {
		this.ackDiscarded = ackDiscarded;
	}

	public Expression getRecordFilter() {
		return this.recordFilter;
	}

	public void setRecordFilter(Expression recordFilter) {
		this.recordFilter = recordFilter;
	}

}
