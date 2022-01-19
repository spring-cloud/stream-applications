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

package org.springframework.cloud.fn.aggregator;

import java.util.function.Function;

import reactor.core.publisher.Flux;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor;
import org.springframework.integration.aggregator.ExpressionEvaluatingCorrelationStrategy;
import org.springframework.integration.aggregator.ExpressionEvaluatingMessageGroupProcessor;
import org.springframework.integration.aggregator.ExpressionEvaluatingReleaseStrategy;
import org.springframework.integration.aggregator.MessageGroupProcessor;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.config.AggregatorFactoryBean;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * @author Artem Bilan
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AggregatorFunctionProperties.class)
public class AggregatorFunctionConfiguration {

	@Autowired
	private AggregatorFunctionProperties properties;

	@Autowired
	private BeanFactory beanFactory;

	@Bean
	public Function<Flux<Message<?>>, Flux<Message<?>>> aggregatorFunction(FluxMessageChannel inputChannel,
			FluxMessageChannel outputChannel) {

		return input -> Flux.from(outputChannel)
				.doOnRequest((request) -> inputChannel.subscribeTo(input));
	}

	@Bean
	public FluxMessageChannel inputChannel() {
		return new FluxMessageChannel();
	}

	@Bean
	public FluxMessageChannel outputChannel() {
		return new FluxMessageChannel();
	}

	@Bean
	@ServiceActivator(inputChannel = "inputChannel")
	public AggregatorFactoryBean aggregator(
			ObjectProvider<CorrelationStrategy> correlationStrategy,
			ObjectProvider<ReleaseStrategy> releaseStrategy,
			ObjectProvider<MessageGroupProcessor> messageGroupProcessor,
			ObjectProvider<MessageGroupStore> messageStore,
			@Qualifier("outputChannel") MessageChannel outputChannel) {

		AggregatorFactoryBean aggregator = new AggregatorFactoryBean();
		aggregator.setExpireGroupsUponCompletion(true);
		aggregator.setSendPartialResultOnExpiry(true);
		aggregator.setGroupTimeoutExpression(this.properties.getGroupTimeout());

		aggregator.setCorrelationStrategy(correlationStrategy.getIfAvailable());
		aggregator.setReleaseStrategy(releaseStrategy.getIfAvailable());

		MessageGroupProcessor groupProcessor = messageGroupProcessor.getIfAvailable();

		if (groupProcessor == null) {
			groupProcessor = new DefaultAggregatingMessageGroupProcessor();
			((BeanFactoryAware) groupProcessor).setBeanFactory(this.beanFactory);
		}
		aggregator.setProcessorBean(groupProcessor);

		aggregator.setMessageStore(messageStore.getIfAvailable());
		aggregator.setOutputChannel(outputChannel);

		return aggregator;
	}

	@Bean
	@ConditionalOnProperty(prefix = AggregatorFunctionProperties.PREFIX, name = "correlation")
	@ConditionalOnMissingBean
	public CorrelationStrategy correlationStrategy() {
		return new ExpressionEvaluatingCorrelationStrategy(this.properties.getCorrelation());
	}

	@Bean
	@ConditionalOnProperty(prefix = AggregatorFunctionProperties.PREFIX, name = "release")
	@ConditionalOnMissingBean
	public ReleaseStrategy releaseStrategy() {
		return new ExpressionEvaluatingReleaseStrategy(this.properties.getRelease());
	}

	@Bean
	@ConditionalOnProperty(prefix = AggregatorFunctionProperties.PREFIX, name = "aggregation")
	@ConditionalOnMissingBean
	public MessageGroupProcessor messageGroupProcessor() {
		return new ExpressionEvaluatingMessageGroupProcessor(this.properties.getAggregation().getExpressionString());
	}


	@Configuration
	@ConditionalOnMissingBean(MessageGroupStore.class)
	@Import({ MessageStoreConfiguration.Mongo.class, MessageStoreConfiguration.Redis.class,
			MessageStoreConfiguration.Gemfire.class, MessageStoreConfiguration.Jdbc.class })
	protected static class MessageStoreAutoConfiguration {

	}

}
