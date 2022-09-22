/*
 * Copyright 2020-2022 the original author or authors.
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

package org.springframework.cloud.fn.supplier.mail;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import jakarta.mail.URLName;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.config.ComponentCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.integration.dsl.MessageSourceSpec;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.endpoint.ReactiveMessageSourceProducer;
import org.springframework.integration.mail.MailHeaders;
import org.springframework.integration.mail.dsl.ImapIdleChannelAdapterSpec;
import org.springframework.integration.mail.dsl.Mail;
import org.springframework.integration.mail.dsl.MailInboundChannelAdapterSpec;
import org.springframework.integration.transformer.support.AbstractHeaderValueMessageProcessor;
import org.springframework.integration.transformer.support.HeaderValueMessageProcessor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * Mail supplier components.
 *
 * @author Amol
 * @author Artem Bilan
 * @author Chris Schaefer
 * @author Soby Chacko
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MailSupplierProperties.class)
public class MailSupplierConfiguration {

	@Autowired
	private MailSupplierProperties properties;

	@Bean
	public Publisher<Message<Object>> mailInboundFlow(MessageProducerSupport messageProducer) {

		return IntegrationFlows.from(messageProducer)
				.transform(Mail.toStringTransformer(this.properties.getCharset()))
				.enrichHeaders(h -> h
						.defaultOverwrite(true)
						.header(MailHeaders.TO, arrayToListProcessor(MailHeaders.TO))
						.header(MailHeaders.CC, arrayToListProcessor(MailHeaders.CC))
						.header(MailHeaders.BCC, arrayToListProcessor(MailHeaders.BCC)))
				.toReactivePublisher(true);
	}

	@Bean
	public Supplier<Flux<Message<?>>> mailSupplier(Publisher<Message<Object>> messagePublisher) {
		return () -> Flux.from(messagePublisher);
	}

	private HeaderValueMessageProcessor<?> arrayToListProcessor(final String header) {
		return new AbstractHeaderValueMessageProcessor<List<String>>() {

			@Override
			public List<String> processMessage(Message<?> message) {
				return Arrays.asList(message.getHeaders().get(header, String[].class));
			}

		};
	}

	@Bean("mailChannelAdapter")
	@ConditionalOnProperty("mail.supplier.idle-imap")
	MessageProducerSpec<?, ?> imapIdleProducer(
			@Nullable ComponentCustomizer<ImapIdleChannelAdapterSpec> imapIdleChannelAdapterSpecCustomizer) {

		URLName urlName = this.properties.getUrl();
		ImapIdleChannelAdapterSpec imapIdleChannelAdapterSpec = Mail.imapIdleAdapter(urlName.toString())
				.shouldDeleteMessages(this.properties.isDelete())
				.userFlag(this.properties.getUserFlag())
				.javaMailProperties(getJavaMailProperties(urlName))
				.selectorExpression(this.properties.getExpression())
				.shouldMarkMessagesAsRead(this.properties.isMarkAsRead());

		if (imapIdleChannelAdapterSpecCustomizer != null) {
			imapIdleChannelAdapterSpecCustomizer.customize(imapIdleChannelAdapterSpec);
		}
		return imapIdleChannelAdapterSpec;
	}

	@Bean
	@ConditionalOnProperty(value = "mail.supplier.idle-imap", matchIfMissing = true, havingValue = "false")
	MessageSourceSpec<?, ?> mailMessageSource(
			@Nullable ComponentCustomizer<MailInboundChannelAdapterSpec<?, ?>>
					mailInboundChannelAdapterSpecCustomizer) {

		MailInboundChannelAdapterSpec<?, ?> adapterSpec;
		URLName urlName = this.properties.getUrl();
		switch (urlName.getProtocol().toUpperCase()) {
			case "IMAP":
			case "IMAPS":
				adapterSpec = getImapChannelAdapterSpec(urlName);
				break;
			case "POP3":
			case "POP3S":
				adapterSpec = getPop3ChannelAdapterSpec(urlName);
				break;
			default:
				throw new IllegalArgumentException(
						"Unsupported mail protocol: " + urlName.getProtocol());
		}
		adapterSpec.javaMailProperties(getJavaMailProperties(urlName))
				.userFlag(this.properties.getUserFlag())
				.selectorExpression(this.properties.getExpression())
				.shouldDeleteMessages(this.properties.isDelete());

		if (mailInboundChannelAdapterSpecCustomizer != null) {
			mailInboundChannelAdapterSpecCustomizer.customize(adapterSpec);
		}

		return adapterSpec;
	}

	@Bean("mailChannelAdapter")
	@ConditionalOnProperty(value = "mail.supplier.idle-imap", matchIfMissing = true, havingValue = "false")
	MessageProducerSupport mailMessageProducer(MessageSource<?> mailMessageSource) {
		return new ReactiveMessageSourceProducer(mailMessageSource);
	}

	/**
	 * Method to build Mail Channel Adapter for POP3.
	 * @param urlName Mail source URL.
	 * @return Mail Channel for POP3
	 */
	@SuppressWarnings("rawtypes")
	private MailInboundChannelAdapterSpec getPop3ChannelAdapterSpec(URLName urlName) {
		return Mail.pop3InboundAdapter(urlName.toString());
	}

	/**
	 * Method to build Mail Channel Adapter for IMAP.
	 * @param urlName Mail source URL.
	 * @return Mail Channel for IMAP
	 */
	@SuppressWarnings("rawtypes")
	private MailInboundChannelAdapterSpec getImapChannelAdapterSpec(URLName urlName) {
		return Mail.imapInboundAdapter(urlName.toString())
				.shouldMarkMessagesAsRead(this.properties.isMarkAsRead());
	}

	private Properties getJavaMailProperties(URLName urlName) {
		Properties javaMailProperties = new Properties();

		switch (urlName.getProtocol().toUpperCase()) {
			case "IMAP":
				javaMailProperties.setProperty("mail.imap.socketFactory.class", "javax.net.SocketFactory");
				javaMailProperties.setProperty("mail.imap.socketFactory.fallback", "false");
				javaMailProperties.setProperty("mail.store.protocol", "imap");
				break;

			case "IMAPS":
				javaMailProperties.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
				javaMailProperties.setProperty("mail.imap.socketFactory.fallback", "false");
				javaMailProperties.setProperty("mail.store.protocol", "imaps");
				break;

			case "POP3":
				javaMailProperties.setProperty("mail.pop3.socketFactory.class", "javax.net.SocketFactory");
				javaMailProperties.setProperty("mail.pop3.socketFactory.fallback", "false");
				javaMailProperties.setProperty("mail.store.protocol", "pop3");
				break;

			case "POP3S":
				javaMailProperties.setProperty("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
				javaMailProperties.setProperty("mail.pop3.socketFactory.fallback", "false");
				javaMailProperties.setProperty("mail.store.protocol", "pop3s");
				break;
		}

		javaMailProperties.putAll(this.properties.getJavaMailProperties());
		return javaMailProperties;
	}

}
