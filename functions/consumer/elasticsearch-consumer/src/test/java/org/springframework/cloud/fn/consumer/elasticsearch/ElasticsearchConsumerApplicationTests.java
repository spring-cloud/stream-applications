/*
 * Copyright 2020-2021 the original author or authors.
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

package org.springframework.cloud.fn.consumer.elasticsearch;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.json.JsonData;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.fn.common.config.SpelExpressionConverterConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Soby Chacko
 * @author Andrea Montemaggio
 */
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
public class ElasticsearchConsumerApplicationTests {

	@Container
	static final ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
		DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch")
			.withTag("7.17.7")
	).withStartupTimeout(Duration.ofSeconds(120))
		.withStartupAttempts(3);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(ElasticsearchConsumerTestApplication.class, SpelExpressionConverterConfiguration.class);

	@Test
	public void testBasicJsonString() {
		this.contextRunner
			.withPropertyValues("elasticsearch.consumer.index=foo", "elasticsearch.consumer.id=1",
				"spring.elasticsearch.rest.uris=http://" + elasticsearch.getHttpHostAddress())
			.run(context -> {
				final Consumer<Message<?>> elasticsearchConsumer = context.getBean("elasticsearchConsumer", Consumer.class);

				final String jsonObject = "{\"age\":10,\"dateOfBirth\":1471466076564,"
					+ "\"fullName\":\"John Doe\"}";
				final Message<String> message = MessageBuilder.withPayload(jsonObject).build();

				elasticsearchConsumer.accept(message);
				final ElasticsearchClient elasticsearchClient = context.getBean(ElasticsearchClient.class);
				final GetRequest getRequest = new GetRequest.Builder().index("foo").id("1").build();
				final GetResponse<JsonData> response = elasticsearchClient.get(getRequest, JsonData.class);

				assertThat(response.found()).isTrue();
				assertThat(response.source()).isNotNull();
				assertThat(response.source().toJson()).isEqualTo(JsonData.fromJson(jsonObject).toJson());
			});
	}

	@Test
	public void testIdPassedAsMessageHeader() {
		this.contextRunner
			.withPropertyValues("elasticsearch.consumer.index=foo",
				"spring.elasticsearch.rest.uris=http://" + elasticsearch.getHttpHostAddress())
			.run(context -> {
				final Consumer<Message<?>> elasticsearchConsumer = context.getBean("elasticsearchConsumer", Consumer.class);

				final String jsonObject = "{\"age\":10,\"dateOfBirth\":1471466076564,"
					+ "\"fullName\":\"John Doe\"}";
				final Message<String> message = MessageBuilder.withPayload(jsonObject)
					.setHeader(ElasticsearchConsumerConfiguration.INDEX_ID_HEADER, "2").build();

				elasticsearchConsumer.accept(message);

				final ElasticsearchClient elasticsearchClient = context.getBean(ElasticsearchClient.class);
				final GetRequest getRequest = new GetRequest.Builder().index("foo").id("2").build();
				final GetResponse<JsonData> response = elasticsearchClient.get(getRequest, JsonData.class);
				assertThat(response.found()).isTrue();
				assertThat(response.source()).isNotNull();
				assertThat(response.source().toJson()).isEqualTo(JsonData.fromJson(jsonObject).toJson());
				assertThat(response.id()).isEqualTo("2");
			});
	}

	@Test
	public void testJsonAsMap() {
		this.contextRunner
			.withPropertyValues("elasticsearch.consumer.index=foo", "elasticsearch.consumer.id=3",
				"spring.elasticsearch.rest.uris=http://" + elasticsearch.getHttpHostAddress())
			.run(context -> {
				final Consumer<Message<?>> elasticsearchConsumer = context.getBean("elasticsearchConsumer", Consumer.class);

				final Map<String, Object> jsonMap = new HashMap<>();
				jsonMap.put("age", 10);
				jsonMap.put("dateOfBirth", 1471466076564L);
				jsonMap.put("fullName", "John Doe");
				final Message<Map<String, Object>> message = MessageBuilder.withPayload(jsonMap).build();

				elasticsearchConsumer.accept(message);
				final ElasticsearchClient elasticsearchClient = context.getBean(ElasticsearchClient.class);
				final GetRequest getRequest = new GetRequest.Builder().index("foo").id("3").build();
				final GetResponse<HashMap> response = elasticsearchClient.get(getRequest, HashMap.class);

				assertThat(response.found()).isTrue();
				HashMap map = response.source();

				jsonMap.entrySet().forEach(entry -> {
					Object value = map.get(entry.getKey());
					assertThat(value).isNotNull();
					assertThat(value).isEqualTo(entry.getValue());
				});
				assertThat(response.id()).isEqualTo("3");
			});
	}

	@Test
	public void testAsyncIndexing() {
		this.contextRunner
			.withPropertyValues("elasticsearch.consumer.index=foo", "elasticsearch.consumer.async=true",
				"elasticsearch.consumer.id=5",
				"spring.elasticsearch.rest.uris=http://" + elasticsearch.getHttpHostAddress())
			.run(context -> {
				final Consumer<Message<?>> elasticsearchConsumer = context.getBean("elasticsearchConsumer", Consumer.class);

				final String jsonObject = "{\"age\":10,\"dateOfBirth\":1471466076564,"
					+ "\"fullName\":\"John Doe\"}";
				final Message<String> message = MessageBuilder.withPayload(jsonObject).build();

				elasticsearchConsumer.accept(message);

				final ElasticsearchClient elasticsearchClient = context.getBean(ElasticsearchClient.class);
				final GetRequest getRequest = new GetRequest.Builder().index("foo").id("5").build();

				Awaitility.given()
					.ignoreException(ElasticsearchException.class)
					.await()
					.until(() -> elasticsearchClient.get(getRequest, JsonData.class).found());
			});
	}

	@Test
	public void testBulkIndexingWithIdFromHeader() {
		this.contextRunner
			.withPropertyValues("elasticsearch.consumer.index=foo_" + UUID.randomUUID(), "elasticsearch.consumer.batch-size=10",
				"spring.elasticsearch.rest.uris=http://" + elasticsearch.getHttpHostAddress())
			.run(context -> {
				final Consumer<Message<?>> elasticsearchConsumer = context.getBean("elasticsearchConsumer", Consumer.class);
				final ElasticsearchConsumerProperties properties = context.getBean(ElasticsearchConsumerProperties.class);
				final ElasticsearchClient elasticsearchClient = context.getBean(ElasticsearchClient.class);


				for (int i = 0; i < properties.getBatchSize(); i++) {
					final GetRequest getRequest = new GetRequest.Builder().index(properties.getIndex()).id(Integer.toString(i)).build();
					assertThatExceptionOfType(ElasticsearchException.class)
						.isThrownBy(() -> elasticsearchClient.get(getRequest, JsonData.class))
						.withFailMessage("Expected index not found exception for message %d")
						.withMessageContaining("index_not_found_exception");

					final Message<String> message = MessageBuilder
						.withPayload("{\"seq\":" + i + ",\"age\":10,\"dateOfBirth\":1471466076564,"
							+ "\"fullName\":\"John Doe\"}")
						.setHeader(ElasticsearchConsumerConfiguration.INDEX_ID_HEADER, Integer.toString(i))
						.build();

					elasticsearchConsumer.accept(message);
				}

				for (int i = 0; i < properties.getBatchSize(); i++) {
					final GetRequest getRequest = new GetRequest.Builder().index(properties.getIndex()).id(Integer.toString(i)).build();
					final GetResponse<JsonData> response = elasticsearchClient.get(getRequest, JsonData.class);

					assertThat(response.found())
						.withFailMessage("Document with id=%d cannot be found.", i)
						.isTrue();
					assertThat(response.source().toJson().asJsonObject().get("seq").toString()).isEqualTo(Integer.toString(i));
				}
			});
	}

	@Test
	public void testBulkIndexingItemFailure() {
		this.contextRunner
			.withPropertyValues("elasticsearch.consumer.index=foo_" + UUID.randomUUID(), "elasticsearch.consumer.batch-size=10",
				"spring.elasticsearch.rest.uris=http://" + elasticsearch.getHttpHostAddress())
			.run(context -> {
				final Consumer<Message<?>> elasticsearchConsumer = context.getBean("elasticsearchConsumer", Consumer.class);
				final ElasticsearchConsumerProperties properties = context.getBean(ElasticsearchConsumerProperties.class);
				final ElasticsearchClient elasticsearchClient = context.getBean(ElasticsearchClient.class);


				for (int i = 0; i < properties.getBatchSize(); i++) {
					final GetRequest getRequest = new GetRequest.Builder().index(properties.getIndex()).id(Integer.toString(i)).build();
					assertThatExceptionOfType(ElasticsearchException.class)
						.isThrownBy(() -> elasticsearchClient.get(getRequest, JsonData.class))
						.withFailMessage("Expected index not found exception for message %d")
						.withMessageContaining("index_not_found_exception");

					MessageBuilder<String> builder = MessageBuilder
						.withPayload("{\"seq\":" + i + ",\"age\":10,\"dateOfBirth\":1471466076564,"
							+ "\"fullName\":\"John Doe\"}")
						.setHeader(ElasticsearchConsumerConfiguration.INDEX_ID_HEADER, Integer.toString(i));

					if (i == 0) {
						// set an invalid index name to make the first request fail
						builder.setHeader(ElasticsearchConsumerConfiguration.INDEX_NAME_HEADER, "_" + properties.getIndex());
					}

					final Message<String> message = builder.build();

					if (i < properties.getBatchSize() - 1) {
						elasticsearchConsumer.accept(message);
					}
					else {
						// last invocation
						assertThatIllegalStateException()
							.isThrownBy(() -> elasticsearchConsumer.accept(message))
							.withMessageContaining("Bulk indexing operation completed with failures");
					}
				}
			});
	}

	@Test
	public void testIndexFromMessageHeader() {
		this.contextRunner
			.withPropertyValues("elasticsearch.consumer.index=foo",
				"spring.elasticsearch.rest.uris=http://" + elasticsearch.getHttpHostAddress())
			.run(context -> {
				final Consumer<Message<?>> elasticsearchConsumer = context.getBean("elasticsearchConsumer", Consumer.class);
				final ElasticsearchConsumerProperties properties = context.getBean(ElasticsearchConsumerProperties.class);

				final String dynamicIndex = properties.getIndex() + "-2";

				final String jsonObject = "{\"age\":10,\"dateOfBirth\":1471466076564,"
					+ "\"fullName\":\"John Doe\"}";
				final Message<String> message = MessageBuilder.withPayload(jsonObject)
					.setHeader(ElasticsearchConsumerConfiguration.INDEX_ID_HEADER, "2")
					.setHeader(ElasticsearchConsumerConfiguration.INDEX_NAME_HEADER, dynamicIndex)
					.build();

				elasticsearchConsumer.accept(message);
				final ElasticsearchClient elasticsearchClient = context.getBean(ElasticsearchClient.class);

				GetRequest getRequest = new GetRequest.Builder().index(dynamicIndex).id("2").build();
				final GetResponse<JsonData> response = elasticsearchClient.get(getRequest, JsonData.class);
				assertThat(response.found()).isTrue();
				assertThat(response.source()).isNotNull();
				assertThat(response.source().toJson()).isEqualTo(JsonData.fromJson(jsonObject).toJson());
				assertThat(response.id()).isEqualTo("2");
			});
	}

	@SpringBootApplication
	static class ElasticsearchConsumerTestApplication {
	}

	@Configuration
	static class Config extends ElasticsearchConfiguration {
		@NonNull
		@Override
		public ClientConfiguration clientConfiguration() {
			return ClientConfiguration.builder()
				.connectedTo(elasticsearch.getHttpHostAddress())
				.build();
		}
	}
}
