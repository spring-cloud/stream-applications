/*
 * Copyright 2016-2017 the original author or authors.
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

package org.springframework.cloud.stream.app.pgcopy.sink;

import java.util.Collections;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Used to configure the pgcopy sink module options that are related to writing using the PostgreSQL CopyManager API.
 *
 * @author Thomas Risberg
 */
@SuppressWarnings("unused")
@ConfigurationProperties("pgcopy")
@Validated
public class PgcopySinkProperties {

	/**
	 * The name of the table to write into.
	 */
	@NotNull
	private String tableName;

	/**
	 * The names of the columns that shall receive data.
	 * Also used at initialization time to issue the DDL.
	 */
	private List<String> columns = Collections.singletonList("payload");

	/**
	 * Threshold in number of messages when data will be flushed to database table.
	 */
	private int batchSize = 10000;

	/**
	 * Idle timeout in milliseconds when data is automatically flushed to database table.
	 */
	private long idleTimeout = -1L;

	/**
	 * 'true', 'false' or the location of a custom initialization script for the table.
	 */
	private String initialize = "false";

	/**
	 * Format to use for the copy command.
	 */
	private Format format = Format.TEXT;

	/**
	 * Specifies the string that represents a null value. The default is \N (backslash-N) in text format, and an
	 * unquoted empty string in CSV format.
	 */
	private String nullString;

	/**
	 * Specifies the character that separates columns within each row (line) of the file. The default is a tab character
	 * in text format, a comma in CSV format. This must be a single one-byte character. Using an escaped value like '\t'
	 * is allowed.
	 */
	private String delimiter;

	/**
	 * Specifies the quoting character to be used when a data value is quoted. The default is double-quote. This must
	 * be a single one-byte character. This option is allowed only when using CSV format.
	 */
	private Character quote;

	/**
	 * Specifies the character that should appear before a data character that matches the QUOTE value. The default is
	 * the same as the QUOTE value (so that the quoting character is doubled if it appears in the data). This must be
	 * a single one-byte character. This option is allowed only when using CSV format.
	 */
	private Character escape;

	/**
	 * The name of the error table used for writing rows causing errors. The error table should have three columns
	 * named "table_name", "error_message" and "payload" large enough to hold potential data values.
	 * You can use the following DDL to create this table:
	 *     'CREATE TABLE ERRORS (TABLE_NAME VARCHAR(255), ERROR_MESSAGE TEXT,PAYLOAD TEXT)'
	 */
	private String errorTable;


	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public List<String> getColumns() {
		return columns;
	}

	public void setColumns(List<String> columns) {
		this.columns = columns;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public long getIdleTimeout() {
		return idleTimeout;
	}

	public void setIdleTimeout(long idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	public String getInitialize() {
		return initialize;
	}

	public void setInitialize(String initialize) {
		this.initialize = initialize;
	}

	public Format getFormat() {
		return format;
	}

	public void setFormat(Format format) {
		this.format = format;
	}

	public String getNullString() {
		return nullString;
	}

	public void setNullString(String nullString) {
		this.nullString = nullString;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public Character getQuote() {
		return quote;
	}

	public void setQuote(Character quote) {
		this.quote = quote;
	}

	public Character getEscape() {
		return escape;
	}

	public void setEscape(Character escape) {
		this.escape = escape;
	}

	public String getErrorTable() {
		return errorTable;
	}

	public void setErrorTable(String errorTable) {
		this.errorTable = errorTable;
	}

	public enum Format {

		/**
		 * text.
		 */
		TEXT,
		/**
		 * csv.
		 */
		CSV

	}
}
