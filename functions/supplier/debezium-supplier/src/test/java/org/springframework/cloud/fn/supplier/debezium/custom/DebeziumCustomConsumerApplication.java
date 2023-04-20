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

package org.springframework.cloud.fn.supplier.debezium.custom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 *
 * @author Christian Tzolov
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
public class DebeziumCustomConsumerApplication {

	@Bean
	public JdbcTemplate myJdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean
	@Primary
	@ConfigurationProperties("app.datasource")
	public DataSourceProperties dataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean
	public HikariDataSource dataSource(DataSourceProperties dataSourceProperties) {
		return dataSourceProperties.initializeDataSourceBuilder()
				.type(HikariDataSource.class)
				.driverClassName("com.mysql.cj.jdbc.Driver")
				.build();
	}

	@Bean
	public EmbeddedEngineExecutorService embeddedEngine(DebeziumEngine<?> debeziumEngine) {
		return new EmbeddedEngineExecutorService(debeziumEngine);
	}

	@Bean
	public Consumer<ChangeEvent<byte[], byte[]>> customConsumer() {
		return new TestDebeziumConsumer();
	}

	public static class TestDebeziumConsumer implements Consumer<ChangeEvent<byte[], byte[]>> {

		public Map<Object, Object> keyValue = new HashMap<>();

		public List<ChangeEvent<byte[], byte[]>> recordList = new CopyOnWriteArrayList<>();

		public TestDebeziumConsumer() {
		}

		@Override
		public void accept(ChangeEvent<byte[], byte[]> changeEvent) {
			if (changeEvent != null) { // ignore null records
				recordList.add(changeEvent);
				keyValue.put(changeEvent.key(), changeEvent.value());
				System.out.println("SIZE=" + recordList.size());
				System.out.println("[Debezium Event]: " + changeEvent.toString());
			}
		}
	}
}
