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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.core.TimeValue;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.aggregator.AbstractAggregatingMessageGroupProcessor;
import org.springframework.integration.aggregator.MessageCountReleaseStrategy;
import org.springframework.integration.config.AggregatorFactoryBean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Soby Chacko
 * @author Andrea Montemaggio
 */
@Configuration
@EnableConfigurationProperties(ElasticsearchConsumerProperties.class)
public class ElasticsearchConsumerConfiguration {

	/**
	 * Message header for the Index id.
	 */
	public static final String INDEX_ID_HEADER = "INDEX_ID";

	/**
	 * Message header for the Index name.
	 */
	public static final String INDEX_NAME_HEADER = "INDEX_NAME";

	private static final Log logger = LogFactory.getLog(ElasticsearchConsumerConfiguration.class);

	@Bean
	FactoryBean<MessageHandler> aggregator(MessageGroupStore messageGroupStore, ElasticsearchConsumerProperties consumerProperties) {
		AggregatorFactoryBean aggregatorFactoryBean = new AggregatorFactoryBean();
		aggregatorFactoryBean.setCorrelationStrategy(message -> "");
		aggregatorFactoryBean.setReleaseStrategy(new MessageCountReleaseStrategy(consumerProperties.getBatchSize()));
		if (consumerProperties.getGroupTimeout() >= 0) {
			aggregatorFactoryBean.setGroupTimeoutExpression(new ValueExpression<>(consumerProperties.getGroupTimeout()));
		}
		aggregatorFactoryBean.setMessageStore(messageGroupStore);

		// Currently, there is no way to customize the splitting behavior of an aggregator receiving
		// a Collection<Message<?>> from the configured MessageGroupProcessor.
		// Thus, fooling the aggregator with a wrapper of Message<?> is just a straightforward way to preserve the
		// individual message headers and release an entire batch to downstream indexing handler.
		aggregatorFactoryBean.setProcessorBean(new AbstractAggregatingMessageGroupProcessor() {
			@Override
			protected Object aggregatePayloads(MessageGroup group, Map<String, Object> defaultHeaders) {
				Collection<Message<?>> messages = group.getMessages();
				Assert.notEmpty(messages, this.getClass().getSimpleName() + " cannot process empty message groups");
				List<Object> payloads = new ArrayList<Object>(messages.size());
				for (Message<?> message : messages) {
					payloads.add(new MessageWrapper(message));
				}
				return payloads;
			}
		});
		aggregatorFactoryBean.setExpireGroupsUponCompletion(true);
		aggregatorFactoryBean.setSendPartialResultOnExpiry(true);

		return aggregatorFactoryBean;
	}

	@Bean
	MessageGroupStore messageGroupStore() {
		SimpleMessageStore messageGroupStore = new SimpleMessageStore();
		messageGroupStore.setTimeoutOnIdle(true);
		messageGroupStore.setCopyOnGet(false);
		return messageGroupStore;
	}

	@Bean
	IntegrationFlow elasticsearchConsumerFlow(@Qualifier("aggregator") MessageHandler aggregator, ElasticsearchConsumerProperties properties,
											@Qualifier("indexingHandler") MessageHandler indexingHandler) {

		final IntegrationFlowBuilder builder =
				IntegrationFlows.from(Consumer.class, gateway -> gateway.beanName("elasticsearchConsumer"));
		if (properties.getBatchSize() > 1) {
			builder.handle(aggregator);
		}
		return builder.handle(indexingHandler).get();
	}

	@Bean
	public MessageHandler indexingHandler(RestHighLevelClient restHighLevelClient,
										ElasticsearchConsumerProperties consumerProperties) {
		return message -> {
			if (message.getPayload() instanceof Iterable) {
				BulkRequest bulkRequest = new BulkRequest();
				StreamSupport.stream(((Iterable<?>) message.getPayload()).spliterator(), false)
						.filter(MessageWrapper.class::isInstance)
						.map(itemPayload -> ((MessageWrapper) itemPayload).getMessage())
						.map(m -> buildIndexRequest(m, consumerProperties))
						.forEach(bulkRequest::add);

				index(restHighLevelClient, bulkRequest, consumerProperties.isAsync());
			}
			else {
				IndexRequest request = buildIndexRequest(message, consumerProperties);
				index(restHighLevelClient, request, consumerProperties.isAsync());
			}
		};
	}

	private IndexRequest buildIndexRequest(Message<?> message, ElasticsearchConsumerProperties consumerProperties) {
		IndexRequest request = new IndexRequest();

		String index = consumerProperties.getIndex();
		if (message.getHeaders().containsKey(INDEX_NAME_HEADER)) {
			index = (String) message.getHeaders().get(INDEX_NAME_HEADER);
		}
		request.index(index);

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

		return request;
	}

	private void index(RestHighLevelClient restHighLevelClient, BulkRequest request, boolean isAsync) {
		if (isAsync) {
			restHighLevelClient.bulkAsync(request, RequestOptions.DEFAULT, new ActionListener<BulkResponse>() {
				@Override
				public void onResponse(BulkResponse bulkResponse) {
					handleBulkResponse(bulkResponse);
				}

				@Override
				public void onFailure(Exception e) {
					throw new IllegalStateException("Error occurred while performing bulk index operation: " + e.getMessage(), e);
				}
			});
		}
		else {
			try {
				BulkResponse bulkResponse = restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
				handleBulkResponse(bulkResponse);
			}
			catch (IOException e) {
				throw new IllegalStateException("Error occurred while performing bulk index operation: " + e.getMessage(), e);
			}
		}
	}

	private void index(RestHighLevelClient restHighLevelClient, IndexRequest request, boolean isAsync) {
		if (isAsync) {
			restHighLevelClient.indexAsync(request, RequestOptions.DEFAULT, new ActionListener<IndexResponse>() {
				@Override
				public void onResponse(IndexResponse indexResponse) {
					handleResponse(indexResponse);
				}

				@Override
				public void onFailure(Exception e) {
					throw new IllegalStateException("Error occurred while indexing document: " + e.getMessage(), e);
				}
			});
		}
		else {
			try {
				IndexResponse response = restHighLevelClient.index(request, RequestOptions.DEFAULT);
				handleResponse(response);
			}
			catch (IOException e) {
				throw new IllegalStateException("Error occurred while indexing document: " + e.getMessage(), e);
			}
		}
	}

	private void handleBulkResponse(BulkResponse response) {
		if (logger.isDebugEnabled() || response.hasFailures()) {
			for (BulkItemResponse itemResponse : response) {
				if (itemResponse.isFailed()) {
					logger.error(String.format("Index operation [i=%d, id=%s, index=%s] failed: %s",
							itemResponse.getItemId(), itemResponse.getId(), itemResponse.getIndex(), itemResponse.getFailureMessage())
					);
				}
				else {
					DocWriteResponse r = itemResponse.getResponse();
					logger.debug(String.format("Index operation [i=%d, id=%s, index=%s] succeeded: document [id=%s, version=%d] was written on shard %s.",
							itemResponse.getItemId(), itemResponse.getId(), itemResponse.getIndex(), r.getId(), r.getVersion(), r.getShardId())
					);
				}
			}
		}

		if (response.hasFailures()) {
			throw new IllegalStateException("Bulk indexing operation completed with failures: " + response.buildFailureMessage());
		}
	}

	private void handleResponse(IndexResponse response) {
		logger.debug(String.format("Index operation [index=%s] succeeded: document [id=%s, version=%d] was written on shard %s.",
				response.getIndex(), response.getId(), response.getVersion(), response.getShardId())
		);
	}

	static class MessageWrapper {
		private final Message<?> message;

		MessageWrapper(Message<?> message) {
			this.message = message;
		}

		public Message<?> getMessage() {
			return message;
		}
	}
}
