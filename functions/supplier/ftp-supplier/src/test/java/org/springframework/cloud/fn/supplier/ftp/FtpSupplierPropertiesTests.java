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

package org.springframework.cloud.fn.supplier.ftp;

import java.io.File;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Turanski
 * @author Gary Russell
 * @author Artem Bilan
 */
public class FtpSupplierPropertiesTests {

	@Test
	public void localDirCanBeCustomized() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("ftp.supplier.localDir:local")
				.applyTo(context);
		context.register(Conf.class);
		context.refresh();
		FtpSupplierProperties properties = context.getBean(FtpSupplierProperties.class);
		assertThat(properties.getLocalDir()).isEqualTo(new File("local"));
		context.close();
	}

	@Test
	public void remoteDirCanBeCustomized() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("ftp.supplier.remoteDir:/remote")
				.applyTo(context);
		context.register(Conf.class);
		context.refresh();
		FtpSupplierProperties properties = context.getBean(FtpSupplierProperties.class);
		assertThat(properties.getRemoteDir()).isEqualTo("/remote");
		context.close();
	}

	@Test
	public void deleteRemoteFilesCanBeEnabled() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("ftp.supplier.deleteRemoteFiles:true")
				.applyTo(context);
		context.register(Conf.class);
		context.refresh();
		FtpSupplierProperties properties = context.getBean(FtpSupplierProperties.class);
		assertThat(properties.isDeleteRemoteFiles()).isTrue();
		context.close();
	}

	@Test
	public void autoCreateLocalDirCanBeDisabled() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("ftp.supplier.autoCreateLocalDir:false")
				.applyTo(context);
		context.register(Conf.class);
		context.refresh();
		FtpSupplierProperties properties = context.getBean(FtpSupplierProperties.class);
		assertThat(!properties.isAutoCreateLocalDir()).isTrue();
		context.close();
	}

	@Test
	public void tmpFileSuffixCanBeCustomized() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("ftp.supplier.tmpFileSuffix:.foo")
				.applyTo(context);
		context.register(Conf.class);
		context.refresh();
		FtpSupplierProperties properties = context.getBean(FtpSupplierProperties.class);
		assertThat(properties.getTmpFileSuffix()).isEqualTo(".foo");
		context.close();
	}

	@Test
	public void filenamePatternCanBeCustomized() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("ftp.supplier.filenamePattern:*.foo")
				.applyTo(context);
		context.register(Conf.class);
		context.refresh();
		FtpSupplierProperties properties = context.getBean(FtpSupplierProperties.class);
		assertThat(properties.getFilenamePattern()).isEqualTo("*.foo");
		context.close();
	}

	@Test
	public void remoteFileSeparatorCanBeCustomized() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("ftp.supplier.remoteFileSeparator:\\")
				.applyTo(context);
		context.register(Conf.class);
		context.refresh();
		FtpSupplierProperties properties = context.getBean(FtpSupplierProperties.class);
		assertThat(properties.getRemoteFileSeparator()).isEqualTo("\\");
		context.close();
	}


	@Test
	public void preserveTimestampDirCanBeDisabled() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("ftp.supplier.preserveTimestamp:false")
				.applyTo(context);
		context.register(Conf.class);
		context.refresh();
		FtpSupplierProperties properties = context.getBean(FtpSupplierProperties.class);
		assertThat(!properties.isPreserveTimestamp()).isTrue();
		context.close();
	}

	@Configuration
	@EnableConfigurationProperties(FtpSupplierProperties.class)
	static class Conf {

	}
}
