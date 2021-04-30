/*
 * Copyright 2020-2021 the original author or authors.
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

package org.springframework.cloud.fn.consumer.elasticsearch;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;

/**
 * @author Soby Chacko
 * @author Andrea Montemaggio
 */
@ConfigurationProperties("elasticsearch.consumer")
public class ElasticsearchConsumerProperties {

	/**
	 * The id of the document to index.
	 * If set, the INDEX_ID header value overrides this property on a per message basis.
	 */
	Expression id;

	/**
	 * Name of the index.
	 * If set, the INDEX_NAME header value overrides this property on a per message basis.
	 */
	String index;

	/**
	 * Indicates the shard to route to.
	 * If not provided, Elasticsearch will default to a hash of the document id.
	 */
	String routing;

	/**
	 * Timeout for the shard to be available.
	 * If not set, it defaults to 1 minute set by the Elasticsearch client.
	 */
	long timeoutSeconds;

	/**
	 * Indicates whether the indexing operation is async or not.
	 * By default indexing is done synchronously.
	 */
	boolean async;

	/**
	 * Number of items to index for each request. It defaults to 1.
	 * For values greater than 1 bulk indexing API will be used.
	 */
	int batchSize = 1;

	/**
	 * Timeout in milliseconds after which message group is flushed when bulk indexing is active.
	 * It defaults to -1, meaning no automatic flush of idle message groups occurs.
	 */
	long groupTimeout = -1L;

	public Expression getId() {
		return id;
	}

	public void setId(Expression id) {
		this.id = id;
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getRouting() {
		return routing;
	}

	public void setRouting(String routing) {
		this.routing = routing;
	}

	public long getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(long timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public boolean isAsync() {
		return async;
	}

	public void setAsync(boolean async) {
		this.async = async;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public long getGroupTimeout() {
		return groupTimeout;
	}

	public void setGroupTimeout(long groupTimeout) {
		this.groupTimeout = groupTimeout;
	}
}
