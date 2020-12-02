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

package org.springframework.cloud.fn.consumer.elasticsearch;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(ElasticsearchConsumerProperties.class)
public class ElasticsearchConsumerConfiguration {

	/**
	 * Message header for the Index id.
	 */
	public static final String INDEX_ID_HEADER = "INDEX_ID";

	private static final Log logger = LogFactory.getLog(ElasticsearchConsumerConfiguration.class);

	@Bean
	public Consumer<Message<?>> elasticsearchConsumer(RestHighLevelClient restHighLevelClient,
													ElasticsearchConsumerProperties consumerProperties) {
		return message -> {

			IndexRequest request = new IndexRequest(consumerProperties.getIndex());

			String id = "";
			if (message.getHeaders().containsKey(INDEX_ID_HEADER)) {
				id = (String) message.getHeaders().get(INDEX_ID_HEADER);
			}
			else if (consumerProperties.getId() != null) {
				id = consumerProperties.getId().getValue(message, String.class);
			}
			request.id(id);

			if (message.getPayload() instanceof String) {
				request.source((String) message.getPayload(), XContentType.JSON);
			}
			else if (message.getPayload() instanceof Map) {
				request.source((Map<String, ?>) message.getPayload(), XContentType.JSON);
			}
			else if (message.getPayload() instanceof XContentBuilder) {
				request.source((XContentBuilder) message.getPayload());
			}

			if (!StringUtils.isEmpty(consumerProperties.getRouting())) {
				request.routing(consumerProperties.getRouting());
			}
			if (consumerProperties.getTimeoutSeconds() > 0) {
				request.timeout(TimeValue.timeValueSeconds(consumerProperties.getTimeoutSeconds()));
			}

			index(restHighLevelClient, request, consumerProperties.isAsync());
		};
	}

	private void index(RestHighLevelClient restHighLevelClient,
					IndexRequest request, boolean isAsync) {
		if (isAsync) {
			restHighLevelClient.indexAsync(request, RequestOptions.DEFAULT, new ActionListener<IndexResponse>() {
				@Override
				public void onResponse(IndexResponse indexResponse) {
					if (logger.isDebugEnabled()) {
						logger.debug("Document with ID: " + indexResponse.getId() + " has been indexed");
					}
				}

				@Override
				public void onFailure(Exception e) {
					throw new IllegalStateException("Error occurred while indexing the document", e);
				}
			});
		}
		else {
			try {
				restHighLevelClient.index(request, RequestOptions.DEFAULT);
			}
			catch (IOException e) {
				throw new IllegalStateException("Error occurred while indexing the document", e);
			}
		}
	}
}
