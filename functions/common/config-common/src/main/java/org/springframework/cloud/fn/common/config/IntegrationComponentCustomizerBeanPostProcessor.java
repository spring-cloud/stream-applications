/*
 * Copyright 2022-2022 the original author or authors.
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

package org.springframework.cloud.fn.common.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.ResolvableType;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationComponentSpec;
import org.springframework.messaging.MessageHandler;

/**
 * The {@link BeanPostProcessor} to apply a {@link IntegrationComponentCustomizer} for a bean
 * to process if a customizer for its type is present in the application context.
 *
 * @author Artem Bilan
 *
 * @since 1.2.1
 */
class IntegrationComponentCustomizerBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

	private BeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof MessageSource ||
				bean instanceof MessageHandler ||
				bean instanceof MessageProducer ||
				bean instanceof IntegrationComponentSpec<?, ?>) {

			ResolvableType integrationComponentCustomizerType =
					ResolvableType.forClassWithGenerics(IntegrationComponentCustomizer.class, bean.getClass());

			IntegrationComponentCustomizer<Object> beanCustomizer =
					this.beanFactory.<IntegrationComponentCustomizer<Object>>getBeanProvider(
									integrationComponentCustomizerType)
							.getIfAvailable();

			if (beanCustomizer != null) {
				beanCustomizer.customizeBean(bean);
			}
		}
		return bean;
	}

}
