/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.stream.app.test;

import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

/**
 * Unlike the {@code PropertiesInitializer}, this does not require boot infrastructure
 * to add properties to the context. Used for testing generated apps where the
 * {@code ApplicationContextInitializer} can't be used. Since it's a BDRPP, it runs
 * before any BFPPs - i.e. as early as possible.
 *
 * @author Gary Russell
 */
public class BinderTestPropertiesInitializer implements BeanDefinitionRegistryPostProcessor {

	private final ConfigurableApplicationContext context;

	private final Properties properties;

	public BinderTestPropertiesInitializer(ConfigurableApplicationContext context, Properties properties) {
		this.context = context;
		this.properties = properties;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		this.context.getEnvironment().getPropertySources()
				.addLast(new PropertiesPropertySource("scsAppProperties", properties));
	}

}
