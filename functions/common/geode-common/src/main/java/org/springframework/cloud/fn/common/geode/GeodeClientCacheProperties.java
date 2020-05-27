/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.cloud.fn.common.geode;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Geode client pool configuration properties.
 *
 * @author Christian Tzolov
 */
@ConfigurationProperties("geode.client")
@Validated
public class GeodeClientCacheProperties {

	/**
	 * Deserialize the Geode objects into PdxInstance instead of the domain class.
	 */
	private boolean pdxReadSerialized = false;

	public boolean isPdxReadSerialized() {
		return pdxReadSerialized;
	}

	public void setPdxReadSerialized(boolean pdxReadSerialized) {
		this.pdxReadSerialized = pdxReadSerialized;
	}
}
