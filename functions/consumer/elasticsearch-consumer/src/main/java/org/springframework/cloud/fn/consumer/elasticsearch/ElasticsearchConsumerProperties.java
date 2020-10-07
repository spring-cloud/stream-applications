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

package org.springframework.cloud.fn.consumer.elasticsearch;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;

@ConfigurationProperties("elasticsearch.consumer")
public class ElasticsearchConsumerProperties {

	/**
	 * The id of the document index.
	 */
	Expression id;

	/**
	 * Name of the index.
	 */
	String index;

	/**
	 * Indicates the shard to route to.
	 * If not provided, this resolves to the ID used on the document.
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
}
