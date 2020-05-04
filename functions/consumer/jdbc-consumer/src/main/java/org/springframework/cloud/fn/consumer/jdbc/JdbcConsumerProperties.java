/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.cloud.fn.consumer.jdbc;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Eric Bottard
 * @author Artem Bilan
 * @author Oliver Flasch
 */
@ConfigurationProperties("jdbc.consumer")
public class JdbcConsumerProperties {

	@Autowired
	private ShorthandMapConverter shorthandMapConverter;

	/**
	 * The name of the table to write into.
	 */
	private String tableName = "messages";

	/**
	 * The comma separated colon-based pairs of column names and SpEL expressions for values to insert/update.
	 * Names are used at initialization time to issue the DDL.
	 */
	private String columns = "payload:payload.toString()";

	/**
	 * 'true', 'false' or the location of a custom initialization script for the table.
	 */
	private String initialize = "false";

	/**
	 * Threshold in number of messages when data will be flushed to database table.
	 */
	private int batchSize = 1;

	/**
	 * Idle timeout in milliseconds when data is automatically flushed to database table.
	 */
	private long idleTimeout = -1L;

	private Map<String, String> columnsMap;

	public String getTableName() {
		return this.tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getColumns() {
		return this.columns;
	}

	public void setColumns(String columns) {
		this.columns = columns;
	}

	public String getInitialize() {
		return this.initialize;
	}

	public void setInitialize(String initialize) {
		this.initialize = initialize;
	}

	public int getBatchSize() {
		return this.batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public long getIdleTimeout() {
		return this.idleTimeout;
	}

	public void setIdleTimeout(long idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	Map<String, String> getColumnsMap() {
		if (this.columnsMap == null) {
			this.columnsMap = this.shorthandMapConverter.convert(this.columns);
		}
		return this.columnsMap;
	}
}
