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

package org.springframework.cloud.stream.app.source.cdc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.messaging.Message;
import org.springframework.util.StreamUtils;

/**
 * @author Christian Tzolov
 */
public final class CdcTestUtils {

	/**
	 * derived from the spring.cloud.stream.function.definition value
	 */
	public static final String CDC_SUPPLIER_OUT_0 = "cdcSupplier-out-0";

	private CdcTestUtils() {

	}

	public static JdbcTemplate jdbcTemplate(String jdbcDriver, String jdbcUrl, String user, String password) {

		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(jdbcDriver);
		dataSource.setUrl(jdbcUrl);
		dataSource.setUsername(user);
		dataSource.setPassword(password);

		return new JdbcTemplate(dataSource);
	}

	public static List<Message<?>> receiveAll(OutputDestination outputDestination) {
		return receiveAll(outputDestination, CDC_SUPPLIER_OUT_0);
	}

	public static List<Message<?>> receiveAll(OutputDestination outputDestination, String bindingName) {
		List<Message<?>> list = new ArrayList<>();
		Message<?> received;
		do {
			received = outputDestination.receive(Duration.ofSeconds(30).toMillis(), bindingName);
			if (received != null) {
				list.add(received);
			}
		} while (received != null);
		return list;
	}

	public static String resourceToString(String resourcePath) throws IOException {
		return StreamUtils.copyToString(
				new DefaultResourceLoader().getResource(resourcePath).getInputStream(), StandardCharsets.UTF_8);
	}
}
