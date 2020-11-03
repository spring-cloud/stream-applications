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

package org.springframework.cloud.stream.app.test.integration.junit.jupiter;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;

/**
 * A junit Jupiter extension used to discover {@link StreamAppContainer}s annotated with {code @BaseContainer}.
 * @author David Turanski
 */
public class BaseContainerExtension implements ExecutionCondition {
	private static StreamAppContainer baseContainer;

	public static StreamAppContainer containerInstance() {
		return baseContainer;
	}

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
		AtomicInteger count = new AtomicInteger();
		AnnotationUtils.findAnnotatedFields(extensionContext.getRequiredTestClass(), BaseContainer.class, field -> {
			try {
				StreamAppContainer base = (StreamAppContainer) field.get(null);
				if (base == null) {
					throw new ExtensionConfigurationException("@BaseContainer is not initialized");
				}
				count.getAndIncrement();

				baseContainer = base;
			}
			catch (Exception e) {
				throw new ExtensionConfigurationException(e.getMessage(), e);
			}
			return true;
		});
		if (count.get() != 1) {
			throw new ExtensionConfigurationException(
					"Expecting exactly one @BaseContainer instance, found " + count.get());
		}
		return ConditionEvaluationResult.enabled("@BaseContainer found");
	}
}
