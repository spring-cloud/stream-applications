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

import org.apache.kafka.connect.storage.FileOffsetBackingStore;
import org.apache.kafka.connect.storage.KafkaOffsetBackingStore;
import org.apache.kafka.connect.storage.MemoryOffsetBackingStore;
import org.apache.kafka.connect.storage.OffsetBackingStore;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.fn.common.cdc.store.MetadataStoreOffsetBackingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.metadata.MetadataStore;

/**
 * @author Christian Tzolov
 */
@Configuration
public class CdcOffsetBackingStoreConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnExpression("'${cdc.config.offset.storage}'.equalsIgnoreCase('org.springframework.cloud.stream.app.cdc.common.core.store.MetadataStoreOffsetBackingStore') " +
			"or '${cdc.offset.storage:metadata}'.equals('metadata')")
	public OffsetBackingStore metadataStoreOffsetBackingStore(MetadataStore metadataStore) {
		return new MetadataStoreOffsetBackingStore(metadataStore);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnExpression("'${cdc.config.offset.storage}'.equalsIgnoreCase('org.apache.kafka.connect.storage.FileOffsetBackingStore') " +
			"or '${cdc.offset.storage:metadata}'.equalsIgnoreCase('file')")
	public OffsetBackingStore fileOffsetBackingStore() {
		return new FileOffsetBackingStore();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnExpression("'${cdc.config.offset.storage}'.equalsIgnoreCase('org.apache.kafka.connect.storage.KafkaOffsetBackingStore') " +
			"or '${cdc.offset.storage:metadata}'.equalsIgnoreCase('kafka')")
	public OffsetBackingStore kafkaOffsetBackingStore() {
		return new KafkaOffsetBackingStore();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnExpression("'${cdc.config.offset.storage}'.equalsIgnoreCase('org.apache.kafka.connect.storage.MemoryOffsetBackingStore') " +
			"or '${cdc.offset.storage:metadata}'.equalsIgnoreCase('memory')")
	public OffsetBackingStore memoryOffsetBackingStore() {
		return new MemoryOffsetBackingStore();
	}
}
