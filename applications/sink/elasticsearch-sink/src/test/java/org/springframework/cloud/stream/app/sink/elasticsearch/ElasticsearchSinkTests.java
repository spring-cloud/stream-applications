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

package org.springframework.cloud.stream.app.sink.elasticsearch;

import java.time.Duration;
import java.util.Map;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.json.JsonData;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.fn.consumer.elasticsearch.ElasticsearchConsumerConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.support.GenericMessage;


import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
public class ElasticsearchSinkTests {

	@Container
	static final ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
		DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch")
		.withTag("7.17.7")
	).withStartupTimeout(Duration.ofSeconds(120))
	.withStartupAttempts(3);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestChannelBinderConfiguration.getCompleteConfiguration(ElasticsearchSinkTestApplication.class));


	@Test
	void elasticSearchSinkWithIndexNameProperty() {
		this.contextRunner
				.withPropertyValues("spring.cloud.function.definition=elasticsearchConsumer",
						"elasticsearch.consumer.index=foo", "elasticsearch.consumer.id=1",
						"spring.elasticsearch.rest.uris=http://" + elasticsearch.getHttpHostAddress())
				.run(context -> {

					final InputDestination inputDestination = context.getBean(InputDestination.class);
					final String jsonObject = "{\"age\":10,\"dateOfBirth\":1471466076564,"
							+ "\"fullName\":\"John Doe\"}";

					inputDestination.send(new GenericMessage<>(jsonObject));

					final ElasticsearchClient elasticsearchClient = context.getBean(ElasticsearchClient.class);
					final GetRequest getRequest = new GetRequest.Builder().index("foo").id("1").build();
					final GetResponse<JsonData> response = elasticsearchClient.get(getRequest, JsonData.class);
					assertThat(response.found()).isTrue();
					assertThat(response.source()).isNotNull();
					assertThat(response.source().toJson()).isEqualTo(JsonData.fromJson(jsonObject).toJson());
				});
	}

	@Test
	void elasticSearchSinkWithIndexNameFromHeader() {
		this.contextRunner
				.withPropertyValues("spring.cloud.function.definition=elasticsearchConsumer", "elasticsearch.consumer.id=1",
						"spring.elasticsearch.rest.uris=http://" + elasticsearch.getHttpHostAddress())
				.run(context -> {

					final InputDestination inputDestination = context.getBean(InputDestination.class);
					final String jsonObject = "{\"age\":10,\"dateOfBirth\":1471466076564,"
							+ "\"fullName\":\"John Doe\"}";

					inputDestination.send(new GenericMessage<>(jsonObject, Map.of("INDEX_NAME", "foo")));

					final ElasticsearchClient elasticsearchClient = context.getBean(ElasticsearchClient.class);
					final GetRequest getRequest = new GetRequest.Builder().index("foo").id("1").build();
					final GetResponse<JsonData> response = elasticsearchClient.get(getRequest, JsonData.class);
					assertThat(response.found()).isTrue();
					assertThat(response.source()).isNotNull();
					assertThat(response.source().toJson()).isEqualTo(JsonData.fromJson(jsonObject).toJson());
				});
	}

	@SpringBootApplication
	@Import(ElasticsearchConsumerConfiguration.class)
	static class ElasticsearchSinkTestApplication {
	}

	@Configuration(proxyBeanMethods = false)
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
