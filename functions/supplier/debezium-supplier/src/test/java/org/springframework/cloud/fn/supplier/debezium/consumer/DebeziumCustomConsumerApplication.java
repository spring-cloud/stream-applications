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

package org.springframework.cloud.fn.supplier.debezium.consumer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import io.debezium.engine.ChangeEvent;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.fn.supplier.debezium.DebeziumConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 *
 * @author Christian Tzolov
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
@Import(DebeziumConfiguration.class)
public class DebeziumCustomConsumerApplication {

	@Bean
	@ConditionalOnProperty(name = "cdc.consumer.override", havingValue = "true")
	public Consumer<ChangeEvent<String, String>> mySourceRecordConsumer2() {
		return new TestDebeziumConsumer();
	}

	public static class TestDebeziumConsumer implements Consumer<ChangeEvent<String, String>> {

		public Map<Object, Object> keyValue = new HashMap<>();

		public List<ChangeEvent<String, String>> recordList = new CopyOnWriteArrayList<>();

		public TestDebeziumConsumer() {
		}

		@Override
		public void accept(ChangeEvent<String, String> changeEvent) {
			if (changeEvent != null) { // ignore null records
				recordList.add(changeEvent);
				keyValue.put(changeEvent.key(), changeEvent.value());
				System.out.println("SIZE=" + recordList.size());
				System.out.println("[CDC Event]: " + changeEvent.toString());
			}
		}
	}
}
