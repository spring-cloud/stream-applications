/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.stream.app.metadata;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.integration.aws.metadata.DynamoDbMetadataStore;
import org.springframework.integration.gemfire.metadata.GemfireMetadataStore;
import org.springframework.integration.jdbc.metadata.JdbcMetadataStore;
import org.springframework.integration.redis.metadata.RedisMetadataStore;

/**
 * @author Artem Bilan
 *
 * @since 2.0.2
 */
@ConfigurationProperties("metadata.store")
public class MetadataStoreProperties {

	private final Mongo mongoDb = new Mongo();

	private final Gemfire gemfire = new Gemfire();

	private final Redis redis = new Redis();

	private final DynamoDb dynamoDb = new DynamoDb();

	private final Jdbc jdbc = new Jdbc();

	private final Zookeeper zookeeper = new Zookeeper();

	public Mongo getMongoDb() {
		return this.mongoDb;
	}

	public Gemfire getGemfire() {
		return this.gemfire;
	}

	public Redis getRedis() {
		return this.redis;
	}

	public DynamoDb getDynamoDb() {
		return this.dynamoDb;
	}

	public Jdbc getJdbc() {
		return this.jdbc;
	}

	public Zookeeper getZookeeper() {
		return this.zookeeper;
	}

	public static class Mongo {

		/**
		 * MongoDB collection name for metadata.
		 */
		private String collection = "metadataStore";

		public String getCollection() {
			return this.collection;
		}

		public void setCollection(String collection) {
			this.collection = collection;
		}

	}

	public static class Gemfire {

		/**
		 * Gemfire region name for metadata.
		 */
		private String region = GemfireMetadataStore.KEY;

		public String getRegion() {
			return this.region;
		}

		public void setRegion(String region) {
			this.region = region;
		}

	}

	public static class Redis {

		/**
		 * Redis key for metadata.
		 */
		private String key = RedisMetadataStore.KEY;

		public String getKey() {
			return this.key;
		}

		public void setKey(String key) {
			this.key = key;
		}

	}

	public static class DynamoDb {

		/**
		 * Table name for metadata.
		 */
		private String table = DynamoDbMetadataStore.DEFAULT_TABLE_NAME;

		/**
		 * Read capacity on the table.
		 */
		private long readCapacity = 1L;

		/**
		 * Write capacity on the table.
		 */
		private long writeCapacity = 1L;

		/**
		 * Delay between create table retries.
		 */
		private int createDelay = 1;

		/**
		 * Retry number for create table request.
		 */
		private int createRetries = 25;

		/**
		 * TTL for table entries.
		 */
		private Integer timeToLive;

		public String getTable() {
			return this.table;
		}

		public void setTable(String table) {
			this.table = table;
		}

		public long getReadCapacity() {
			return this.readCapacity;
		}

		public void setReadCapacity(long readCapacity) {
			this.readCapacity = readCapacity;
		}

		public long getWriteCapacity() {
			return this.writeCapacity;
		}

		public void setWriteCapacity(long writeCapacity) {
			this.writeCapacity = writeCapacity;
		}

		public int getCreateDelay() {
			return this.createDelay;
		}

		public void setCreateDelay(int createDelay) {
			this.createDelay = createDelay;
		}

		public int getCreateRetries() {
			return this.createRetries;
		}

		public void setCreateRetries(int createRetries) {
			this.createRetries = createRetries;
		}

		public Integer getTimeToLive() {
			return this.timeToLive;
		}

		public void setTimeToLive(Integer timeToLive) {
			this.timeToLive = timeToLive;
		}

	}

	public static class Jdbc {

		/**
		 * Prefix for the custom table name.
		 */
		private String tablePrefix = JdbcMetadataStore.DEFAULT_TABLE_PREFIX;

		/**
		 * Unique grouping identifier for messages persisted with this store.
		 */
		private String region = "DEFAULT";

		public String getTablePrefix() {
			return this.tablePrefix;
		}

		public void setTablePrefix(String tablePrefix) {
			this.tablePrefix = tablePrefix;
		}

		public String getRegion() {
			return this.region;
		}

		public void setRegion(String region) {
			this.region = region;
		}

	}

	public static class Zookeeper {

		/**
		 * Zookeeper connect string in form HOST:PORT.
		 */
		private String connectString = "127.0.0.1:2181";

		/**
		 * Retry interval for Zookeeper operations in milliseconds.
		 */
		private int retryInterval = 1000;

		/**
		 * Encoding to use when storing data in Zookeeper.
		 */
		private Charset encoding = StandardCharsets.UTF_8;

		/**
		 * Root node - store entries are children of this node.
		 */
		private String root = "/SpringIntegration-MetadataStore";

		public String getConnectString() {
			return connectString;
		}

		public void setConnectString(String connectString) {
			this.connectString = connectString;
		}

		public int getRetryInterval() {
			return this.retryInterval;
		}

		public void setRetryInterval(int retryInterval) {
			this.retryInterval = retryInterval;
		}

		public Charset getEncoding() {
			return this.encoding;
		}

		public void setEncoding(Charset encoding) {
			this.encoding = encoding;
		}

		public String getRoot() {
			return this.root;
		}

		public void setRoot(String root) {
			this.root = root;
		}

	}

}
