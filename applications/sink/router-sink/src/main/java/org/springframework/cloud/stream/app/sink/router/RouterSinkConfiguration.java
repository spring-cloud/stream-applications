/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.stream.app.sink.router;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.groovy.GroovyScriptExecutingMessageProcessor;
import org.springframework.integration.router.AbstractMappingMessageRouter;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.router.ExpressionEvaluatingRouter;
import org.springframework.integration.router.MessageRouter;
import org.springframework.integration.router.MethodInvokingRouter;
import org.springframework.integration.scripting.DefaultScriptVariableGenerator;
import org.springframework.integration.scripting.RefreshableResourceScriptSource;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.CollectionUtils;

/**
 * A sink app that routes to one or more named channels.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Christian Tzolov
 * @author Soby Chacko
 */
@Configuration
@EnableConfigurationProperties(RouterSinkProperties.class)
public class RouterSinkConfiguration {

	@Autowired
	RouterSinkProperties properties;

	@Bean
	public Consumer<Message<?>> routerSinkConsumer(MessageRouter router) {
		return ((AbstractMessageRouter) router)::handleMessage;
	}

	@Bean
	public MessageRouter router(BinderAwareChannelResolver channelResolver,
								ScriptVariableGenerator scriptVariableGenerator) {
		AbstractMappingMessageRouter router;
		if (properties.getScript() != null) {
			router = new MethodInvokingRouter(scriptProcessor(scriptVariableGenerator, properties));
		}
		else {
			router = new ExpressionEvaluatingRouter(properties.getExpression());
		}
		router.setDefaultOutputChannelName(properties.getDefaultOutputChannel());
		router.setResolutionRequired(properties.isResolutionRequired());
		if (properties.getDestinationMappings() != null) {
			router.replaceChannelMappings(properties.getDestinationMappings());
		}
		router.setChannelResolver(channelResolver);
		return router;
	}

	// Router sink receives the input as String through byteArrayTextToString|routerSinkConsumer.
	// Spring Cloud Stream used to convert the string back to byte[], but the commit below removed that logic.
	// https://github.com/spring-cloud/spring-cloud-stream/commit/5d9de8ad579d3464d1503d1a5d1390168bccbdb9
	// Therefore we are adding it back in the router sink app by programmatically converting the String back to
	// byte[] before sending it out to the bound router channel.
	@Bean
	public BinderAwareChannelResolver.NewDestinationBindingCallback newDestinationBindingCallback(CompositeMessageConverter messageConverter) {
		return new BinderAwareChannelResolver.NewDestinationBindingCallback() {
			@Override
			public void configure(String channelName, MessageChannel channel, ProducerProperties producerProperties, Object extendedProducerProperties) {
				((AbstractMessageChannel) channel).addInterceptor(new ChannelInterceptor() {
					@Override
					public Message<?> preSend(Message<?> message, MessageChannel channel) {
						@SuppressWarnings("unchecked")
						Message<byte[]> outboundMessage = message.getPayload() instanceof byte[]
								? (Message<byte[]>) message : (Message<byte[]>) messageConverter
								.toMessage(message.getPayload(), message.getHeaders());
						if (outboundMessage == null) {
							throw new IllegalStateException("Failed to convert message: '" + message
									+ "' to outbound message.");
						}
						return outboundMessage;
					}
				});
			}
		};
	}

	@Bean(name = "variableGenerator")
	public ScriptVariableGenerator scriptVariableGenerator() throws IOException {
		Map<String, Object> variables = new HashMap<>();
		CollectionUtils.mergePropertiesIntoMap(properties.getVariables(), variables);
		if (properties.getVariablesLocation() != null) {
			CollectionUtils.mergePropertiesIntoMap(
					PropertiesLoaderUtils.loadProperties(properties.getVariablesLocation()), variables);
		}
		return new DefaultScriptVariableGenerator(variables);
	}

	@Bean
	@ConditionalOnProperty("router.script")
	public GroovyScriptExecutingMessageProcessor scriptProcessor(ScriptVariableGenerator scriptVariableGenerator,
																RouterSinkProperties properties) {
		ScriptSource scriptSource = new RefreshableResourceScriptSource(properties.getScript(),
				properties.getRefreshDelay());
		return new GroovyScriptExecutingMessageProcessor(scriptSource, scriptVariableGenerator);
	}

}
