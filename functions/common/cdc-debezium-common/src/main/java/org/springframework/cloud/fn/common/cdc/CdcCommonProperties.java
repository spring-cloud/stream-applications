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

package org.springframework.cloud.fn.common.cdc;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 *
 * @author Christian Tzolov
 */
@ConfigurationProperties("cdc")
@Validated
public class CdcCommonProperties {

	/**
	 * Unique name for this sourceConnector instance.
	 */
	@NotEmpty
	private String name;

	/**
	 * Shortcut for the cdc.config.connector.class property. Either of those can be used as long as they do not
	 * contradict with each other.
	 */
	@NotNull
	private ConnectorType connector = null;

	private final Offset offset = new Offset();

	/**
	 * Include the schema's as part of the outbound message.
	 */
	private boolean schema = false;

	/**
	 * Event Flattening (https://debezium.io/docs/configuration/event-flattening).
	 */
	private final Flattening flattening = new Flattening();

	/**
	 * Spring pass-trough wrapper for debezium configuration properties.
	 * All properties with a 'cdc.config.' prefix are native Debezium properties.
	 * The prefix is removed, converting them into Debezium io.debezium.config.Configuration.
	 */
	private Map<String, String> config = defaultConfig();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Offset getOffset() {
		return offset;
	}

	public Flattening getFlattening() {
		return flattening;
	}

	public Map<String, String> getConfig() {
		return config;
	}

	public boolean isSchema() {
		return schema;
	}

	public void setSchema(boolean schema) {
		this.schema = schema;
	}

	private Map<String, String> defaultConfig() {
		Map<String, String> defaultConfig = new HashMap<>();
		defaultConfig.put("database.history", "io.debezium.relational.history.MemoryDatabaseHistory");
		//defaultConfig.put("offset.flush.interval.ms", "60000");
		return defaultConfig;
	}

	public ConnectorType getConnector() {
		return connector;
	}

	public void setConnector(ConnectorType connector) {
		this.connector = connector;
	}

	@AssertTrue
	public boolean connectorIsSet() {
		return this.getConnector() != null || this.getConfig().containsKey("connector.class");
	}

	public static class Offset {

		/**
		 * Interval at which to try committing offsets. The default is 1 minute.
		 */
		private Duration flushInterval = Duration.ofMillis(60000);

		/**
		 * Maximum number of milliseconds to wait for records to flush and partition offset data to be committed to
		 * offset storage before cancelling the process and restoring the offset data to be committed in a future attempt.
		 */
		private Duration commitTimeout = Duration.ofMillis(5000);

		/**
		 * Offset storage commit policy.
		 */
		private OffsetPolicy policy = OffsetPolicy.periodic;

		/**
		 * Kafka connector tracks the number processed records and regularly stores the count (as "offsets") in a
		 * preconfigured metadata storage. On restart the connector resumes the reading from the last recorded source offset.
		 */
		private OffsetStorageType storage = OffsetStorageType.metadata;

		public enum OffsetPolicy {
			/** periodic. */
			periodic("io.debezium.embedded.spi.OffsetCommitPolicy$PeriodicCommitOffsetPolicy"),
			/** always. */
			always("OffsetCommitPolicy.AlwaysCommitOffsetPolicy.class.getName()");

			/** policy. */
			public final String policyClass;

			OffsetPolicy(String policyClassName) {
				this.policyClass = policyClassName;
			}
		}

		public Duration getFlushInterval() {
			return flushInterval;
		}

		public void setFlushInterval(Duration flushInterval) {
			this.flushInterval = flushInterval;
		}

		public Duration getCommitTimeout() {
			return commitTimeout;
		}

		public void setCommitTimeout(Duration commitTimeout) {
			this.commitTimeout = commitTimeout;
		}

		public OffsetPolicy getPolicy() {
			return policy;
		}

		public void setPolicy(OffsetPolicy policy) {
			this.policy = policy;
		}

		public OffsetStorageType getStorage() {
			return storage;
		}

		public void setStorage(OffsetStorageType storage) {
			this.storage = storage;
		}

	}

	public enum OffsetStorageType {
		/** memory offset storage type. */
		memory("org.apache.kafka.connect.storage.MemoryOffsetBackingStore"),
		/** File offset storage type. */
		file("org.apache.kafka.connect.storage.FileOffsetBackingStore"),
		/** Kafka offset storage type. */
		kafka("org.apache.kafka.connect.storage.KafkaOffsetBackingStore"),
		/** Metadata offset storage type. */
		metadata("org.springframework.cloud.stream.app.cdc.common.core.store.MetadataStoreOffsetBackingStore");

		/** Class name of the Offset Storage. */
		public final String offsetStorageClass;

		OffsetStorageType(String type) {
			this.offsetStorageClass = type;
		}
	}

	public enum ConnectorType {
		/** MySql connector type. */
		mysql("io.debezium.connector.mysql.MySqlConnector"),
		/** Postgres connector type. */
		postgres("io.debezium.connector.postgresql.PostgresConnector"),
		/** Mongodb connector type. */
		mongodb("io.debezium.connector.mongodb.MongoDbConnector"),
		/** Oracle connector type. */
		oracle("io.debezium.connector.oracle.OracleConnector"),
		/** SqlServer connector type. */
		sqlserver("io.debezium.connector.sqlserver.SqlServerConnector");

		/** Connector class name. */
		public final String connectorClass;

		ConnectorType(String type) {
			this.connectorClass = type;
		}
	}

	public enum DeleteHandlingMode {
		/** records are removed. */
		drop,
		/** add a __deleted column with true/false values based on record operation. */
		rewrite,
		/** pass delete events. */
		none
	}

	/**
	 * https://debezium.io/documentation/reference/0.10/configuration/event-flattening.html .
	 * https://debezium.io/documentation/reference/0.10/configuration/event-flattening.html#configuration_options
	 */
	public static class Flattening {

		/**
		 * Enable flattening the source record events (https://debezium.io/docs/configuration/event-flattening).
		 */
		private boolean enabled = true;

		/**
		 * By default Debezium generates tombstone records to enable Kafka compaction on deleted records.
		 * The dropTombstones can suppress the tombstone records.
		 */
		private boolean dropTombstones = true;

		/**
		 * Options for handling deleted records: (1) none - pass the records through, (2) drop - remove the records and
		 * (3) rewrite - add a '__deleted' field to the records.
		 */
		private DeleteHandlingMode deleteHandlingMode = DeleteHandlingMode.drop;

		/**
		 * Comma separated list of metadata fields to add to the flattened message.
		 * The fields will be prefixed with "__" or "__[<]struct]__", depending on the specification of the struct.
		 */
		private String addFields = null;

		/**
		 * Comma separated list specify a list of metadata fields to add to the header of the flattened message.
		 * The fields will be prefixed with "__" or "__[struct]__".
		 */
		private String addHeaders = null;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isDropTombstones() {
			return dropTombstones;
		}

		public void setDropTombstones(boolean dropTombstones) {
			this.dropTombstones = dropTombstones;
		}

		public DeleteHandlingMode getDeleteHandlingMode() {
			return deleteHandlingMode;
		}

		public void setDeleteHandlingMode(DeleteHandlingMode deleteHandlingMode) {
			this.deleteHandlingMode = deleteHandlingMode;
		}

		public String getAddFields() {
			return addFields;
		}

		public void setAddFields(String addFields) {
			this.addFields = addFields;
		}

		public String getAddHeaders() {
			return addHeaders;
		}

		public void setAddHeaders(String addHeaders) {
			this.addHeaders = addHeaders;
		}
	}
}
