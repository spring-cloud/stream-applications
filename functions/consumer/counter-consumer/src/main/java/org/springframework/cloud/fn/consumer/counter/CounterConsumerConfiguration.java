/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.cloud.fn.consumer.counter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.messaging.Message;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 *
 * @author Christian Tzolov
 */
@Configuration
@EnableConfigurationProperties({ CounterConsumerProperties.class })
public class CounterConsumerConfiguration {

	@Bean
	public Function<String, Expression> stringToSpelFunction(@Lazy EvaluationContext evaluationContext) {
		return new StringToSpelConversionFunction(evaluationContext);
	}

	@Bean
	@ConfigurationPropertiesBinding
	public Converter<String, Expression> propertiesSpelConverter(Function<String, Expression> stringToSpelFunction) {
		return new Converter<String, Expression>() { // NOTE Using lambda causes Java Generics issues.
			@Override
			public Expression convert(String source) {
				return stringToSpelFunction.apply(source);
			}
		};
	}

	@Bean(name = "counterConsumer")
	public Consumer<Message<?>> counterConsumer(CounterConsumerProperties properties, MeterRegistry[] meterRegistries,
			@Qualifier(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME) EvaluationContext context) {

		return message -> {

			String counterName = properties.getComputedNameExpression().getValue(context, message, CharSequence.class).toString();

			// All fixed tags together are passed with every counter increment.
			Tags fixedTags = this.toTags(properties.getTag().getFixed());

			double amount = properties.getComputedAmountExpression().getValue(context, message, double.class);

			Map<String, List<Tag>> allGroupedTags = new HashMap<>();
			// Tag Expressions Counter
			if (properties.getTag().getExpression() != null) {

				Map<String, List<Tag>> groupedTags = properties.getTag().getExpression().entrySet().stream()
						// maps a <name, expr> pair into [<name, expr#val_1>, ... <name, expr#val_N>] Tag array.
						.map(namedExpression ->
								toList(namedExpression.getValue().getValue(context, message)).stream()
										.map(tagValue -> Tag.of(namedExpression.getKey(), tagValue))
										.collect(Collectors.toList())).flatMap(List::stream)
						.collect(Collectors.groupingBy(tag -> tag.getKey(), Collectors.toList()));
				allGroupedTags.putAll(groupedTags);
			}

			this.count(meterRegistries, counterName, fixedTags, allGroupedTags, amount);
		};
	}

	/**
	 * Converts a key/value Map into Tag(key,value) list. Filters out the empty key/value pairs.
	 * @param keyValueMap key/value map to convert into tags.
	 * @return Returns Tags list representing every non-empty key/value pair.
	 */
	protected Tags toTags(Map<String, String> keyValueMap) {
		return CollectionUtils.isEmpty(keyValueMap) ? Tags.empty() :
				Tags.of(keyValueMap.entrySet().stream()
						.filter(e -> StringUtils.hasText(e.getKey()) && StringUtils.hasText(e.getValue()))
						.map(e -> Tag.of(e.getKey(), e.getValue()))
						.collect(Collectors.toList()));
	}

	/**
	 * Converts the input value into an list of values. If the value is not a collection/array type the result
	 * is a single element list. For collection/array input value the result is the list of stringifie content of
	 * this collection.
	 * @param value input value can be array, collection or single value.
	 * @return Returns value list.
	 */
	protected List<String> toList(Object value) {
		if (value == null) {
			return Collections.emptyList();
		}

		if ((value instanceof Collection) || ObjectUtils.isArray(value)) {
			Collection<?> valueCollection = (value instanceof Collection) ? (Collection<?>) value
					: Arrays.asList(ObjectUtils.toObjectArray(value));

			return valueCollection.stream()
					.filter(v -> v != null)
					.map(Object::toString)
					.filter(StringUtils::hasText)
					.collect(Collectors.toList());
		}
		else {
			return Arrays.asList(value.toString());
		}
	}

	private void count(MeterRegistry[] meterRegistries, String counterName, Tags fixedTags, Map<String, List<Tag>> groupedTags, double amount) {
		if (!CollectionUtils.isEmpty(groupedTags)) {
			groupedTags.values().stream().map(List::size).max(Integer::compareTo).ifPresent(
					max -> {
						for (int i = 0; i < max; i++) {
							Tags currentTags = Tags.of(fixedTags);
							for (Map.Entry<String, List<Tag>> e : groupedTags.entrySet()) {
								currentTags = (e.getValue().size() > i) ?
										currentTags.and(e.getValue().get(i)) :
										currentTags.and(Tags.of(e.getKey(), ""));
							}

							// Increment the counterName increment for every configured MaterRegistry.
							for (MeterRegistry meterRegistry : meterRegistries) {
								meterRegistry.counter(counterName, currentTags).increment(amount);
							}
						}
					}
			);
		}
		else {
			// Increment the counterName increment for every configured MaterRegistry.
			for (MeterRegistry meterRegistry : meterRegistries) {
				meterRegistry.counter(counterName, fixedTags).increment(amount);
			}
		}
	}

	@Bean
	@ConditionalOnMissingBean
	public SimpleMeterRegistry simpleMeterRegistry() {
		return new SimpleMeterRegistry();
	}
}
