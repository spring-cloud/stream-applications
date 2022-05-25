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

/**
 * The customizer contract to apply to beans in the application context which are marked
 * with the {@link CustomizationAware} annotation and their type is matching to generic
 * type of the instance of this interface.
 * <p>
 * The bean for {@link ComponentCustomizer} has to be declared as a {@code static} bean
 * method to avoid early bean initialization syndrome when not all bean post processors
 * are configured into the application context yet. Its dependencies have to be {@code static}
 * as well, or all of related beans can be declared in the dedicated {@code @Configuration} class.
 *
 * @param <T> the target component (bean) type in the application context to customize.
 *
 * @author Artem Bilan
 *
 * @since 1.2.1
 */
@FunctionalInterface
public interface ComponentCustomizer<T> {

	void customize(T component, String beanName);

}
