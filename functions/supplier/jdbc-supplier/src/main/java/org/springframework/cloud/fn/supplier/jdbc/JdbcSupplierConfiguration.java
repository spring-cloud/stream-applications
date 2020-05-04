/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.cloud.fn.supplier.jdbc;

import io.pivotal.java.function.splitter.function.SplitterFunctionConfiguration;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.function.context.PollableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.jdbc.JdbcPollingChannelAdapter;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

/**
 * @author Soby Chacko
 * @author Artem Bilan
 */
@Configuration
@EnableConfigurationProperties(JdbcSupplierProperties.class)
@Import(SplitterFunctionConfiguration.class)
public class JdbcSupplierConfiguration {

	private final JdbcSupplierProperties properties;

	private final DataSource dataSource;

	public JdbcSupplierConfiguration(JdbcSupplierProperties properties, DataSource dataSource) {
		this.properties = properties;
		this.dataSource = dataSource;
	}

	@Bean
	public MessageSource<Object> jdbcMessageSource() {
		JdbcPollingChannelAdapter jdbcPollingChannelAdapter =
				new JdbcPollingChannelAdapter(this.dataSource, this.properties.getQuery());
		jdbcPollingChannelAdapter.setMaxRows(this.properties.getMaxRows());
		jdbcPollingChannelAdapter.setUpdateSql(this.properties.getUpdate());
		return jdbcPollingChannelAdapter;
	}

	@Bean(name = "jdbcSupplier")
	@PollableBean(splittable = true)
	@ConditionalOnProperty(prefix = "jdbc.supplier", name = "split", matchIfMissing = true)
	public Supplier<Flux<Message<?>>> splittedSupplier(Function<Message<?>, List<Message<?>>> splitterFunction) {
		return () -> {
			Message<?> received = jdbcMessageSource().receive();
			if (received != null) {
				return Flux.fromIterable(splitterFunction.apply(received)); // multiple Message<Map<String, Object>>
			}
			else {
				return Flux.empty();
			}
		};
	}

	@Bean
	@ConditionalOnProperty(prefix = "jdbc.supplier", name = "split", havingValue = "false")
	public Supplier<Message<?>> jdbcSupplier() {
		return () -> jdbcMessageSource().receive();
	}

}
