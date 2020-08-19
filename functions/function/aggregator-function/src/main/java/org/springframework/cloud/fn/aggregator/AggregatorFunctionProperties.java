/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.fn.aggregator;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;

/**
 * Configuration properties for the Aggregator function.
 *
 * @author Artem Bilan
 */
@ConfigurationProperties("aggregator")
public class AggregatorFunctionProperties {

	static final String PREFIX = "aggregator";

	/**
	 * SpEL expression for correlation key. Default to correlationId header.
	 */
	private Expression correlation;

	/**
	 * SpEL expression for release strategy. Default is based on the sequenceSize header.
	 */
	private Expression release;

	/**
	 * SpEL expression for aggregation strategy. Default is collection of payloads.
	 */
	private Expression aggregation;

	/**
	 * SpEL expression for timeout to expiring uncompleted groups.
	 */
	private Expression groupTimeout;

	/**
	 * Message store type.
	 */
	private String messageStoreType = MessageStoreType.SIMPLE;

	/**
	 * Persistence message store entity: table prefix in RDBMS, collection name in MongoDb, etc.
	 */
	private String messageStoreEntity;

	public Expression getCorrelation() {
		return this.correlation;
	}

	public void setCorrelation(Expression correlation) {
		this.correlation = correlation;
	}

	public Expression getRelease() {
		return this.release;
	}

	public void setRelease(Expression release) {
		this.release = release;
	}

	public Expression getAggregation() {
		return this.aggregation;
	}

	public void setAggregation(Expression aggregation) {
		this.aggregation = aggregation;
	}

	public Expression getGroupTimeout() {
		return this.groupTimeout;
	}

	public void setGroupTimeout(Expression groupTimeout) {
		this.groupTimeout = groupTimeout;
	}

	public String getMessageStoreEntity() {
		return this.messageStoreEntity;
	}

	public void setMessageStoreEntity(String messageStoreEntity) {
		this.messageStoreEntity = messageStoreEntity;
	}

	public String getMessageStoreType() {
		return this.messageStoreType;
	}

	public void setMessageStoreType(String messageStoreType) {
		this.messageStoreType = messageStoreType;
	}

	static final class MessageStoreType {

		static final String SIMPLE = "simple";

		static final String JDBC = "jdbc";

		static final String MONGODB = "mongodb";

		static final String REDIS = "redis";

		static final String GEMFIRE = "gemfire";

	}

}
