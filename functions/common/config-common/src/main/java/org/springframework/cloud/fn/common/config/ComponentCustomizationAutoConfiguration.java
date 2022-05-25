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

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

/**
 * An auto-configuration to expose customizer strategy beans to accept user-provided component customizers.
 *
 * @author Artem Bilan
 *
 * @since 1.2.1
 *
 * @see ComponentCustomizerBeanPostProcessor
 */
public class ComponentCustomizationAutoConfiguration {

	@Bean
	@ConditionalOnBean(ComponentCustomizer.class)
	static BeanPostProcessor componentCustomizerBeanPostProcessor() {
		return new ComponentCustomizerBeanPostProcessor();
	}

}
