/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.stream.app.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

import java.beans.Introspector;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.apache.curator.test.TestingServer;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.aws.metadata.DynamoDbMetadataStore;
import org.springframework.integration.gemfire.metadata.GemfireMetadataStore;
import org.springframework.integration.hazelcast.metadata.HazelcastMetadataStore;
import org.springframework.integration.jdbc.metadata.JdbcMetadataStore;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.mongodb.metadata.MongoDbMetadataStore;
import org.springframework.integration.redis.metadata.RedisMetadataStore;
import org.springframework.integration.zookeeper.metadata.ZookeeperMetadataStore;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;

/**
 * @author Artem Bilan
 *
 * @since 2.0.2
 */
@RunWith(Parameterized.class)
@Ignore
public class MetadataStoreAutoConfigurationTests {

	private final static List<Class<? extends ConcurrentMetadataStore>> METADATA_STORE_CLASSES =
			Arrays.asList(
					RedisMetadataStore.class,
					MongoDbMetadataStore.class,
					GemfireMetadataStore.class,
					JdbcMetadataStore.class,
					ZookeeperMetadataStore.class,
					HazelcastMetadataStore.class,
					DynamoDbMetadataStore.class,
					SimpleMetadataStore.class
			);

	private static FilteredClassLoader filteredClassLoaderBut(Class<? extends ConcurrentMetadataStore> classToInclude) {
		return new FilteredClassLoader(
				METADATA_STORE_CLASSES.stream()
						.filter(Predicate.isEqual(classToInclude).negate())
						.toArray(Class<?>[]::new));
	}

	private final ApplicationContextRunner contextRunner;

	private final Class<? extends ConcurrentMetadataStore> classToInclude;


	public MetadataStoreAutoConfigurationTests(Class<? extends ConcurrentMetadataStore> classToInclude) {
		this.classToInclude = classToInclude;
		this.contextRunner =
				new ApplicationContextRunner()
						.withUserConfiguration(TestConfiguration.class)
						.withClassLoader(filteredClassLoaderBut(classToInclude));
	}

	@Parameterized.Parameters
	public static Iterable<?> parameters() {
		return METADATA_STORE_CLASSES;
	}

	@Test
	public void testMetadataStore() {
		this.contextRunner
				.run(context -> {
					assertThat(context.getBeansOfType(MetadataStore.class)).hasSize(1);

					assertThat(context.getBeanNamesForType(this.classToInclude))
							.containsOnlyOnce(Introspector.decapitalize(this.classToInclude.getSimpleName()));
				});
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
