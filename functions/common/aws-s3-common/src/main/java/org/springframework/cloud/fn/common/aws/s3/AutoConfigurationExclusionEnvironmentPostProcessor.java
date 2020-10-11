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

package org.springframework.cloud.fn.common.aws.s3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.cloud.aws.autoconfigure.context.ContextInstanceDataAutoConfiguration;
import org.springframework.cloud.aws.autoconfigure.context.ContextResourceLoaderAutoConfiguration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.StringUtils;

/**
 * @author Timo Salm
 * @author David Turanski
 */
@Order
public class AutoConfigurationExclusionEnvironmentPostProcessor implements EnvironmentPostProcessor {

	static final String SPRING_AUTOCONFIGURE_EXCLUDE_PROPERTY = "spring.autoconfigure.exclude";

	static final String S_3_COMMON_ENDPOINT_URL = "s3.common.endpoint-url";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		final PropertySource<?> propertySource = environment.getPropertySources()
				.get(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		if (propertySource instanceof MapPropertySource) {
			String excludedAutoConfiguration = ContextResourceLoaderAutoConfiguration.class.getCanonicalName();
			//If an endpoint url is set, avoid the timeout attempting to connect to AWS to retrieve instance data.
			if (StringUtils.hasText(environment.getProperty(S_3_COMMON_ENDPOINT_URL))) {
				excludedAutoConfiguration = excludedAutoConfiguration
						.concat("," + ContextInstanceDataAutoConfiguration.class.getCanonicalName());
			}
			((MapPropertySource) propertySource).getSource().put(SPRING_AUTOCONFIGURE_EXCLUDE_PROPERTY,
					excludedAutoConfiguration);
		}
	}
}
