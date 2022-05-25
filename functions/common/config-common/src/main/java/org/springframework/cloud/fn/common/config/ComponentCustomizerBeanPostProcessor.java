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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.core.log.LogMessage;
import org.springframework.core.type.MethodMetadata;

/**
 * The {@link MergedBeanDefinitionPostProcessor} to apply a {@link ComponentCustomizer} for a bean
 * in process if this bean is marked with the {@link CustomizationAware} annotation and
 * a customizer for a type of this bean is present in the application context.
 *
 * @author Artem Bilan
 *
 * @since 1.2.1
 */
class ComponentCustomizerBeanPostProcessor implements MergedBeanDefinitionPostProcessor, BeanFactoryAware {

	private static final Log logger = LogFactory.getLog(ComponentCustomizerBeanPostProcessor.class);

	private final Set<String> customizationAwareBeanNames = new HashSet<>();

	private BeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
		if (beanDefinition instanceof AnnotatedBeanDefinition) {
			AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) beanDefinition;
			MethodMetadata factoryMethodMetadata = annotatedBeanDefinition.getFactoryMethodMetadata();
			if (factoryMethodMetadata != null &&
					factoryMethodMetadata.isAnnotated(CustomizationAware.class.getName())) {

				this.customizationAwareBeanNames.add(beanName);
			}
		}
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (this.customizationAwareBeanNames.remove(beanName)) {
			ResolvableType integrationComponentCustomizerType =
					ResolvableType.forClassWithGenerics(ComponentCustomizer.class,
							AopProxyUtils.ultimateTargetClass(bean));

			ComponentCustomizer<Object> componentCustomizer =
					this.beanFactory.<ComponentCustomizer<Object>>getBeanProvider(integrationComponentCustomizerType)
					.getIfAvailable();

			if (componentCustomizer != null) {
				if (logger.isDebugEnabled()) {
					logger.debug(LogMessage.format("Use '%s' for '%s' customization...", componentCustomizer, beanName));
				}
				componentCustomizer.customize(bean, beanName);
			}
		}

		return bean;
	}

}
