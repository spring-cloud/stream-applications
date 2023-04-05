/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.cloud.fn.header.filter;

import java.util.HashSet;
import java.util.function.Function;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.transformer.HeaderFilter;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

/**
 * Configure a function using {@link HeaderFilter}.
 *
 * @author Corneil du Plessis
 */
@AutoConfiguration
@EnableConfigurationProperties(HeaderFilterFunctionProperties.class)
@ConditionalOnExpression("'${header.filter.remove}'!='' or '${header.filter.delete-all}' != ''")
public class HeaderFilterFunctionConfiguration {
	private final HeaderFilterFunctionProperties properties;
	public HeaderFilterFunctionConfiguration(HeaderFilterFunctionProperties properties) {
		this.properties = properties;
	}

	@Bean
	public Function<Message<?>, Message<?>> headerFilterFunction() {
		if (properties.isDeleteAll()) {
			return (message) -> {
				var  accessor = new IntegrationMessageHeaderAccessor(message);
				var headers = new HashSet<>(message.getHeaders().keySet());
				headers.removeIf(accessor::isReadOnly);
				HeaderFilter filter = new HeaderFilter(headers.toArray(new String[0]));
				return filter.transform(message);
			};
		}
		else {
			return headerFilter()::transform;
		}
	}

	@Bean
	public HeaderFilter headerFilter() {
		if (properties.getRemove() != null) {
			String[] remove = StringUtils.tokenizeToStringArray(properties.getRemove(), ", ", true, true);
			HeaderFilter filter = new HeaderFilter(remove);
			if (properties.getRemove().contains("*")) {
				filter.setPatternMatch(true);
			}
			return filter;
		}
		else {
			return new HeaderFilter("");
		}
	}

}
