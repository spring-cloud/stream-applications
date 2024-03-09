/*
 * Copyright 2016-2024 the original author or authors.
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
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.router.AbstractMappingMessageRouter;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.router.ExpressionEvaluatingRouter;
import org.springframework.integration.router.MethodInvokingRouter;
import org.springframework.integration.scripting.dsl.ScriptSpec;
import org.springframework.integration.scripting.dsl.Scripts;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * A sink app that routes to one or more named channels.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Christian Tzolov
 * @author Soby Chacko
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RouterSinkProperties.class)
public class RouterSinkConfiguration {

	private final RouterSinkProperties properties;

	public RouterSinkConfiguration(RouterSinkProperties properties) {
		this.properties = properties;
	}

	@Bean
	public Consumer<Message<?>> routerSinkConsumer(AbstractMessageRouter router) {
		return router::handleMessage;
	}

	@Bean
	public AbstractMessageRouter router(BindingService bindingService, StreamBridge streamBridge,
			@Nullable MessageProcessor<?> scriptProcessor) {

		AbstractMappingMessageRouter router;
		if (scriptProcessor != null) {
			router = new MethodInvokingRouter(scriptProcessor);
		}
		else {
			router = new ExpressionEvaluatingRouter(this.properties.getExpression());
		}
		String defaultOutputBinding = this.properties.getDefaultOutputBinding();
		if (StringUtils.hasText(defaultOutputBinding)) {
			router.setDefaultOutputChannelName(defaultOutputBinding);
		}
		router.setResolutionRequired(this.properties.isResolutionRequired());
		Properties destinationMappings = this.properties.getDestinationMappings();
		if (destinationMappings != null) {
			router.replaceChannelMappings(destinationMappings);
		}

		router.setChannelResolver(
				new BindingChannelResolver(bindingService, streamBridge, this.properties.isResolutionRequired()));
		return router;
	}

	@Bean
	@ConditionalOnProperty("router.script")
	public ScriptSpec scriptProcessor() {
		return Scripts.processor(this.properties.getScript())
				.lang("groovy")
				.refreshCheckDelay(this.properties.getRefreshDelay())
				.variables(obtainScriptVariables(this.properties));
	}

	private static Map<String, Object> obtainScriptVariables(RouterSinkProperties properties) {
		Map<String, Object> variables = new HashMap<>();
		CollectionUtils.mergePropertiesIntoMap(properties.getVariables(), variables);
		Resource variablesLocation = properties.getVariablesLocation();
		if (variablesLocation != null) {
			try {
				Properties props = PropertiesLoaderUtils.loadProperties(properties.getVariablesLocation());
				CollectionUtils.mergePropertiesIntoMap(props, variables);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}
		return variables;
	}

}
