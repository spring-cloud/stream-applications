/*
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.fn.consumer.cassandra;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.integration.cassandra.outbound.CassandraMessageHandler;

import com.datastax.oss.driver.api.core.ConsistencyLevel;

/**
 * @author Artem Bilan
 * @author Thomas Risberg
 */
@ConfigurationProperties("cassandra")
public class CassandraConsumerProperties {

	/**
	 * Time-to-live option of WriteOptions.
	 */
	private int ttl;

	/**
	 * QueryType for Cassandra Sink.
	 */
	private CassandraMessageHandler.Type queryType;

	/**
	 * Ingest Cassandra query.
	 */
	private String ingestQuery;

	/**
	 * Expression in Cassandra query DSL style.
	 */
	private Expression statementExpression;

	/**
	 * The consistency level for write operation.
	 */
	private ConsistencyLevel consistencyLevel;

	public int getTtl() {
		return this.ttl;
	}

	public void setTtl(int ttl) {
		this.ttl = ttl;
	}

	public CassandraMessageHandler.Type getQueryType() {
		return this.queryType;
	}

	public void setQueryType(CassandraMessageHandler.Type queryType) {
		this.queryType = queryType;
	}

	public String getIngestQuery() {
		return this.ingestQuery;
	}

	public void setIngestQuery(String ingestQuery) {
		this.ingestQuery = ingestQuery;
	}

	public Expression getStatementExpression() {
		return this.statementExpression;
	}

	public void setStatementExpression(Expression statementExpression) {
		this.statementExpression = statementExpression;
	}

	public ConsistencyLevel getConsistencyLevel() {
		return this.consistencyLevel;
	}

	public void setConsistencyLevel(ConsistencyLevel consistencyLevel) {
		this.consistencyLevel = consistencyLevel;
	}

}
