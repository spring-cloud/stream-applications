/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.stream.app.pgcopy.test;

import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.test.junit.AbstractExternalResourceTestSupport;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceUtils;

/**
 * JUnit {@link org.junit.Rule} that detects the fact that a PostgreSQL server is running on localhost.
 *
 * @author Thomas Risberg
 * @author Artem Bilan
 */
public class PostgresTestSupport extends AbstractExternalResourceTestSupport<DataSource> {

	private ConfigurableApplicationContext context;

	public PostgresTestSupport() {
		super("POSTGRES");
	}

	@Override
	protected void cleanupResource() {
		context.close();
	}

	@Override
	protected void obtainResource() {
		context = new SpringApplicationBuilder(Config.class)
				.web(WebApplicationType.NONE)
				.run();
		DataSource dataSource = context.getBean(DataSource.class);
		Connection con = DataSourceUtils.getConnection(dataSource);
		DataSourceUtils.releaseConnection(con, dataSource);
	}

	@Configuration
	@EnableAutoConfiguration
	public static class Config {

	}

}
