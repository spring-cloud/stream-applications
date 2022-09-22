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

package org.springframework.cloud.fn.aggregator;

import java.util.Arrays;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.EnablePdx;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.geode.boot.autoconfigure.ClientCacheAutoConfiguration;
import org.springframework.integration.gemfire.store.GemfireMessageStore;
import org.springframework.integration.jdbc.store.JdbcMessageStore;
import org.springframework.integration.mongodb.store.ConfigurableMongoDbMessageStore;
import org.springframework.integration.mongodb.support.BinaryToMessageConverter;
import org.springframework.integration.mongodb.support.MessageToBinaryConverter;
import org.springframework.integration.redis.store.RedisMessageStore;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;


/**
 * A helper class containing configuration classes for particular technologies
 * to expose an appropriate {@link org.springframework.integration.store.MessageStore} bean
 * via matched configuration properties.
 *
 * @author Artem Bilan
 */
class MessageStoreConfiguration {

	@ConditionalOnClass(ConfigurableMongoDbMessageStore.class)
	@ConditionalOnProperty(prefix = AggregatorFunctionProperties.PREFIX,
			name = "message-store-type",
			havingValue = AggregatorFunctionProperties.MessageStoreType.MONGODB)
	@Import({ MongoAutoConfiguration.class,
			MongoDataAutoConfiguration.class })
	static class Mongo {

		@Bean
		public MessageGroupStore messageStore(MongoTemplate mongoTemplate, AggregatorFunctionProperties properties) {
			if (StringUtils.hasText(properties.getMessageStoreEntity())) {
				return new ConfigurableMongoDbMessageStore(mongoTemplate, properties.getMessageStoreEntity());
			}
			else {
				return new ConfigurableMongoDbMessageStore(mongoTemplate);
			}
		}

		@Bean
		@Primary
		public MongoCustomConversions mongoDbCustomConversions() {
			return new MongoCustomConversions(Arrays.asList(
					new MessageToBinaryConverter(), new BinaryToMessageConverter()));
		}

	}

	@ConditionalOnClass(RedisMessageStore.class)
	@ConditionalOnProperty(prefix = AggregatorFunctionProperties.PREFIX,
			name = "message-store-type",
			havingValue = AggregatorFunctionProperties.MessageStoreType.REDIS)
	@Import(RedisAutoConfiguration.class)
	static class Redis {

		@Bean
		public MessageGroupStore messageStore(RedisTemplate<?, ?> redisTemplate) {
			return new RedisMessageStore(redisTemplate.getConnectionFactory());
		}

	}

	@ConditionalOnClass(GemfireMessageStore.class)
	@ConditionalOnProperty(prefix = AggregatorFunctionProperties.PREFIX,
			name = "message-store-type",
			havingValue = AggregatorFunctionProperties.MessageStoreType.GEMFIRE)
	@Import(ClientCacheAutoConfiguration.class)
	@EnablePdx
	static class Gemfire {

		@Bean
		@ConditionalOnMissingBean
		public ClientRegionFactoryBean<?, ?> gemfireRegion(GemFireCache cache, AggregatorFunctionProperties properties) {
			ClientRegionFactoryBean<?, ?> clientRegionFactoryBean = new ClientRegionFactoryBean<>();
			clientRegionFactoryBean.setCache(cache);
			clientRegionFactoryBean.setName(properties.getMessageStoreEntity());
			return clientRegionFactoryBean;
		}

		@Bean
		public MessageGroupStore messageStore(Region<Object, Object> region) {
			return new GemfireMessageStore(region);
		}

	}

	@ConditionalOnClass(JdbcMessageStore.class)
	@ConditionalOnProperty(prefix = AggregatorFunctionProperties.PREFIX,
			name = "message-store-type",
			havingValue = AggregatorFunctionProperties.MessageStoreType.JDBC)
	@Import({
			DataSourceAutoConfiguration.class,
			DataSourceTransactionManagerAutoConfiguration.class })
	static class Jdbc {

		@Bean
		public MessageGroupStore messageStore(JdbcTemplate jdbcTemplate, AggregatorFunctionProperties properties) {
			JdbcMessageStore messageStore = new JdbcMessageStore(jdbcTemplate);
			if (StringUtils.hasText(properties.getMessageStoreEntity())) {
				messageStore.setTablePrefix(properties.getMessageStoreEntity());
			}
			return messageStore;
		}

	}

}
