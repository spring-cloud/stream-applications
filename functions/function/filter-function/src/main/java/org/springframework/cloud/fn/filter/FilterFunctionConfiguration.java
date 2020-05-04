/*
 * Copyright (c) 2020 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.fn.filter;

import java.util.Optional;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.fn.spel.SpelFunctionConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;

@Configuration
@Import(SpelFunctionConfiguration.class)
public class FilterFunctionConfiguration {

	@Bean
	public Function<Message<?>, Message<?>> filterFunction(
			@Qualifier("spelFunction") Function<Message<?>, Message<?>> spelFunction) {

		return message ->
				Optional.of(message)
						.filter(m -> (Boolean) spelFunction.apply(m).getPayload())
						.orElse(null);
	}

}
