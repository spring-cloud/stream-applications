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

package org.springframework.cloud.stream.app.micrometer.common;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
@RunWith(Enclosed.class)
public class SpringCloudStreamMicrometerCommonTagsTest {

	public static class TestDefaultTagValues extends AbstractMicrometerTagTest {

		@Test
		public void testDefaultTagValues() {
			assertThat(meter.getId().getTag("stream.name")).isEqualTo("unknown");
			assertThat(meter.getId().getTag("application.name")).isEqualTo("unknown");
			assertThat(meter.getId().getTag("instance.index")).isEqualTo("0");
			assertThat(meter.getId().getTag("application.type")).isEqualTo("unknown");
			assertThat(meter.getId().getTag("application.guid")).isEqualTo("unknown");
		}
	}

	@TestPropertySource(properties = {
			"spring.cloud.dataflow.stream.name=myStream",
			"spring.cloud.dataflow.stream.app.label=myApp",
			"spring.cloud.stream.instanceIndex=666",
			"spring.cloud.application.guid=666guid",
			"spring.cloud.dataflow.stream.app.type=source"})
	public static class TestPresetTagValues extends AbstractMicrometerTagTest {

		@Test
		public void testPresetTagValues() {
			assertThat(meter.getId().getTag("stream.name")).isEqualTo("myStream");
			assertThat(meter.getId().getTag("application.name")).isEqualTo("myApp");
			assertThat(meter.getId().getTag("instance.index")).isEqualTo("666");
			assertThat(meter.getId().getTag("application.type")).isEqualTo("source");
			assertThat(meter.getId().getTag("application.guid")).isEqualTo("666guid");
		}
	}

	@TestPropertySource(properties = {"spring.cloud.stream.app.metrics.common.tags.enabled=false"})
	public static class TestDisabledTagValues extends AbstractMicrometerTagTest {

		@Test
		public void testDefaultTagValues() {
			assertThat(meter.getId().getTag("stream.name")).isNull();
			assertThat(meter.getId().getTag("application.name")).isNull();
			assertThat(meter.getId().getTag("instance.index")).isNull();
			assertThat(meter.getId().getTag("application.type")).isNull();
			assertThat(meter.getId().getTag("application.guid")).isNull();
		}
	}
}
