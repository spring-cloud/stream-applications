/*
 * Copyright 2018 the original author or authors.
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

import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Christian Tzolov
 */
@RunWith(Enclosed.class)
public class CloudFoundryMicrometerCommonTagsTest {

	@ActiveProfiles("cloud")
	public static class ActiveCloudProfileDefaultValues extends AbstractMicrometerTagTest {
		@Test
		public void testDefaultTagValues() {
			assertThat(meter.getId().getTag("cf.org.name"), is("default"));
			assertThat(meter.getId().getTag("cf.space.id"), is("unknown"));
			assertThat(meter.getId().getTag("cf.space.name"), is("unknown"));
			assertThat(meter.getId().getTag("cf.app.name"), is("unknown"));
			assertThat(meter.getId().getTag("cf.app.id"), is("unknown"));
			assertThat(meter.getId().getTag("cf.app.version"), is("unknown"));
			assertThat(meter.getId().getTag("cf.instance.index"), is("0"));
		}
	}

	@TestPropertySource(properties = {
			"vcap.application.org_name=PivotalOrg",
			"vcap.application.space_id=SpringSpaceId",
			"vcap.application.space_name=SpringSpace",
			"vcap.application.application_name=App666",
			"vcap.application.application_id=666guid",
			"vcap.application.application_version=2.0",
			"vcap.application.instance_index=123" })
	@ActiveProfiles("cloud")
	public static class ActiveCloudProfile extends AbstractMicrometerTagTest {

		@Test
		public void testPresetTagValues() {
			assertThat(meter.getId().getTag("cf.org.name"), is("PivotalOrg"));
			assertThat(meter.getId().getTag("cf.space.id"), is("SpringSpaceId"));
			assertThat(meter.getId().getTag("cf.space.name"), is("SpringSpace"));
			assertThat(meter.getId().getTag("cf.app.name"), is("App666"));
			assertThat(meter.getId().getTag("cf.app.id"), is("666guid"));
			assertThat(meter.getId().getTag("cf.app.version"), is("2.0"));
			assertThat(meter.getId().getTag("cf.instance.index"), is("123"));
		}
	}

	@TestPropertySource(properties = {
			"vcap.application.org_name=PivotalOrg",
			"vcap.application.space_id=SpringSpaceId",
			"vcap.application.space_name=SpringSpace",
			"vcap.application.application_name=App666",
			"vcap.application.application_id=666guid",
			"vcap.application.application_version=2.0",
			"vcap.application.instance_index=123" })
	public static class InactiveCloudProfile extends AbstractMicrometerTagTest {

		@Test
		public void testDisabledTagValues() {
			assertThat(meter.getId().getTag("cf.org.name"), is(Matchers.nullValue()));
			assertThat(meter.getId().getTag("cf.space.id"), is(Matchers.nullValue()));
			assertThat(meter.getId().getTag("cf.space.name"), is(Matchers.nullValue()));
			assertThat(meter.getId().getTag("cf.app.name"), is(Matchers.nullValue()));
			assertThat(meter.getId().getTag("cf.app.id"), is(Matchers.nullValue()));
			assertThat(meter.getId().getTag("cf.app.version"), is(Matchers.nullValue()));
			assertThat(meter.getId().getTag("cf.instance.index"), is(Matchers.nullValue()));
		}
	}

	@TestPropertySource(properties = { "spring.cloud.stream.app.metrics.cf.tags.enabled=false" })
	@ActiveProfiles("cloud")
	public static class ActiveCloudProfileDisabledProperty extends InactiveCloudProfile {
	}
}

