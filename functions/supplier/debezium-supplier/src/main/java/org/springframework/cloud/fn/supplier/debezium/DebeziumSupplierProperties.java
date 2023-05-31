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

package org.springframework.cloud.fn.supplier.debezium;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("debezium.supplier")
public class DebeziumSupplierProperties {

	/**
	 * Copy Change Event headers into Message headers.
	 */
	private boolean copyHeaders = true;

	public boolean isCopyHeaders() {
		return copyHeaders;
	}

	public void setCopyHeaders(boolean copyHeaders) {
		this.copyHeaders = copyHeaders;
	}
}
