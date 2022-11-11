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

package org.springframework.cloud.stream.app.source.xmpp;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.fn.common.xmpp.XmppConnectionFactoryProperties;
import org.springframework.cloud.fn.supplier.xmpp.XmppSupplierProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for XmppSource with invalid config.
 *
 * @author Daniel Frey
 * @since 4.0.0
 */
public class XmppSourceInvalidConfigTests {

	@ParameterizedTest
	@ValueSource(strings = { "host", "user", "password" })
	void requiredXmppConnectionFactoryPropertyIsSetEmpty(String propertyName) {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> {
					AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
					TestPropertyValues.of(String.format("xmpp.factory.%s: ", propertyName)).applyTo(context);
					context.register(Config.class);
					context.refresh();
				})
				.withMessageContaining("Error creating bean with name 'xmpp.factory-org.springframework.cloud.fn.common.xmpp.XmppConnectionFactoryProperties': Could not bind properties to 'XmppConnectionFactoryProperties'")
				.havingRootCause()
				.withMessageContaining("Binding validation errors on xmpp.factory")
				.withMessageContaining("Field error in object 'xmpp.factory' on field '%s': rejected value []", propertyName);
	}

	@Configuration
	@EnableConfigurationProperties({XmppConnectionFactoryProperties.class, XmppSupplierProperties.class})
	static class Config {
	}

}
