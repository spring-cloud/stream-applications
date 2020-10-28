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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.kafka.connect.source.SourceRecord;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.metadata.SimpleMetadataStore;

/**
 * @author Christian Tzolov
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
public class TestCdcApplication {

	@Bean
	public Consumer<SourceRecord> mySourceRecordConsumer(Function<SourceRecord, byte[]> valueSerializer,
			Function<SourceRecord, byte[]> keySerializer) {
		return new TestSourceRecordConsumer(valueSerializer, keySerializer);
	}

	@Bean
	public SimpleMetadataStore simpleMetadataStore() {
		return new SimpleMetadataStore();
	}

	public static class TestSourceRecordConsumer implements Consumer<SourceRecord> {

		private final Function<SourceRecord, byte[]> valueSerializer;

		private final Function<SourceRecord, byte[]> keySerializer;

		public Map<Object, Object> keyValue = new HashMap<>();

		public List<SourceRecord> recordList = new CopyOnWriteArrayList<>();

		public TestSourceRecordConsumer(Function<SourceRecord, byte[]> valueSerializer,
				Function<SourceRecord, byte[]> keySerializer) {
			this.valueSerializer = valueSerializer;
			this.keySerializer = keySerializer;
		}

		@Override
		public void accept(SourceRecord sourceRecord) {
			if (sourceRecord != null) { // ignore null records
				recordList.add(sourceRecord);
				Object payload = valueSerializer.apply(sourceRecord);
				Object key = keySerializer.apply(sourceRecord);
				keyValue.put(key, payload);

				System.out.println("[CDC Event]: " + sourceRecord.toString());
			}
		}
	}
}
