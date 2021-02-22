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

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.fn.consumer.elasticsearch.ElasticsearchConsumerConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
public class ElasticsearchSinkTests {

	@Container
	static final ElasticsearchContainer elasticsearch = new ElasticsearchContainer().withStartupAttempts(5)
			.withStartupTimeout(Duration.ofMinutes(10));

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestChannelBinderConfiguration.getCompleteConfiguration(ElasticsearchSinkTestApplication.class));


	@Test
	public void tesElasticSearchSink() {
		this.contextRunner
				.withPropertyValues("spring.cloud.function.definition=elasticsearchConsumer",
						"elasticsearch.consumer.index=foo", "elasticsearch.consumer.id=1",
						"spring.elasticsearch.rest.uris=http://" + elasticsearch.getHttpHostAddress())
				.run(context -> {

					InputDestination inputDestination = context.getBean(InputDestination.class);
					String jsonObject = "{\"age\":10,\"dateOfBirth\":1471466076564,"
							+ "\"fullName\":\"John Doe\"}";
					inputDestination.send(new GenericMessage<>(jsonObject));

					RestHighLevelClient restHighLevelClient = context.getBean(RestHighLevelClient.class);
					GetRequest getRequest = new GetRequest("foo").id("1");
					final GetResponse response = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
					assertThat(response.isExists()).isTrue();
					assertThat(response.getSourceAsString()).isEqualTo(jsonObject);
				});
	}

	@SpringBootApplication
	@Import(ElasticsearchConsumerConfiguration.class)
	static class ElasticsearchSinkTestApplication {

	}

}
