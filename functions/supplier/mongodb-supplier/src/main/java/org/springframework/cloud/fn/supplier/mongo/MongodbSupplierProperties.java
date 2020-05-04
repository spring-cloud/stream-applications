/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.fn.supplier.mongo;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.validation.annotation.Validated;

/**
 * @author Adam Zwickey
 * @author Artem Bilan
 * @author Chris Schaefer
 * @author David Turanski
 *
 */
@ConfigurationProperties("mongodb.supplier")
@Validated
public class MongodbSupplierProperties {

	/**
	 * The MongoDB collection to query
	 */
	private String collection;

	/**
	 * The MongoDB query
	 */
	private String query = "{ }";

	/**
	 * The SpEL expression in MongoDB query DSL style
	 */
	private Expression queryExpression;

	/**
	 * Whether to split the query result as individual messages.
	 */
	private boolean split = true;

	@NotEmpty(message = "Query is required")
	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public Expression getQueryExpression() {
		return queryExpression;
	}

	public void setQueryExpression(Expression queryExpression) {
		this.queryExpression = queryExpression;
	}

	public void setCollection(String collection) {
		this.collection = collection;
	}

	@NotBlank(message = "Collection name is required")
	public String getCollection() {
		return collection;
	}

	public boolean isSplit() {
		return split;
	}

	public void setSplit(boolean split) {
		this.split = split;
	}

}
