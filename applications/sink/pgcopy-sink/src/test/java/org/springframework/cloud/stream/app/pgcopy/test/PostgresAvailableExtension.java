/*
 * Copyright 2022-2022 the original author or authors.
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

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.log.LogAccessor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceUtils;

public class PostgresAvailableExtension implements ExecutionCondition {

	private final LogAccessor logger = new LogAccessor(this.getClass());

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {

		try (ConfigurableApplicationContext applicationContext = new SpringApplicationBuilder(PostgresAvailableExtension.Config.class)
				.properties("spring.integration.jdbc.initialize-schema=never",
						"spring.integration.jdbc.platform=postgres")
				.web(WebApplicationType.NONE)
				.run()) {
			DataSource dataSource = applicationContext.getBean(DataSource.class);
			Connection con = DataSourceUtils.getConnection(dataSource);
			DataSourceUtils.releaseConnection(con, dataSource);
		}
		catch (DataAccessException ex) {
			logger.warn(ex, () -> "Postgres not available - " + ex.getMessage());
			return ConditionEvaluationResult.disabled("Postgres not available");
		}
		return ConditionEvaluationResult.enabled("Postgres available");
	}

	@Configuration
	@EnableAutoConfiguration
	public static class Config {

	}
}
