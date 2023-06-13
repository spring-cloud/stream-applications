/*
 * Copyright 2017-2020 the original author or authors.
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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationMetadataDocumentationMojoTests {

	@Test
	public void testJavaTypeBeautifier() {
		ConfigurationMetadataDocumentationMojo mojo = new ConfigurationMetadataDocumentationMojo();
		String s = mojo.niceType("java.lang.String");
		assertThat(s).isEqualTo("String");

		s = mojo.niceType("java.lang.Class<?>");
		assertThat(s).isEqualTo("Class<?>");

		s = mojo.niceType("java.util.Map$Entry<java.lang.String,    java.util.Map<java.lang.Integer,java.util.List<java.lang.Long>>>");
		assertThat(s).isEqualTo("Entry<String, Map<Integer, List<Long>>>");
	}

}
