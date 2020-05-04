/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.cloud.stream.app.jdbc.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.supplier.jdbc.JdbcSupplierConfiguration;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;

/**
 * @author Soby Chacko
 * @author Artem Bilan
 */
@SpringBootTest(properties = "spring.cloud.stream.function.definition=jdbcSupplier")
@DirtiesContext
public class JdbcSourceIntegrationTests {

	@Autowired
	protected ObjectMapper objectMapper;

	@Qualifier("jdbcSupplier-out-0")
	@Autowired
	protected MessageChannel output;

	@Autowired
	protected JdbcOperations jdbcOperations;

	@Autowired
	protected MessageCollector messageCollector;

	@SpringBootApplication
	@Import(JdbcSupplierConfiguration.class)
	public static class JdbcSourceConfiguration { }

}
