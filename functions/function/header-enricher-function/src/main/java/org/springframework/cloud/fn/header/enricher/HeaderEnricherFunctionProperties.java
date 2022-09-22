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

package org.springframework.cloud.fn.header.enricher;

import java.util.Properties;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the Header Enricher Processor application.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Soby Chacko
 */
@ConfigurationProperties("header.enricher")
@Validated
public class HeaderEnricherFunctionProperties {

	/**
	 * \n separated properties representing headers in which values are SpEL expressions, e.g
	 * foo='bar' \n baz=payload.baz.
	 */
	private Properties headers;

	/**
	 * set to true to overwrite any existing message headers.
	 */
	private boolean overwrite = false;

	@NotNull
	public Properties getHeaders() {
		return this.headers;
	}

	public void setHeaders(Properties headers) {
		this.headers = headers;
	}

	public boolean isOverwrite() {
		return this.overwrite;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

}
