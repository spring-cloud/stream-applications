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

package org.springframework.cloud.fn.common.debezium;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.DebeziumEngine.Builder;
import io.debezium.engine.DebeziumEngine.CompletionCallback;
import io.debezium.engine.DebeziumEngine.ConnectorCallback;
import io.debezium.engine.format.KeyValueHeaderChangeEventFormat;
import io.debezium.engine.format.SerializationFormat;
import io.debezium.engine.spi.OffsetCommitPolicy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.debezium.DebeziumProperties.DebeziumFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link DebeziumEngine.Builder}.
 * <p>
 * The builder provides a standalone engine configuration that talks with the source data system.
 * <p>
 * The application that runs the debezium engine assumes all responsibility for fault tolerance, scalability, and
 * durability. Additionally, applications must specify how the engine can store its relational database schema history
 * and offsets. By default, this information will be stored in memory and will thus be lost upon application restart.
 * <p>
 * The {@link DebeziumEngine.Builder} auto-configuration is activated only if a Debezium Connector is available on the
 * classpath and the <code>debezium.properties.connector.class</code> property is set.
 * <p>
 * Properties prefixed with <code>debezium.properties</code> are passed through as native Debezium properties.
 *
 * @author Christian Tzolov
 * @author Corneil du Plessis
 */
@AutoConfiguration
@EnableConfigurationProperties(DebeziumProperties.class)
@Conditional(DebeziumEngineBuilderAutoConfiguration.OnDebeziumConnectorCondition.class)
@ConditionalOnProperty(prefix = "debezium", name = "properties.connector.class")
public class DebeziumEngineBuilderAutoConfiguration {

	private static final Log logger = LogFactory.getLog(DebeziumEngineBuilderAutoConfiguration.class);

	/**
	 * The fully-qualified class name of the commit policy type. The default is a periodic commit policy based upon time
	 * intervals.
	 * @param properties The 'debezium.properties.offset.flush.interval.ms' configuration is compulsory for the Periodic
	 * policy type. The ALWAYS and DEFAULT doesn't require additional configuration.
	 */
	@Bean
	@ConditionalOnMissingBean
	public OffsetCommitPolicy offsetCommitPolicy(DebeziumProperties properties) {

		switch (properties.getOffsetCommitPolicy()) {
		case PERIODIC:
			return OffsetCommitPolicy.periodic(properties.getDebeziumNativeConfiguration());
		case ALWAYS:
			return OffsetCommitPolicy.always();
		case DEFAULT:
		default:
			return NULL_OFFSET_COMMIT_POLICY;
		}
	}

	/**
	 * Use the specified clock when needing to determine the current time. Defaults to {@link Clock#systemDefaultZone()
	 * system clock}, but you can override the Bean in your configuration with you {@link Clock implementation}. Returns
	 * @return Clock for the system default zone.
	 */
	@Bean
	@ConditionalOnMissingBean
	public Clock debeziumClock() {
		return Clock.systemDefaultZone();
	}

	/**
	 * When the engine's {@link DebeziumEngine#run()} method completes, call the supplied function with the results.
	 * @return Default completion callback that logs the completion status. The bean can be overridden in custom
	 * implementation.
	 */
	@Bean
	@ConditionalOnMissingBean
	public CompletionCallback completionCallback() {
		return DEFAULT_COMPLETION_CALLBACK;
	}

	/**
	 * During the engine run, provides feedback about the different stages according to the completion state of each
	 * component running within the engine (connectors, tasks etc). The bean can be overridden in custom implementation.
	 */
	@Bean
	@ConditionalOnMissingBean
	public ConnectorCallback connectorCallback() {
		return DEFAULT_CONNECTOR_CALLBACK;
	}

	@Bean
	public Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder(
			OffsetCommitPolicy offsetCommitPolicy, CompletionCallback completionCallback,
			ConnectorCallback connectorCallback, DebeziumProperties properties, Clock debeziumClock) {

		Class<? extends SerializationFormat<byte[]>> payloadFormat = Objects.requireNonNull(
				serializationFormatClass(properties.getPayloadFormat()),
				"Cannot find payload format for " + properties.getProperties());

		Class<? extends SerializationFormat<byte[]>> headerFormat = Objects.requireNonNull(
				serializationFormatClass(properties.getHeaderFormat()),
				"Cannot find header format for " + properties.getProperties());

		return DebeziumEngine
				.create(KeyValueHeaderChangeEventFormat.of(payloadFormat, payloadFormat, headerFormat))
				.using(properties.getDebeziumNativeConfiguration())
				.using(debeziumClock)
				.using(completionCallback)
				.using(connectorCallback)
				.using((offsetCommitPolicy != NULL_OFFSET_COMMIT_POLICY) ? offsetCommitPolicy : null);
	}

	/**
	 * Converts the {@link DebeziumFormat} enum into Debezium {@link SerializationFormat} class.
	 * @param debeziumFormat debezium format property.
	 */
	private Class<? extends SerializationFormat<byte[]>> serializationFormatClass(DebeziumFormat debeziumFormat) {
		switch (debeziumFormat) {
		case JSON:
			return io.debezium.engine.format.JsonByteArray.class;
		case AVRO:
			return io.debezium.engine.format.Avro.class;
		case PROTOBUF:
			return io.debezium.engine.format.Protobuf.class;
		default:
			throw new IllegalArgumentException("Unknown debezium format: " + debeziumFormat);
		}
	}

	/**
	 * A callback function to be notified when the connector completes.
	 */
	private static final CompletionCallback DEFAULT_COMPLETION_CALLBACK = new CompletionCallback() {
		@Override
		public void handle(boolean success, String message, Throwable error) {
			logger.info(String.format("Debezium Engine completed with success:%s, message:%s ", success, message),
					error);
		}
	};

	/**
	 * Callback function which informs users about the various stages a connector goes through during startup.
	 */
	private static final ConnectorCallback DEFAULT_CONNECTOR_CALLBACK = new ConnectorCallback() {

		/**
		 * Called after a connector has been successfully started by the engine.
		 */
		public void connectorStarted() {
			logger.info("Connector Started!");
		};

		/**
		 * Called after a connector has been successfully stopped by the engine.
		 */
		public void connectorStopped() {
			logger.info("Connector Stopped!");
		}

		/**
		 * Called after a connector task has been successfully started by the engine.
		 */
		public void taskStarted() {
			logger.info("Connector Task Started!");
		}

		/**
		 * Called after a connector task has been successfully stopped by the engine.
		 */
		public void taskStopped() {
			logger.info("Connector Task Stopped!");
		}

	};

	/**
	 * The policy that defines when the offsets should be committed to offset storage.
	 */
	private static final OffsetCommitPolicy NULL_OFFSET_COMMIT_POLICY = new OffsetCommitPolicy() {
		@Override
		public boolean performCommit(long numberOfMessagesSinceLastCommit, Duration timeSinceLastCommit) {
			throw new UnsupportedOperationException("Unimplemented method 'performCommit'");
		}
	};

	/**
	 * Determine if Debezium connector is available. This either kicks in if any debezium connector is available.
	 */
	@Order(Ordered.LOWEST_PRECEDENCE)
	static class OnDebeziumConnectorCondition extends AnyNestedCondition {

		OnDebeziumConnectorCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnClass(name = { "io.debezium.connector.mysql.MySqlConnector" })
		static class HasMySqlConnector {

		}

		@ConditionalOnClass(name = "io.debezium.connector.postgresql.PostgresConnector")
		static class HasPostgreSqlConnector {

		}

		@ConditionalOnClass(name = "io.debezium.connector.db2.Db2Connector")
		static class HasDb2Connector {

		}

		@ConditionalOnClass(name = "io.debezium.connector.oracle.OracleConnector")
		static class HasOracleConnector {

		}

		@ConditionalOnClass(name = "io.debezium.connector.sqlserver.SqlServerConnector")
		static class HasSqlServerConnector {

		}

		@ConditionalOnClass(name = "io.debezium.connector.mongodb.MongoDbConnector")
		static class HasMongoDbConnector {

		}

		@ConditionalOnClass(name = "io.debezium.connector.vitess.VitessConnector")
		static class HasVitessConnector {

		}

		@ConditionalOnClass(name = "io.debezium.connector.spanner.SpannerConnector")
		static class HasSpannerConnector {

		}

	}

}
