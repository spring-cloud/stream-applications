/*
 * Copyright 2020-2022 the original author or authors.
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

package org.springframework.cloud.fn.aggregator;

import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.geode.boot.autoconfigure.ClientCacheAutoConfiguration;
import org.springframework.geode.boot.autoconfigure.SslAutoConfiguration;

/**
 * An {@link EnvironmentPostProcessor} to add {@code spring.autoconfigure.exclude} property
 * since we can't use {@code application.properties} from the library perspective.
 *
 * @author Artem Bilan
 */
public class ExcludeStoresAutoConfigurationEnvironmentPostProcessor implements EnvironmentPostProcessor {

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		MutablePropertySources propertySources = environment.getPropertySources();
		Properties properties = new Properties();

		properties.setProperty("spring.autoconfigure.exclude",
				DataSourceAutoConfiguration.class.getName() + ", " +
						DataSourceTransactionManagerAutoConfiguration.class.getName() + ", " +
						MongoAutoConfiguration.class.getName() + ", " +
						MongoDataAutoConfiguration.class.getName() + ", " +
						MongoRepositoriesAutoConfiguration.class.getName() + ", " +
						ClientCacheAutoConfiguration.class.getName() + ", " +
						RedisAutoConfiguration.class.getName() + ", " +
						RedisRepositoriesAutoConfiguration.class.getName());

		String messageStoreType = environment.getProperty(AggregatorFunctionProperties.PREFIX + ".message-store-type");
		if (!AggregatorFunctionProperties.MessageStoreType.GEMFIRE.equals(messageStoreType)) {
			properties.setProperty(SslAutoConfiguration.SECURITY_SSL_ENVIRONMENT_POST_PROCESSOR_ENABLED_PROPERTY,
					"false");
		}

		propertySources.addLast(
				new PropertiesPropertySource("aggregator.exclude.stores.auto-configuration", properties));
	}

}
