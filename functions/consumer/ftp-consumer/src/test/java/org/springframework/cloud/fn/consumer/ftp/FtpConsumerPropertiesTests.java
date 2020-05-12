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

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.file.support.FileExistsMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Turanski
 * @author Gary Russell
 * @author Artem Bilan
 */
public class FtpConsumerPropertiesTests {

	@Test
	public void remoteDirCanBeCustomized() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("ftp.consumer.remoteDir:/remote")
				.applyTo(context);
		context.register(Conf.class);
		context.refresh();
		FtpConsumerProperties properties = context.getBean(FtpConsumerProperties.class);
		assertThat(properties.getRemoteDir()).isEqualTo("/remote");
		context.close();
	}

	@Test
	public void autoCreateDirCanBeDisabled() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("ftp.consumer.autoCreateDir:false")
				.applyTo(context);
		context.register(Conf.class);
		context.refresh();
		FtpConsumerProperties properties = context.getBean(FtpConsumerProperties.class);
		assertThat(!properties.isAutoCreateDir()).isTrue();
		context.close();
	}

	@Test
	public void tmpFileSuffixCanBeCustomized() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("ftp.consumer.tmpFileSuffix:.foo")
				.applyTo(context);
		context.register(Conf.class);
		context.refresh();
		FtpConsumerProperties properties = context.getBean(FtpConsumerProperties.class);
		assertThat(properties.getTmpFileSuffix()).isEqualTo(".foo");
		context.close();
	}

	@Test
	public void tmpFileRemoteDirCanBeCustomized() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("ftp.consumer.temporaryRemoteDir:/foo")
				.applyTo(context);
		context.register(Conf.class);
		context.refresh();
		FtpConsumerProperties properties = context.getBean(FtpConsumerProperties.class);
		assertThat(properties.getTemporaryRemoteDir()).isEqualTo("/foo");
		context.close();
	}

	@Test
	public void remoteFileSeparatorCanBeCustomized() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("ftp.consumer.remoteFileSeparator:\\")
				.applyTo(context);
		context.register(Conf.class);
		context.refresh();
		FtpConsumerProperties properties = context.getBean(FtpConsumerProperties.class);
		assertThat(properties.getRemoteFileSeparator()).isEqualTo("\\");
		context.close();
	}

	@Test
	public void useTemporaryFileNameCanBeCustomized() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("ftp.consumer.useTemporaryFilename:false")
				.applyTo(context);
		context.register(Conf.class);
		context.refresh();
		FtpConsumerProperties properties = context.getBean(FtpConsumerProperties.class);
		assertThat(properties.isUseTemporaryFilename()).isFalse();
		context.close();
	}

	@Test
	public void fileExistsModeCanBeCustomized() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("ftp.consumer.mode:FAIL")
				.applyTo(context);
		context.register(Conf.class);
		context.refresh();
		FtpConsumerProperties properties = context.getBean(FtpConsumerProperties.class);
		assertThat(properties.getMode()).isEqualTo(FileExistsMode.FAIL);
		context.close();
	}

	@Configuration
	@EnableConfigurationProperties(FtpConsumerProperties.class)
	static class Conf {

	}
}
