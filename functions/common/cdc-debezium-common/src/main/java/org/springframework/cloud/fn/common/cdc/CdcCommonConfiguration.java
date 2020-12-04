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

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import io.debezium.transforms.ExtractNewRecordState;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.storage.Converter;
import org.apache.kafka.connect.storage.OffsetBackingStore;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;

/**
 * @author Christian Tzolov
 */
@Configuration
@EnableConfigurationProperties(CdcCommonProperties.class)
@Import(CdcOffsetBackingStoreConfiguration.class)
public class CdcCommonConfiguration {

	private static final Log logger = LogFactory.getLog(CdcCommonConfiguration.class);

	@Bean
	public io.debezium.config.Configuration configuration(CdcCommonProperties properties) {
		Map<String, String> configMap = properties.getConfig();
		return io.debezium.config.Configuration.from(configMap);
	}

	@Bean
	public Function<SourceRecord, SourceRecord> recordFlattening(CdcCommonProperties properties,
			ExtractNewRecordState extractNewRecordState) {
		return sourceRecord -> properties.getFlattening().isEnabled() ?
				(SourceRecord) extractNewRecordState.apply(sourceRecord) : sourceRecord;
	}

	@Bean
	public ExtractNewRecordState extractNewRecordState(CdcCommonProperties properties) {
		ExtractNewRecordState extractNewRecordState = new ExtractNewRecordState();
		Map<String, Object> config = extractNewRecordState.config().defaultValues();
		config.put("drop.tombstones", properties.getFlattening().isDropTombstones());
		config.put("delete.handling.mode", properties.getFlattening().getDeleteHandlingMode().name());
		if (!StringUtils.isEmpty(properties.getFlattening().getAddHeaders())) {
			config.put("add.headers", properties.getFlattening().getAddHeaders());
		}
		if (!StringUtils.isEmpty(properties.getFlattening().getAddFields())) {
			config.put("add.fields", properties.getFlattening().getAddFields());
		}

		extractNewRecordState.configure(config);
		return extractNewRecordState;
	}

	@Bean
	@ConditionalOnMissingBean
	public JsonConverter jsonConverter(CdcCommonProperties properties) {
		JsonConverter jsonConverter = new JsonConverter();
		jsonConverter.configure(Collections.singletonMap("schemas.enable", properties.isSchema()), false);
		return jsonConverter;
	}

	@Bean
	public Function<SourceRecord, byte[]> valueSerializer(Converter valueConverter) {
		return sourceRecord -> valueConverter.fromConnectData(
				sourceRecord.topic(), sourceRecord.valueSchema(), sourceRecord.value());
	}

	@Bean
	public Function<SourceRecord, byte[]> keySerializer(Converter valueConverter) {
		return sourceRecord -> valueConverter.fromConnectData(
				sourceRecord.topic(), sourceRecord.keySchema(), sourceRecord.key());
	}

	@Bean
	public EmbeddedEngine.Builder embeddedEngineBuilder(CdcCommonProperties properties,
			OffsetBackingStore offsetBackingStore) {

		if (!properties.getConfig().containsKey("connector.class")) {
			properties.getConfig().put("connector.class", properties.getConnector().connectorClass);
		}

		if (!properties.getConfig().containsKey("name")) {
			properties.getConfig().put("name", properties.getName());
		}

		if (!properties.getConfig().containsKey("offset.flush.interval.ms")) {
			properties.getConfig().put("offset.flush.interval.ms", properties.getOffset().getFlushInterval().toMillis() + "");
		}

		if (!properties.getConfig().containsKey("offset.flush.timeout.ms")) {
			properties.getConfig().put("offset.flush.timeout.ms", properties.getOffset().getCommitTimeout().toMillis() + "");
		}

		if (!properties.getConfig().containsKey("offset.commit.policy")) {
			properties.getConfig().put("offset.commit.policy", properties.getOffset().getPolicy().policyClass);
		}

		if (!properties.getConfig().containsKey("offset.storage")) {
			properties.getConfig().put("offset.storage", properties.getOffset().getStorage().offsetStorageClass);
		}

		return EmbeddedEngine.create()
				.using(io.debezium.config.Configuration.from(properties.getConfig()))
				.offsetBackingStore(offsetBackingStore);
	}

}
