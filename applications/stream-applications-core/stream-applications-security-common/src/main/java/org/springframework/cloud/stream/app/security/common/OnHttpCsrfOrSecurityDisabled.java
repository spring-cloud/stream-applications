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

package org.springframework.cloud.stream.app.security.common;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * An {@link AnyNestedCondition} to enable app starters-specific security auto-configuration
 * overriding out-of-the-box one in Spring Boot.
 *
 * @author Artem Bilan
 * @since 3.0
 */
class OnHttpCsrfOrSecurityDisabled extends AnyNestedCondition {

	OnHttpCsrfOrSecurityDisabled() {
		super(ConfigurationPhase.PARSE_CONFIGURATION);
	}

	@ConditionalOnProperty(name = "spring.cloud.streamapp.security.enabled", havingValue = "false", matchIfMissing = true)
	static class SecurityDisabled {
	}

	@ConditionalOnProperty(name = "spring.cloud.streamapp.security.csrf-enabled", havingValue = "false", matchIfMissing = true)
	static class HttpCsrfDisabled {
	}

}
