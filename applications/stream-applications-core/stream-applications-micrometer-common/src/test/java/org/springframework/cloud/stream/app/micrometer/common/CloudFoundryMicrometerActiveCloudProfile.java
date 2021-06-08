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
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
@TestPropertySource(properties = {
		"vcap.application.org_name=PivotalOrg",
		"vcap.application.space_id=SpringSpaceId",
		"vcap.application.space_name=SpringSpace",
		"vcap.application.application_name=App666",
		"vcap.application.application_id=666guid",
		"vcap.application.application_version=2.0",
		"vcap.application.instance_index=123" })
@ActiveProfiles("cloud")
public class CloudFoundryMicrometerActiveCloudProfile extends AbstractMicrometerTagTest {

	@Test
	public void testPresetTagValues() {
		assertThat(meter.getId().getTag("cf.org.name")).isEqualTo("PivotalOrg");
		assertThat(meter.getId().getTag("cf.space.id")).isEqualTo("SpringSpaceId");
		assertThat(meter.getId().getTag("cf.space.name")).isEqualTo("SpringSpace");
		assertThat(meter.getId().getTag("cf.app.name")).isEqualTo("App666");
		assertThat(meter.getId().getTag("cf.app.id")).isEqualTo("666guid");
		assertThat(meter.getId().getTag("cf.app.version")).isEqualTo("2.0");
		assertThat(meter.getId().getTag("cf.instance.index")).isEqualTo("123");
	}
}
