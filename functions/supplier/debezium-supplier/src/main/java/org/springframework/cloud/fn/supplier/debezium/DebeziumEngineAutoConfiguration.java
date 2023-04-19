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

import java.util.Properties;
import java.util.function.Consumer;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.spi.OffsetCommitPolicy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.supplier.debezium.DebeziumProperties.DebeziumEngineConfiguration.DebeziumOffsetCommitPolicy;
import org.springframework.context.annotation.Bean;

/**
 *
 * @author Christian Tzolov
 */
@AutoConfiguration
@EnableConfigurationProperties(DebeziumProperties.class)
public class DebeziumEngineAutoConfiguration {

	private static final Log logger = LogFactory.getLog(DebeziumConsumerConfiguration.class);

	@Bean
	public Properties debeziumConfiguration(DebeziumProperties properties) {
		Properties outProps = new java.util.Properties();
		outProps.putAll(properties.getDebezium());
		return outProps;
	}

	/**
	 * The fully-qualified class name of the commit policy type. The default is a periodic commit policy based upon time
	 * intervals.
	 * @param properties 'offset.commit.policy.*' configuration properties.
	 */
	@Bean
	@ConditionalOnMissingBean
	public OffsetCommitPolicy offsetCommitPolicy(DebeziumProperties properties) {

		final DebeziumOffsetCommitPolicy offsetCommitPolicy = properties.getEngine().getOffsetCommitPolicy();
		switch (offsetCommitPolicy) {
		case PERIODIC:
			Properties debeziumConfiguration = new java.util.Properties();
			debeziumConfiguration.putAll(properties.getDebezium());
			return OffsetCommitPolicy.periodic(debeziumConfiguration);
		case ALWAYS:
			return OffsetCommitPolicy.always();
		case DEFAULT:
		default:
			return null;
		}
	}

	@Bean
	public DebeziumEngine<?> debeziumEngine(Consumer<ChangeEvent<byte[], byte[]>> changeEventConsumer,
			OffsetCommitPolicy offsetCommitPolicy,
			DebeziumProperties properties) {

		Properties debeziumConfiguration = new java.util.Properties();
		debeziumConfiguration.putAll(properties.getDebezium());

		DebeziumEngine<ChangeEvent<byte[], byte[]>> debeziumEngine = DebeziumEngine
				.create(properties.getFormat().serializationFormat())
				.using(debeziumConfiguration)
				.using(offsetCommitPolicy)
				.notifying(changeEventConsumer)
				.build();

		return debeziumEngine;
	}

	@Bean
	public EmbeddedEngineExecutorService embeddedEngine(DebeziumEngine<?> debeziumEngine) {
		return new EmbeddedEngineExecutorService(debeziumEngine);
	}
}
