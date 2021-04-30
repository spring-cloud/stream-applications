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

import org.awaitility.Awaitility;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.fn.common.config.SpelExpressionConverterConfiguration;
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
	static final ElasticsearchContainer elasticsearch = new ElasticsearchContainer().withStartupAttempts(5)
			.withStartupTimeout(Duration.ofMinutes(10));

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(ElasticsearchConsumerTestApplication.class, SpelExpressionConverterConfiguration.class);

	@Test
	public void testBasicJsonString() {
		this.contextRunner
				.withPropertyValues("elasticsearch.consumer.index=foo", "elasticsearch.consumer.id=1",
						"spring.elasticsearch.rest.uris=http://" + elasticsearch.getHttpHostAddress())
				.run(context -> {
					Consumer<Message<?>> elasticsearchConsumer = context.getBean("elasticsearchConsumer", Consumer.class);

					String jsonObject = "{\"age\":10,\"dateOfBirth\":1471466076564,"
							+ "\"fullName\":\"John Doe\"}";
					final Message<String> message = MessageBuilder.withPayload(jsonObject).build();

					elasticsearchConsumer.accept(message);

					RestHighLevelClient restHighLevelClient = context.getBean(RestHighLevelClient.class);
					GetRequest getRequest = new GetRequest("foo").id("1");
					final GetResponse response = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);

					assertThat(response.isExists()).isTrue();
					assertThat(response.getSourceAsString()).isEqualTo(jsonObject);
				});
	}

	@Test
	public void testIdPassedAsMessageHeader() {
		this.contextRunner
				.withPropertyValues("elasticsearch.consumer.index=foo",
						"spring.elasticsearch.rest.uris=http://" + elasticsearch.getHttpHostAddress())
				.run(context -> {
					Consumer<Message<?>> elasticsearchConsumer = context.getBean("elasticsearchConsumer", Consumer.class);

					String jsonObject = "{\"age\":10,\"dateOfBirth\":1471466076564,"
							+ "\"fullName\":\"John Doe\"}";
					final Message<String> message = MessageBuilder.withPayload(jsonObject)
							.setHeader(ElasticsearchConsumerConfiguration.INDEX_ID_HEADER, "2").build();

					elasticsearchConsumer.accept(message);

					RestHighLevelClient restHighLevelClient = context.getBean(RestHighLevelClient.class);
					GetRequest getRequest = new GetRequest("foo").id("2");
					final GetResponse response = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
					assertThat(response.isExists()).isTrue();
					assertThat(response.getSourceAsString()).isEqualTo(jsonObject);
					assertThat(response.getId()).isEqualTo("2");
				});
	}

	@Test
	public void testJsonAsMap() {
		this.contextRunner
				.withPropertyValues("elasticsearch.consumer.index=foo", "elasticsearch.consumer.id=3",
						"spring.elasticsearch.rest.uris=http://" + elasticsearch.getHttpHostAddress())
				.run(context -> {
					Consumer<Message<?>> elasticsearchConsumer = context.getBean("elasticsearchConsumer", Consumer.class);

					Map<String, Object> jsonMap = new HashMap<>();
					jsonMap.put("age", 10);
					jsonMap.put("dateOfBirth", 1471466076564L);
					jsonMap.put("fullName", "John Doe");
					final Message<Map<String, Object>> message = MessageBuilder.withPayload(jsonMap).build();

					elasticsearchConsumer.accept(message);

					RestHighLevelClient restHighLevelClient = context.getBean(RestHighLevelClient.class);
					GetRequest getRequest = new GetRequest("foo").id("3");

					final GetResponse response = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);

					assertThat(response.isExists()).isTrue();
					assertThat(response.getSource()).containsAllEntriesOf(jsonMap);
					assertThat(response.getId()).isEqualTo("3");
				});
	}

	@Test
	public void testXContentBuilder() {
		this.contextRunner
				.withPropertyValues("elasticsearch.consumer.index=foo", "elasticsearch.consumer.id=4",
						"spring.elasticsearch.rest.uris=http://" + elasticsearch.getHttpHostAddress())
				.run(context -> {
					Consumer<Message<?>> elasticsearchConsumer = context.getBean("elasticsearchConsumer", Consumer.class);

					XContentBuilder builder = XContentFactory.jsonBuilder();
					builder.startObject();
					builder.field("user", "kimchy");
					builder.timeField("postDate", 1471466076564L);
					builder.field("message", "trying out Elasticsearch");
					builder.endObject();

					final Message<XContentBuilder> message = MessageBuilder.withPayload(builder).build();

					elasticsearchConsumer.accept(message);

					RestHighLevelClient restHighLevelClient = context.getBean(RestHighLevelClient.class);
					GetRequest getRequest = new GetRequest("foo").id("4");
					final GetResponse response = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
					assertThat(response.isExists()).isTrue();

					assertThat(response.getSourceAsString()).isEqualTo(Strings.toString(builder));
				});
	}

	@Test
	public void testAsyncIndexing() {
		this.contextRunner
				.withPropertyValues("elasticsearch.consumer.index=foo", "elasticsearch.consumer.async=true",
						"elasticsearch.consumer.id=5",
						"spring.elasticsearch.rest.uris=http://" + elasticsearch.getHttpHostAddress())
				.run(context -> {
					Consumer<Message<?>> elasticsearchConsumer = context.getBean("elasticsearchConsumer", Consumer.class);

					String jsonObject = "{\"age\":10,\"dateOfBirth\":1471466076564,"
							+ "\"fullName\":\"John Doe\"}";
					final Message<String> message = MessageBuilder.withPayload(jsonObject).build();

					elasticsearchConsumer.accept(message);

					RestHighLevelClient restHighLevelClient = context.getBean(RestHighLevelClient.class);
					GetRequest getRequest = new GetRequest("foo").id("5");

					Awaitility.given()
							.ignoreException(ElasticsearchStatusException.class)
							.await()
							.until(() -> restHighLevelClient.get(getRequest, RequestOptions.DEFAULT).isExists());
				});
	}

	@Test
	public void testBulkIndexingWithIdFromHeader() {
		this.contextRunner
				.withPropertyValues("elasticsearch.consumer.index=foo_" + UUID.randomUUID(), "elasticsearch.consumer.batch-size=10",
						"spring.elasticsearch.rest.uris=http://" + elasticsearch.getHttpHostAddress())
				.run(context -> {
					Consumer<Message<?>> elasticsearchConsumer = context.getBean("elasticsearchConsumer", Consumer.class);
					ElasticsearchConsumerProperties properties = context.getBean(ElasticsearchConsumerProperties.class);
					RestHighLevelClient restHighLevelClient = context.getBean(RestHighLevelClient.class);

					for (int i = 0; i < properties.getBatchSize(); i++) {
						final GetRequest getRequest = new GetRequest(properties.getIndex()).id(Integer.toString(i));
						assertThatExceptionOfType(ElasticsearchStatusException.class)
								.isThrownBy(() -> restHighLevelClient.get(getRequest, RequestOptions.DEFAULT))
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
						GetRequest getRequest = new GetRequest(properties.getIndex()).id(Integer.toString(i));
						GetResponse response = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);

						assertThat(response.isExists())
								.withFailMessage("Document with id=%d cannot be found.", i)
								.isTrue();
						assertThat(response.getSource().get("seq")).isEqualTo(i);
					}
				});
	}

	@Test
	public void testBulkIndexingItemFailure() {
		this.contextRunner
				.withPropertyValues("elasticsearch.consumer.index=foo_" + UUID.randomUUID(), "elasticsearch.consumer.batch-size=10",
						"spring.elasticsearch.rest.uris=http://" + elasticsearch.getHttpHostAddress())
				.run(context -> {
					Consumer<Message<?>> elasticsearchConsumer = context.getBean("elasticsearchConsumer", Consumer.class);
					ElasticsearchConsumerProperties properties = context.getBean(ElasticsearchConsumerProperties.class);
					RestHighLevelClient restHighLevelClient = context.getBean(RestHighLevelClient.class);

					for (int i = 0; i < properties.getBatchSize(); i++) {
						final GetRequest getRequest = new GetRequest(properties.getIndex()).id(Integer.toString(i));
						assertThatExceptionOfType(ElasticsearchStatusException.class)
								.isThrownBy(() -> restHighLevelClient.get(getRequest, RequestOptions.DEFAULT))
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
					Consumer<Message<?>> elasticsearchConsumer = context.getBean("elasticsearchConsumer", Consumer.class);
					ElasticsearchConsumerProperties properties = context.getBean(ElasticsearchConsumerProperties.class);

					final String dynamicIndex = properties.getIndex() + "-2";

					String jsonObject = "{\"age\":10,\"dateOfBirth\":1471466076564,"
							+ "\"fullName\":\"John Doe\"}";
					final Message<String> message = MessageBuilder.withPayload(jsonObject)
							.setHeader(ElasticsearchConsumerConfiguration.INDEX_ID_HEADER, "2")
							.setHeader(ElasticsearchConsumerConfiguration.INDEX_NAME_HEADER, dynamicIndex)
							.build();

					elasticsearchConsumer.accept(message);

					RestHighLevelClient restHighLevelClient = context.getBean(RestHighLevelClient.class);
					GetRequest getRequest = new GetRequest(dynamicIndex).id("2");
					final GetResponse response = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
					assertThat(response.isExists()).isTrue();
					assertThat(response.getSourceAsString()).isEqualTo(jsonObject);
					assertThat(response.getId()).isEqualTo("2");
				});
	}

	@SpringBootApplication
	static class ElasticsearchConsumerTestApplication {
	}
}
