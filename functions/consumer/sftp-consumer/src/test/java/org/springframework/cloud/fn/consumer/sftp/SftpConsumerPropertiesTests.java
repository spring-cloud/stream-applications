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

package org.springframework.cloud.fn.consumer.sftp;

import java.io.File;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.IntegrationConverter;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.test.util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Turanski
 * @author Gary Russell
 * @author Artem Bilan
 * @author Chris Schaefer
 */
public class SftpConsumerPropertiesTests {

	@Test
	public void remoteDirCanBeCustomized() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		testPropertyValues(context, "sftp.consumer.remoteDir:/remote");
		context.register(Conf.class);
		context.refresh();
		SftpConsumerProperties properties = context.getBean(SftpConsumerProperties.class);
		assertThat(properties.getRemoteDir()).isEqualTo("/remote");
		context.close();
	}

	@Test
	public void autoCreateDirCanBeDisabled() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		testPropertyValues(context, "sftp.consumer.autoCreateDir:false");
		context.register(Conf.class);
		context.refresh();
		SftpConsumerProperties properties = context.getBean(SftpConsumerProperties.class);
		assertThat(!properties.isAutoCreateDir()).isTrue();
		context.close();
	}

	@Test
	public void tmpFileSuffixCanBeCustomized() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		testPropertyValues(context, "sftp.consumer.tmpFileSuffix:.foo");
		context.register(Conf.class);
		context.refresh();
		SftpConsumerProperties properties = context.getBean(SftpConsumerProperties.class);
		assertThat(properties.getTmpFileSuffix()).isEqualTo(".foo");
		context.close();
	}

	@Test
	public void tmpFileRemoteDirCanBeCustomized() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		testPropertyValues(context, "sftp.consumer.temporaryRemoteDir:/foo");
		context.register(Conf.class);
		context.refresh();
		SftpConsumerProperties properties = context.getBean(SftpConsumerProperties.class);
		assertThat(properties.getTemporaryRemoteDir()).isEqualTo("/foo");
		context.close();
	}

	@Test
	public void remoteFileSeparatorCanBeCustomized() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		testPropertyValues(context, "sftp.consumer.remoteFileSeparator:\\");
		context.register(Conf.class);
		context.refresh();
		SftpConsumerProperties properties = context.getBean(SftpConsumerProperties.class);
		assertThat(properties.getRemoteFileSeparator()).isEqualTo("\\");
		context.close();
	}

	@Test
	public void useTemporaryFileNameCanBeCustomized() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		testPropertyValues(context, "sftp.consumer.useTemporaryFilename:false");
		context.register(Conf.class);
		context.refresh();
		SftpConsumerProperties properties = context.getBean(SftpConsumerProperties.class);
		assertThat(properties.isUseTemporaryFilename()).isFalse();
		context.close();
	}

	@Test
	public void fileExistsModeCanBeCustomized() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		testPropertyValues(context, "sftp.consumer.mode:FAIL");
		context.register(Conf.class);
		context.refresh();
		SftpConsumerProperties properties = context.getBean(SftpConsumerProperties.class);
		assertThat(properties.getMode()).isEqualTo(FileExistsMode.FAIL);
		context.close();
	}

	@Test
	public void knownHostsExpression() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		testPropertyValues(context,
				"sftp.consumer.factory.known-hosts-expression = @systemProperties[\"user.home\"] + \"/.ssh/known_hosts\"",
				"sftp.consumer.factory.cache-sessions = true");
		context.register(Factory.class);
		context.refresh();
		SessionFactory<?> sessionFactory = context.getBean(SessionFactory.class);
		assertThat(TestUtils.getPropertyValue(sessionFactory, "sessionFactory.knownHosts")
				.toString().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/")
				.endsWith("/.ssh/known_hosts]")).isTrue();
		context.close();
	}

	private void testPropertyValues(ConfigurableApplicationContext context, String... props) {
		TestPropertyValues.of("sftp.consumer.factory.username=foo").and(props).applyTo(context);
	}

	@Configuration
	@EnableConfigurationProperties(SftpConsumerProperties.class)
	static class Conf {

	}

	@Configuration
	@EnableConfigurationProperties(SftpConsumerProperties.class)
	@EnableIntegration
	@Import(SftpConsumerSessionFactoryConfiguration.class)
	static class Factory {

		@Bean
		@ConfigurationPropertiesBinding
		@IntegrationConverter
		public Converter<String, Expression> spelConverter() {
			return new SpelConverter();
		}

		/**
		 * TODO: This needs to be refactored into a generic place for any functions to use.
		 *
		 * A simple converter from String to Expression.
		 *
		 * @author Eric Bottard
		 */
		public static class SpelConverter implements Converter<String, Expression> {

			private SpelExpressionParser parser = new SpelExpressionParser();

			@Autowired
			@Qualifier(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME)
			@Lazy
			private EvaluationContext evaluationContext;

			@Override
			public Expression convert(String source) {
				try {
					Expression expression = this.parser.parseExpression(source);
					if (expression instanceof SpelExpression) {
						((SpelExpression) expression)
								.setEvaluationContext(this.evaluationContext);
					}
					return expression;
				}
				catch (ParseException e) {
					throw new IllegalArgumentException(String.format(
							"Could not convert '%s' into a SpEL expression", source), e);
				}
			}

		}

	}

}
