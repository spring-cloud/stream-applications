/*
 * Copyright 2018-2020 the original author or authors.
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

import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.dataflow.app.plugin.MetadataAggregationMojo.CONFIGURATION_PROPERTIES_CLASSES;
import static org.springframework.cloud.dataflow.app.plugin.MetadataAggregationMojo.CONFIGURATION_PROPERTIES_NAMES;

/**
 * @author David Turanski
 **/

public class MergeWhitelistPropertiesTest {

	@Test
	public void merge() throws IOException {
		Properties properties1 = new Properties();
		properties1.load(new ClassPathResource("META-INF/whitelist-1.properties").getInputStream());

		Properties properties2 = new MetadataAggregationMojo().merge(properties1,
			new ClassPathResource("META-INF/whitelist-2.properties").getInputStream());

		assertThat(properties2).containsKeys(CONFIGURATION_PROPERTIES_CLASSES, CONFIGURATION_PROPERTIES_NAMES);
	}

	@Test
	public void mergeReverseOrder() throws IOException {
		Properties properties1 = new Properties();
		properties1.load(new ClassPathResource("META-INF/whitelist-2.properties").getInputStream());

		Properties properties2 = new MetadataAggregationMojo().merge(properties1,
			new ClassPathResource("META-INF/whitelist-1.properties").getInputStream());

		assertThat(properties2).containsKeys(CONFIGURATION_PROPERTIES_CLASSES, CONFIGURATION_PROPERTIES_NAMES);
	}

}
