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

package org.springframework.cloud.fn.supplier.debezium;

import java.time.Clock;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.DebeziumEngine.CompletionCallback;
import io.debezium.engine.DebeziumEngine.ConnectorCallback;
import io.debezium.engine.spi.OffsetCommitPolicy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * DebeziumEngine auto-configuration.
 *
 * The engine configuration is entirely standalone and only talks with the source system; Applications using the engine
 * auto-configuration simply provides a {@link Consumer consumer function} implementation to which the engine will pass
 * all records containing database change events.
 * <p>
 * With the engine, the application that runs the connector assumes all responsibility for fault tolerance, scalability,
 * and durability. Additionally, applications must specify how the engine can store its relational database schema
 * history and offsets. By default, this information will be stored in memory and will thus be lost upon application
 * restart.
 * <p>
 * Engine Is designed to be submitted to an {@link Executor} or {@link ExecutorService} for execution by a single
 * thread, and a running connector can be stopped either by calling {@link #stop()} from another thread or by
 * interrupting the running thread (e.g., as is the case with {@link ExecutorService#shutdownNow()}).
 *
 * @author Christian Tzolov
 */
@AutoConfiguration
@EnableConfigurationProperties(DebeziumProperties.class)
public class DebeziumEngineAutoConfiguration {

	private static final Log logger = LogFactory.getLog(DebeziumEngineAutoConfiguration.class);

	@Bean
	public Properties debeziumConfiguration(DebeziumProperties properties) {
		Properties outProps = new java.util.Properties();
		outProps.putAll(properties.getInner());
		return outProps;
	}

	/**
	 * The fully-qualified class name of the commit policy type. The default is a periodic commit policy based upon time
	 * intervals.
	 * @param properties The 'debezium.inner.offset.flush.interval.ms' configuration is compulsory for the Periodic policy
	 * type. The ALWAYS and DEFAULT doesn't require properties.
	 */
	@Bean
	@ConditionalOnMissingBean
	public OffsetCommitPolicy offsetCommitPolicy(DebeziumProperties properties, Properties debeziumConfiguration) {

		switch (properties.getOffsetCommitPolicy()) {
		case PERIODIC:
			return OffsetCommitPolicy.periodic(debeziumConfiguration);
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
	public DebeziumEngine<?> debeziumEngine(Consumer<ChangeEvent<byte[], byte[]>> changeEventConsumer,
			OffsetCommitPolicy offsetCommitPolicy, CompletionCallback completionCallback,
			ConnectorCallback connectorCallback, DebeziumProperties properties, Properties debeziumConfiguration,
			Clock debeziumClock) {

		DebeziumEngine<ChangeEvent<byte[], byte[]>> debeziumEngine = DebeziumEngine
				.create(properties.getFormat().serializationFormat())
				.using(debeziumConfiguration)
				.using(debeziumClock)
				.using(completionCallback)
				.using(connectorCallback)
				.using((offsetCommitPolicy != NULL_OFFSET_COMMIT_POLICY) ? offsetCommitPolicy : null)
				.notifying(changeEventConsumer)
				.build();

		logger.debug("Debezium Engine created!");

		return debeziumEngine;
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
}
