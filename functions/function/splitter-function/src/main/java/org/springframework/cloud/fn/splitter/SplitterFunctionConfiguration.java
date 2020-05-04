/*
 * Copyright (c) 2011-2020 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.fn.splitter;

import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.ReactiveStreamsSubscribableChannel;
import org.springframework.integration.file.splitter.FileSplitter;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.integration.splitter.DefaultMessageSplitter;
import org.springframework.integration.splitter.ExpressionEvaluatingSplitter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import reactor.core.publisher.Flux;

@Configuration
@EnableConfigurationProperties(SplitterFunctionProperties.class)
public class SplitterFunctionConfiguration {

	@Bean
	public Function<Message<?>, List<Message<?>>> splitterFunction(AbstractMessageSplitter messageSplitter,
			SplitterFunctionProperties splitterFunctionProperties) {

		messageSplitter.setApplySequence(splitterFunctionProperties.isApplySequence());
		ThreadLocalFluxSinkMessageChannel outputChannel = new ThreadLocalFluxSinkMessageChannel();
		messageSplitter.setOutputChannel(outputChannel);
		return message -> {
			messageSplitter.handleMessage(message);
			return outputChannel.publisherThreadLocal.get();
		};
	}

	@Bean
	@ConditionalOnProperty(prefix = "splitter", name = "expression")
	public AbstractMessageSplitter expressionSplitter(SplitterFunctionProperties splitterFunctionProperties) {
		return new ExpressionEvaluatingSplitter(
				new SpelExpressionParser()
						.parseExpression(splitterFunctionProperties.getExpression()));
	}

	@Bean
	@ConditionalOnMissingBean
	@Conditional(FileSplitterCondition.class)
	public AbstractMessageSplitter fileSplitter(SplitterFunctionProperties splitterFunctionProperties) {
		Boolean markers = splitterFunctionProperties.getFileMarkers();
		String charset = splitterFunctionProperties.getCharset();
		if (markers == null) {
			markers = false;
		}
		FileSplitter fileSplitter = new FileSplitter(true, markers, splitterFunctionProperties.getMarkersJson());
		if (charset != null) {
			fileSplitter.setCharset(Charset.forName(charset));
		}
		return fileSplitter;
	}


	@Bean
	@ConditionalOnMissingBean
	public AbstractMessageSplitter defaultSplitter(SplitterFunctionProperties splitterFunctionProperties) {
		DefaultMessageSplitter defaultMessageSplitter = new DefaultMessageSplitter();
		defaultMessageSplitter.setDelimiters(splitterFunctionProperties.getDelimiters());
		return defaultMessageSplitter;
	}

	static class FileSplitterCondition extends AnyNestedCondition {

		FileSplitterCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty(prefix = "splitter", name = "charset")
		static class Charset { }

		@ConditionalOnProperty(prefix = "splitter", name = "fileMarkers")
		static class FileMarkers { }

	}

	private static final class ThreadLocalFluxSinkMessageChannel
			implements MessageChannel, ReactiveStreamsSubscribableChannel {

		private final ThreadLocal<List<Message<?>>> publisherThreadLocal = new ThreadLocal<>();

		@Override
		@SuppressWarnings("unchecked")
		public void subscribeTo(Publisher<? extends Message<?>> publisher) {
			this.publisherThreadLocal.set(Flux.from(publisher).collectList().cast(List.class).block());
		}

		@Override
		public boolean send(Message<?> message, long l) {
			throw new UnsupportedOperationException("This channel only supports a reactive 'subscribeTo()' ");
		}

	}

}
