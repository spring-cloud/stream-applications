/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.stream.app.source.cdc;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.cloud.fn.supplier.cdc.CdcSupplierConfiguration;
import org.springframework.cloud.stream.binder.kafka.KafkaNullConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MessageConverter;

/**
 * @author Christian Tzolov
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = MongoAutoConfiguration.class)
@Import(CdcSupplierConfiguration.class)
public class TestCdcSourceApplication {

	/**
	 * This is only required for testing. In production the KafkaNullConverter bean is auto-configured.
	 */
	@Bean
	@ConditionalOnMissingBean(KafkaNullConverter.class)
	MessageConverter kafkaNullConverter() {
		return new KafkaNullConverter();
	}
}
