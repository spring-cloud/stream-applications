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

import java.beans.Introspector;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import io.awspring.cloud.core.region.RegionProvider;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.aws.metadata.DynamoDbMetadataStore;
import org.springframework.integration.hazelcast.metadata.HazelcastMetadataStore;
import org.springframework.integration.jdbc.metadata.JdbcMetadataStore;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.mongodb.metadata.MongoDbMetadataStore;
import org.springframework.integration.redis.metadata.RedisMetadataStore;
import org.springframework.integration.zookeeper.metadata.ZookeeperMetadataStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Artem Bilan
 * @author Corneil du Plessis
 *
 * @since 2.0.2
 */
public class MetadataStoreAutoConfigurationTests {

	private final static List<Class<? extends ConcurrentMetadataStore>> METADATA_STORE_CLASSES =
			List.of(
					RedisMetadataStore.class,
					MongoDbMetadataStore.class,
					JdbcMetadataStore.class,
					ZookeeperMetadataStore.class,
					HazelcastMetadataStore.class,
					DynamoDbMetadataStore.class,
					SimpleMetadataStore.class
			);

	@ParameterizedTest
	@MethodSource
	public void testMetadataStore(Class<? extends ConcurrentMetadataStore> classToInclude) {
		ApplicationContextRunner contextRunner =
				new ApplicationContextRunner()
						.withUserConfiguration(TestConfiguration.class)
						.withPropertyValues("metadata.store.type=" +
								classToInclude.getSimpleName()
										.replaceFirst("MetadataStore", "")
										.toLowerCase()
										.replaceFirst("simple", "memory"))
						.withClassLoader(filteredClassLoaderBut(classToInclude));
		contextRunner
				.run(context -> {
					assertThat(context.getBeansOfType(MetadataStore.class)).hasSize(1);

					assertThat(context.getBeanNamesForType(classToInclude))
							.containsOnlyOnce(Introspector.decapitalize(classToInclude.getSimpleName()));
				});
	}

	static List<Class<? extends ConcurrentMetadataStore>> testMetadataStore() {
		return METADATA_STORE_CLASSES;
	}

	private static FilteredClassLoader filteredClassLoaderBut(Class<? extends ConcurrentMetadataStore> classToInclude) {
		return new FilteredClassLoader(
				METADATA_STORE_CLASSES.stream()
						.filter(Predicate.isEqual(classToInclude).negate())
						.toArray(Class<?>[]::new));
	}

	@Configuration
	@EnableAutoConfiguration
	public static class TestConfiguration {

		@Bean(destroyMethod = "stop")
		@ConditionalOnClass(ZookeeperMetadataStore.class)
		public static TestingServer zookeeperTestingServer() throws Exception {
			TestingServer testingServer = new TestingServer(true);

			System.setProperty("metadata.store.zookeeper.connect-string", testingServer.getConnectString());
			System.setProperty("metadata.store.zookeeper.encoding", StandardCharsets.US_ASCII.name());

			return testingServer;
		}

		@Configuration
		@ConditionalOnClass(DynamoDbMetadataStore.class)
		protected static class DynamoDbMockConfig {

			@Bean
			public static AmazonDynamoDBAsync dynamoDB() {
				AmazonDynamoDBAsync dynamoDb = mock(AmazonDynamoDBAsync.class);
				willReturn(new DescribeTableResult())
						.given(dynamoDb)
						.describeTable(any(DescribeTableRequest.class));

				return dynamoDb;
			}

			@Bean
			public static AWSCredentialsProvider awsCredentialsProvider() {
				return mock(AWSCredentialsProvider.class);
			}

			@Bean
			public static RegionProvider regionProvider() {
				return mock(RegionProvider.class);
			}

		}

	}

}
