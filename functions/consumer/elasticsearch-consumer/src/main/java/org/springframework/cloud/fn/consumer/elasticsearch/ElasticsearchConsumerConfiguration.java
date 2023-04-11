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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
 * @author Corneil du Plessis
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
	IntegrationFlow elasticsearchConsumerFlow(
		@Qualifier("aggregator") MessageHandler aggregator,
		ElasticsearchConsumerProperties properties,
		@Qualifier("indexingHandler") MessageHandler indexingHandler
	) {

		final IntegrationFlowBuilder builder =
			IntegrationFlow.from(MessageConsumer.class, gateway -> gateway.beanName("elasticsearchConsumer"));
		if (properties.getBatchSize() > 1) {
			builder.handle(aggregator);
		}
		return builder.handle(indexingHandler).get();
	}

	@Bean
	public MessageHandler indexingHandler(
		ElasticsearchClient elasticsearchClient,
		ElasticsearchConsumerProperties consumerProperties
	) {
		return message -> {
			if (message.getPayload() instanceof Iterable) {
				BulkRequest.Builder builder = new BulkRequest.Builder();
				StreamSupport.stream(((Iterable<?>) message.getPayload()).spliterator(), false)
					.filter(MessageWrapper.class::isInstance)
					.map(itemPayload -> ((MessageWrapper) itemPayload).getMessage())
					.map(m -> buildIndexRequest(m, consumerProperties))
					.forEach(indexRequest ->
						builder.operations(builder1 -> builder1.index(idx -> idx.index(indexRequest.index()).id(indexRequest.id()).document(indexRequest.document())))
					);

				index(elasticsearchClient, builder.build(), consumerProperties.isAsync());
			}
			else {
				IndexRequest request = buildIndexRequest(message, consumerProperties);
				index(elasticsearchClient, request, consumerProperties.isAsync());
			}
		};
	}

	private IndexRequest buildIndexRequest(Message<?> message, ElasticsearchConsumerProperties consumerProperties) {
		IndexRequest.Builder requestBuilder = new IndexRequest.Builder();

		String index = consumerProperties.getIndex();
		if (message.getHeaders().containsKey(INDEX_NAME_HEADER)) {
			index = (String) message.getHeaders().get(INDEX_NAME_HEADER);
		}
		requestBuilder.index(index);

		String id = "";
		if (message.getHeaders().containsKey(INDEX_ID_HEADER)) {
			id = (String) message.getHeaders().get(INDEX_ID_HEADER);
		}
		else if (consumerProperties.getId() != null) {
			id = consumerProperties.getId().getValue(message, String.class);
		}
		requestBuilder.id(id);

		if (message.getPayload() instanceof String) {
			requestBuilder.withJson(new StringReader((String) message.getPayload()));
		}
		else if (message.getPayload() instanceof Map) {
			requestBuilder.document(message.getPayload());
		}

		if (StringUtils.hasText(consumerProperties.getRouting())) {
			requestBuilder.routing(consumerProperties.getRouting());
		}
		if (consumerProperties.getTimeoutSeconds() > 0) {
			requestBuilder.timeout(new Time.Builder().time(consumerProperties.getTimeoutSeconds() + "s").build());
		}

		return requestBuilder.build();
	}

	private void index(ElasticsearchClient elasticsearchClient, BulkRequest request, boolean isAsync) {
		if (isAsync) {
			ElasticsearchAsyncClient elasticsearchAsyncClient = new ElasticsearchAsyncClient(elasticsearchClient._transport());
			CompletableFuture<BulkResponse> responseCompletableFuture = elasticsearchAsyncClient.bulk(request);
			responseCompletableFuture.whenComplete((bulkResponse, x) -> {
				if (x != null) {
					throw new IllegalStateException("Error occurred while performing bulk index operation: " + x.getMessage(), x);
				}
				else {
					handleBulkResponse(bulkResponse);
				}
			});

		}
		else {
			try {
				BulkResponse bulkResponse = elasticsearchClient.bulk(request);
				handleBulkResponse(bulkResponse);
			}
			catch (IOException e) {
				throw new IllegalStateException("Error occurred while performing bulk index operation: " + e.getMessage(), e);
			}
		}
	}

	private void index(ElasticsearchClient elasticsearchClient, IndexRequest request, boolean isAsync) {
		if (isAsync) {
			ElasticsearchAsyncClient elasticsearchAsyncClient = new ElasticsearchAsyncClient(elasticsearchClient._transport());
			CompletableFuture<IndexResponse> responseCompletableFuture = elasticsearchAsyncClient.index(request);
			responseCompletableFuture.whenComplete((indexResponse, x) -> {
				if (x != null) {
					throw new IllegalStateException("Error occurred while indexing document: " + x.getMessage(), x);
				}
				else {
					handleResponse(indexResponse);
				}
			});
		}
		else {
			try {
				IndexResponse response = elasticsearchClient.index(request);
				handleResponse(response);
			}
			catch (IOException e) {
				throw new IllegalStateException("Error occurred while indexing document: " + e.getMessage(), e);
			}
		}
	}

	private void handleBulkResponse(BulkResponse response) {
		if (logger.isDebugEnabled() || response.errors()) {
			for (BulkResponseItem itemResponse : response.items()) {
				if (itemResponse.error() != null) {
					if (logger.isDebugEnabled()) {
						logger.debug("itemResponse.error=" + itemResponse.error());
					}
					logger.error(String.format("Index operation [id=%s, index=%s] failed: %s",
						itemResponse.id(), itemResponse.index(), itemResponse.error().toString())
					);
				}
				else {
					var r = itemResponse.get();
					if (r != null) {
						if (logger.isDebugEnabled()) {
							logger.debug("itemResponse:" + r);
						}
						logger.debug(String.format("Index operation [id=%s, index=%s] succeeded: document [id=%s, version=%s] was written on shard %s.",
							itemResponse.id(), itemResponse.index(), r.source().get("id"), r.source().get("version"), r.source().get("shardId"))
						);
					}
					else {
						logger.debug(String.format("Index operation [id=%s, index=%s] succeeded", itemResponse.id(), itemResponse.index()));
					}
				}
			}
		}

		if (response.errors()) {
			String error = response.items()
				.stream()
				.map(bulkResponseItem ->  bulkResponseItem.error() != null ? bulkResponseItem.error().toString() : "")
				.reduce((errorCause, errorCause2) -> errorCause != null ? errorCause + " : " + errorCause2 : errorCause2)
				.orElseGet(response::toString);
			throw new IllegalStateException("Bulk indexing operation completed with failures: " + error);
		}
	}

	private void handleResponse(IndexResponse response) {
		logger.debug(String.format("Index operation [index=%s] succeeded: document [id=%s, version=%d] was written on shard %s.",
			response.index(), response.id(), response.version(), response.shards().toString())
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

	private interface MessageConsumer extends Consumer<Message<?>> {

	}

}
