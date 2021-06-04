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

import java.io.IOException;
import java.nio.charset.Charset;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.pivotal.cfenv.test.AbstractCfEnvTests;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimplePropertiesConfigAdapter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 * @author Soby Chacko
 */
@SpringBootTest(classes = AbstractMicrometerTagTest.AutoConfigurationApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class AbstractMicrometerTagTest {

	@Autowired
	protected SimpleMeterRegistry simpleMeterRegistry;

	@Autowired
	protected ConfigurableApplicationContext context;

	protected Meter meter;

	@BeforeAll
	public static void mockVcapServices() throws IOException {
		String serviceJson = StreamUtils.copyToString(new DefaultResourceLoader().getResource(
				"classpath:/org/springframework/cloud/stream/app/micrometer/common/pcf-scs-info.json")
				.getInputStream(), Charset.forName("UTF-8"));
		new Vcap().mockServices(serviceJson);
	}

	@BeforeEach
	public void before() {
		assertThat(simpleMeterRegistry).isNotNull();
		meter = simpleMeterRegistry.find("jvm.memory.committed").meter();
		assertThat(meter).isNotNull();
	}

	@SpringBootApplication
	@EnableConfigurationProperties(SimpleProperties.class)
	public static class AutoConfigurationApplication {

		public static void main(String[] args) {
			SpringApplication.run(AutoConfigurationApplication.class, args);
		}

		@Bean
		public SimpleMeterRegistry simpleMeterRegistry(SimpleConfig config, Clock clock) {
			return new SimpleMeterRegistry(config, clock);
		}

		@Bean
		@ConditionalOnMissingBean
		public SimpleConfig simpleConfig(SimpleProperties simpleProperties) {
			return new SimplePropertiesConfigAdapter(simpleProperties);
		}
	}
}

class Vcap extends AbstractCfEnvTests {
	public void mockServices(String json) {
		super.mockVcapServices(json);
	}
}
