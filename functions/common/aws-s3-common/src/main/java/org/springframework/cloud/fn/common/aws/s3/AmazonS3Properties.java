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

package org.springframework.cloud.fn.common.aws.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Timo Salm
 * @author David Turanski
 */
@ConfigurationProperties("s3.common")
public class AmazonS3Properties {

	/**
	 * Optional endpoint url to connect to s3 compatible storage.
	 */
	private String endpointUrl;

	/**
	 * Use path style access.
	 */
	private boolean pathStyleAccess;

	public boolean isPathStyleAccess() {
		return pathStyleAccess;
	}

	public void setPathStyleAccess(boolean pathStyleAccess) {
		this.pathStyleAccess = pathStyleAccess;
	}

	public String getEndpointUrl() {
		return this.endpointUrl;
	}

	public void setEndpointUrl(String endpointUrl) {
		this.endpointUrl = endpointUrl;
	}
}
