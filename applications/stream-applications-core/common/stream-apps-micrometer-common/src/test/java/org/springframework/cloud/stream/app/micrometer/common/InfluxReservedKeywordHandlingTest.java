/*
 * Copyright 2019-2020 the original author or authors.
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

import io.micrometer.core.instrument.Meter;
import io.micrometer.influx.InfluxMeterRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = InfluxReservedKeywordHandlingTest.AutoConfigurationApplication.class,
		properties = {
				"management.metrics.export.influx.enabled=true",
				"spring.cloud.dataflow.stream.app.label=time"})
public class InfluxReservedKeywordHandlingTest {

	@Autowired
	protected InfluxMeterRegistry influxMeterRegistry;

	@Test
	public void testPresetTagValues() {

		assertThat(influxMeterRegistry).isNotNull();

		Meter meter = influxMeterRegistry.find("jvm.memory.committed").meter();
		assertThat(meter).isNotNull();

		assertThat(meter.getId().getTag("application.name")).isEqualTo(("atime"));
	}

	@SpringBootApplication
	public static class AutoConfigurationApplication {
		public static void main(String[] args) {
			SpringApplication.run(AutoConfigurationApplication.class, args);
		}
	}
}
