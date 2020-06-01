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

package org.springframework.cloud.fn.supplier.geode.cq;

import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.pdx.PdxInstance;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.geode.GeodeClientRegionConfiguration;
import org.springframework.cloud.fn.common.geode.JsonPdxFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.gemfire.client.Interest;
import org.springframework.data.gemfire.listener.ContinuousQueryListener;
import org.springframework.data.gemfire.listener.ContinuousQueryListenerContainer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.gemfire.inbound.ContinuousQueryMessageProducer;
import org.springframework.integration.router.PayloadTypeRouter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * The Geode CQ Supplier provides a {@link ContinuousQueryMessageProducer} which produces
 * a message for each event included in the given query. Internally it uses a
 * {@link ContinuousQueryListenerContainer} which, by default, emits an
 * {@link org.apache.geode.cache.query.CqEvent} which holds all of the event details. A
 * SpEl Expression, given by the property 'entryEventExpression' is used to extract
 * required information from the payload. The default expression is 'newValue' which
 * returns the updated object. This may not be ideal for every use case especially if it
 * does not provide the key value. The key is referenced by the field 'key'. Also
 * available are 'operation' (the operation associated with the event, and 'oldValue'. If
 * the cached key and value types are primitives, an simple expression like "key + ':' +
 * newValue" may work.
 *
 * More complex transformations, such as Json, will require customization. To access the
 * original object, set 'entryEntryExpression' to '#root' or "#this'.
 *
 * This converts payloads of type {@link PdxInstance}, which Gemfire uses to store JSON
 * content (the type of 'newValue' for instance), to a JSON String.
 *
 *
 * @author David Turanski
 */

@Import(GeodeClientRegionConfiguration.class)
@Configuration
@PropertySource("classpath:geode-client.properties")
@EnableConfigurationProperties(GeodeCqSupplierProperties.class)
public class GeodeCqSupplierConfiguration {

	private EmitterProcessor<Message<?>> cqEvents = EmitterProcessor.create();

	@Bean
	public Supplier<Flux<?>> geodeCqSupplier() {
		return () -> cqEvents.map(Message::getPayload);
	}

	@Bean
	FluxMessageChannel fluxChannel() {
		FluxMessageChannel fluxChannel = new FluxMessageChannel();
		fluxChannel.subscribe(cqEvents);
		return fluxChannel;
	}

	@Bean
	MessageChannel convertToStringChannel() {
		return new DirectChannel();
	}

	@Bean
	MessageChannel routerChannel() {
		return new DirectChannel();
	}

	@Bean
	PayloadTypeRouter payloadTypeRouter(FluxMessageChannel fluxChannel) {
		PayloadTypeRouter router = new PayloadTypeRouter();
		router.setDefaultOutputChannel(fluxChannel);
		router.setChannelMapping(PdxInstance.class.getName(), "convertToStringChannel");

		return router;
	}

	@ConditionalOnMissingBean(Interest.class)
	@Bean
	public Interest<?> allKeysInterest() {
		return new Interest<>(Interest.ALL_KEYS);
	}

	@Bean
	IntegrationFlow startFlow(MessageChannel routerChannel, PayloadTypeRouter payloadTypeRouter) {
		return IntegrationFlows.from(routerChannel)
				.route(payloadTypeRouter)
				.get();
	}

	@Bean
	Function<PdxInstance, String> pdxToJson() {
		return JsonPdxFunctions.pdxToJson();
	}

	@Bean
	IntegrationFlow convertToString(MessageChannel convertToStringChannel, FluxMessageChannel fluxChannel,
			Function<PdxInstance, String> pdxToJson) {
		return IntegrationFlows.from(convertToStringChannel)
				.transform(pdxToJson)
				.channel(fluxChannel)
				.get();
	}

	@Bean
	ContinuousQueryListener continuousQueryListener(ContinuousQueryListenerContainer continuousQueryListenerContainer,
			GeodeCqSupplierProperties properties) {
		ContinuousQueryMessageProducer continuousQueryMessageProducer = new ContinuousQueryMessageProducer(
				continuousQueryListenerContainer,
				properties.getQuery());
		continuousQueryMessageProducer.setPayloadExpression(properties.getEventExpression());
		continuousQueryMessageProducer.setOutputChannel(routerChannel());
		return continuousQueryMessageProducer;
	}

	@Bean
	ContinuousQueryListenerContainer continuousQueryListenerContainer(ClientCache clientCache) {
		ContinuousQueryListenerContainer continuousQueryListenerContainer = new ContinuousQueryListenerContainer();
		try {
			continuousQueryListenerContainer.setCache(clientCache);
		}
		catch (Exception e) {
			throw new BeanCreationException(e.getLocalizedMessage(), e);
		}
		return continuousQueryListenerContainer;
	}
}
