/*
 * Copyright 2018-2021 the original author or authors.
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

package org.springframework.cloud.fn.common.metadata.store;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.hazelcast.core.HazelcastInstance;
import io.awspring.cloud.core.region.RegionProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryForever;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.integration.aws.metadata.DynamoDbMetadataStore;
import org.springframework.integration.gemfire.metadata.GemfireMetadataStore;
import org.springframework.integration.hazelcast.metadata.HazelcastMetadataStore;
import org.springframework.integration.jdbc.metadata.JdbcMetadataStore;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.MetadataStoreListener;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.mongodb.metadata.MongoDbMetadataStore;
import org.springframework.integration.redis.metadata.RedisMetadataStore;
import org.springframework.integration.zookeeper.metadata.ZookeeperMetadataStore;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Artem Bilan
 * @author David Turanski
 * @since 2.0.2
 */
@Configuration
@ConditionalOnClass(ConcurrentMetadataStore.class)
@EnableConfigurationProperties(MetadataStoreProperties.class)
public class MetadataStoreAutoConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "metadata.store", name = "type", havingValue = "memory", matchIfMissing = true)
	@ConditionalOnMissingBean
	public ConcurrentMetadataStore simpleMetadataStore() {
		return new SimpleMetadataStore();
	}

	@ConditionalOnProperty(prefix = "metadata.store", name = "type", havingValue = "redis")
	static class Redis {

		@Bean
		@ConditionalOnMissingBean
		public ConcurrentMetadataStore redisMetadataStore(RedisTemplate<String, ?> redisTemplate,
				MetadataStoreProperties metadataStoreProperties) {

			return new RedisMetadataStore(redisTemplate, metadataStoreProperties.getRedis().getKey());
		}

	}

	@ConditionalOnProperty(prefix = "metadata.store", name = "type", havingValue = "mongodb")
	static class Mongo {

		@Bean
		@ConditionalOnMissingBean
		public ConcurrentMetadataStore mongoDbMetadataStore(MongoTemplate mongoTemplate,
				MetadataStoreProperties metadataStoreProperties) {

			return new MongoDbMetadataStore(mongoTemplate, metadataStoreProperties.getMongoDb().getCollection());
		}

	}

	@ConditionalOnProperty(prefix = "metadata.store", name = "type", havingValue = "gemfire")
	@Import(ClientCacheAutoConfiguration.class)
	static class Gemfire {

		@Bean
		@ConditionalOnMissingBean
		public ClientRegionFactoryBean<?, ?> gemfireRegion(GemFireCache cache,
				MetadataStoreProperties metadataStoreProperties) {

			ClientRegionFactoryBean<?, ?> clientRegionFactoryBean = new ClientRegionFactoryBean<>();
			clientRegionFactoryBean.setCache(cache);
			clientRegionFactoryBean.setName(metadataStoreProperties.getGemfire().getRegion());
			return clientRegionFactoryBean;
		}

		@Bean
		@ConditionalOnMissingBean
		public ConcurrentMetadataStore gemfireMetadataStore(Region<?, ?> region,
				ObjectProvider<MetadataStoreListener> metadataStoreListenerObjectProvider) {

			@SuppressWarnings("unchecked")
			GemfireMetadataStore gemfireMetadataStore = new GemfireMetadataStore((Region<String, String>) region);
			metadataStoreListenerObjectProvider.ifAvailable(gemfireMetadataStore::addListener);

			return gemfireMetadataStore;
		}

	}

	@ConditionalOnProperty(prefix = "metadata.store", name = "type", havingValue = "hazelcast")
	static class Hazelcast {

		@Bean
		@ConditionalOnMissingBean
		public HazelcastInstance hazelcastInstance() {
			return com.hazelcast.core.Hazelcast.newHazelcastInstance();
		}

		@Bean
		@ConditionalOnMissingBean
		public ConcurrentMetadataStore hazelcastMetadataStore(HazelcastInstance hazelcastInstance,
				ObjectProvider<MetadataStoreListener> metadataStoreListenerObjectProvider) {

			HazelcastMetadataStore hazelcastMetadataStore = new HazelcastMetadataStore(hazelcastInstance);
			metadataStoreListenerObjectProvider.ifAvailable(hazelcastMetadataStore::addListener);
			return hazelcastMetadataStore;
		}

	}

	@ConditionalOnProperty(prefix = "metadata.store", name = "type", havingValue = "zookeeper")
	static class Zookeeper {

		@Bean(initMethod = "start")
		@ConditionalOnMissingBean
		public CuratorFramework curatorFramework(MetadataStoreProperties metadataStoreProperties) {
			MetadataStoreProperties.Zookeeper zookeeperProperties = metadataStoreProperties.getZookeeper();
			return CuratorFrameworkFactory.newClient(zookeeperProperties.getConnectString(),
					new RetryForever(zookeeperProperties.getRetryInterval()));
		}

		@Bean
		@ConditionalOnMissingBean
		public ConcurrentMetadataStore zookeeperMetadataStore(CuratorFramework curatorFramework,
				MetadataStoreProperties metadataStoreProperties,
				ObjectProvider<MetadataStoreListener> metadataStoreListenerObjectProvider) {

			MetadataStoreProperties.Zookeeper zookeeperProperties = metadataStoreProperties.getZookeeper();
			ZookeeperMetadataStore zookeeperMetadataStore = new ZookeeperMetadataStore(curatorFramework);
			zookeeperMetadataStore.setEncoding(zookeeperProperties.getEncoding().name());
			zookeeperMetadataStore.setRoot(zookeeperProperties.getRoot());
			metadataStoreListenerObjectProvider.ifAvailable(zookeeperMetadataStore::addListener);
			return zookeeperMetadataStore;
		}

	}

	@ConditionalOnProperty(prefix = "metadata.store", name = "type", havingValue = "dynamodb")
	static class DynamoDb {

		@Bean
		@ConditionalOnMissingBean
		public AmazonDynamoDBAsync dynamoDB(AWSCredentialsProvider awsCredentialsProvider,
				RegionProvider regionProvider) {

			return AmazonDynamoDBAsyncClientBuilder.standard()
					.withCredentials(awsCredentialsProvider)
					.withRegion(
							regionProvider.getRegion()
									.getName())
					.build();
		}

		@Bean
		@ConditionalOnMissingBean
		public ConcurrentMetadataStore dynamoDbMetadataStore(AmazonDynamoDBAsync dynamoDB,
				MetadataStoreProperties metadataStoreProperties) {

			MetadataStoreProperties.DynamoDb dynamoDbProperties = metadataStoreProperties.getDynamoDb();

			DynamoDbMetadataStore dynamoDbMetadataStore = new DynamoDbMetadataStore(dynamoDB,
					dynamoDbProperties.getTable());

			dynamoDbMetadataStore.setReadCapacity(dynamoDbProperties.getReadCapacity());
			dynamoDbMetadataStore.setWriteCapacity(dynamoDbProperties.getWriteCapacity());
			dynamoDbMetadataStore.setCreateTableDelay(dynamoDbProperties.getCreateDelay());
			dynamoDbMetadataStore.setCreateTableRetries(dynamoDbProperties.getCreateRetries());
			if (dynamoDbProperties.getTimeToLive() != null) {
				dynamoDbMetadataStore.setTimeToLive(dynamoDbProperties.getTimeToLive());
			}

			return dynamoDbMetadataStore;
		}

	}

	@ConditionalOnProperty(prefix = "metadata.store", name = "type", havingValue = "jdbc")
	static class Jdbc {
		@Bean
		@ConditionalOnMissingBean
		public ConcurrentMetadataStore jdbcMetadataStore(JdbcTemplate jdbcTemplate,
				MetadataStoreProperties metadataStoreProperties) {

			MetadataStoreProperties.Jdbc jdbcProperties = metadataStoreProperties.getJdbc();

			JdbcMetadataStore jdbcMetadataStore = new JdbcMetadataStore(jdbcTemplate);
			jdbcMetadataStore.setTablePrefix(jdbcProperties.getTablePrefix());
			jdbcMetadataStore.setRegion(jdbcProperties.getRegion());

			return jdbcMetadataStore;
		}

	}

}
