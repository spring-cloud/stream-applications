/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.cloud.fn.consumer.ftp;

import java.util.function.Consumer;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.ftp.FtpSessionFactoryConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.dsl.Ftp;
import org.springframework.integration.ftp.dsl.FtpMessageHandlerSpec;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import org.springframework.messaging.Message;

@Configuration
@EnableConfigurationProperties(FtpConsumerProperties.class)
@Import(FtpSessionFactoryConfiguration.class)
public class FtpConsumerConfiguration {

	private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

	@Autowired
	FtpConsumerProperties ftpConsumerProperties;

	@Bean
	public IntegrationFlow ftpInboundFlow(FtpConsumerProperties properties, SessionFactory<FTPFile> ftpSessionFactory) {

		IntegrationFlowBuilder integrationFlowBuilder =
				IntegrationFlows.from(MessageConsumer.class, (gateway) -> gateway.beanName("ftpConsumer"));

		FtpMessageHandlerSpec handlerSpec =
				Ftp.outboundAdapter(new FtpRemoteFileTemplate(ftpSessionFactory), properties.getMode())
						.remoteDirectory(properties.getRemoteDir())
						.remoteFileSeparator(properties.getRemoteFileSeparator())
						.autoCreateDirectory(properties.isAutoCreateDir())
						.temporaryFileSuffix(properties.getTmpFileSuffix());
		if (properties.getFilenameExpression() != null) {
			handlerSpec.fileNameExpression(EXPRESSION_PARSER.parseExpression(properties.getFilenameExpression()).getExpressionString());
		}
		return integrationFlowBuilder
				.handle(handlerSpec)
				.get();
	}

	private interface MessageConsumer extends Consumer<Message<?>> {

	}

}
