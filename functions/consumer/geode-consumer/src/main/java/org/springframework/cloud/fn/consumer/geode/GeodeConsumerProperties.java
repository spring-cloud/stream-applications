/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.cloud.fn.consumer.geode;

import jakarta.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @author David Turanski
 */
@ConfigurationProperties("geode.consumer")
@Validated
public class GeodeConsumerProperties {

	/**
	 * SpEL expression to use as a cache key.
	 */
	private String keyExpression;

	/**
	 * Indicates if the Geode region stores json objects as PdxInstance.
	 */
	private boolean json;

	@NotEmpty(message = "A valid key expression is required")
	public String getKeyExpression() {
		return keyExpression;
	}

	public void setKeyExpression(String keyExpression) {
		this.keyExpression = keyExpression;
	}

	public boolean isJson() {
		return json;
	}

	public void setJson(boolean json) {
		this.json = json;
	}
}
