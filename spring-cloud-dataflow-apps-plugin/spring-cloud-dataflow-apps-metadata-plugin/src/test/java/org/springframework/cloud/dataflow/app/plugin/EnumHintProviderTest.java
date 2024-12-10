/*
 * Copyright 2018-2024 the original author or authors.
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

package org.springframework.cloud.dataflow.app.plugin;

import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemHint;
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller;
import org.springframework.cloud.dataflow.completion.Expresso;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Turanski
 **/
class EnumHintProviderTest {

	@Test
	void enumHintsAdded() throws Exception {
		JsonMarshaller jsonMarshaller = new JsonMarshaller();
		ConfigurationMetadata configurationMetadata = jsonMarshaller.read(new ClassPathResource("META-INF/spring"
			+ "-configuration-metadata.json").getInputStream());
		new MetadataAggregationMojo().addEnumHints(configurationMetadata, this.getClass().getClassLoader());
		assertThat(configurationMetadata.getHints()).hasSize(1);
		ItemHint itemHint = configurationMetadata.getHints().get(0);
		// equals fails on ValueHint so ...
		// assertThat(new ItemHint.ValueHint("SINGLE",null)).isEqualTo(new ItemHint.ValueHint("SINGLE",null));
		assertThat(itemHint.getValues()).hasSize(2);
		assertThat(itemHint.getValues().get(0).getValue()).isEqualTo(Expresso.SINGLE);
		assertThat(itemHint.getValues().get(1).getValue()).isEqualTo(Expresso.DOUBLE);
	}
}
