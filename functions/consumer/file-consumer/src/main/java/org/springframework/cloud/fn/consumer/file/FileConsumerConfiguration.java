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

package org.springframework.cloud.fn.consumer.file;

import java.util.function.Consumer;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Soby Chacko
 */
@Configuration
@EnableConfigurationProperties(FileConsumerProperties.class)
public class FileConsumerConfiguration {

	private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

	private final FileConsumerProperties properties;

	public FileConsumerConfiguration(FileConsumerProperties properties) {
		this.properties = properties;
	}

	@Bean
	public Consumer<Message<?>> fileConsumer() {
		return fileWritingMessageHandler()::handleMessage;
	}

	@Bean
	public FileWritingMessageHandler fileWritingMessageHandler() {
		FileWritingMessageHandler handler = (properties.getDirectoryExpression() != null)
				? new FileWritingMessageHandler(EXPRESSION_PARSER.parseExpression(properties.getDirectoryExpression()))
				: new FileWritingMessageHandler(properties.getDirectory());
		handler.setAutoCreateDirectory(true);
		handler.setAppendNewLine(!properties.isBinary());
		handler.setCharset(properties.getCharset());
		handler.setExpectReply(false);
		handler.setFileExistsMode(properties.getMode());
		handler.setFileNameGenerator(fileNameGenerator());
		return handler;
	}

	@Bean
	public FileNameGenerator fileNameGenerator() {
		DefaultFileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();
		fileNameGenerator.setExpression(properties.getNameExpression());
		return fileNameGenerator;
	}
}
