/*
 * Copyright 2018-2021 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
@ActiveProfiles("cloud")
public class CloudFoundryMicrometerActiveCloudProfileDefaultValues extends AbstractMicrometerTagTest {
	@Test
	public void testDefaultTagValues() {
		assertThat(meter.getId().getTag("cf.org.name")).isEqualTo("default");
		assertThat(meter.getId().getTag("cf.space.id")).isEqualTo("unknown");
		assertThat(meter.getId().getTag("cf.space.name")).isEqualTo("unknown");
		assertThat(meter.getId().getTag("cf.app.name")).isEqualTo("unknown");
		assertThat(meter.getId().getTag("cf.app.id")).isEqualTo("unknown");
		assertThat(meter.getId().getTag("cf.app.version")).isEqualTo("unknown");
		assertThat(meter.getId().getTag("cf.instance.index")).isEqualTo("0");
	}
}
