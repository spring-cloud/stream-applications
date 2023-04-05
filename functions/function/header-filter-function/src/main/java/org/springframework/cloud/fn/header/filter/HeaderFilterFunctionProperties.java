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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for configuration of header-filter-function.
 * @author Corneil du Plessis
 */
@ConfigurationProperties("header.filter")
public class HeaderFilterFunctionProperties {
	/**
	 * Indicates the need to remove all headers.
	 */
	private boolean deleteAll = false;

	/**
	 * Remove all headers named. A comma, space separated list of header names.
	 * The names may contain patterns.
	 */
	private String remove;

	public boolean isDeleteAll() {
		return deleteAll;
	}

	public void setDeleteAll(boolean deleteAll) {
		this.deleteAll = deleteAll;
	}

	public String getRemove() {
		return remove;
	}

	public void setRemove(String remove) {
		this.remove = remove;
	}
}
