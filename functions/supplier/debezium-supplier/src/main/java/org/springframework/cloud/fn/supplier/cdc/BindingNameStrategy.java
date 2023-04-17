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

package org.springframework.cloud.fn.supplier.cdc;

import org.springframework.cloud.function.context.FunctionProperties;
import org.springframework.util.StringUtils;

/**
 * Computes the binding name. If the 'overrideBindingName' property is not empty it is used as binding name. Otherwise
 * the binding name is computed from the function definition name and the '-out-0' suffix. If the function definition
 * name is empty, then the binding name defaults to 'cdcSupplier-out-0'.
 */
public class BindingNameStrategy {

	private static final String DEFAULT_FUNCTION_DEFINITION_NAME = "cdcSupplier";
	private static final String DEFAULT_SUFFIX = "-out-0";

	private CdcProperties cdcProperties;
	private FunctionProperties functionProperties;

	public BindingNameStrategy(CdcProperties cdcProperties, FunctionProperties functionProperties) {
		this.cdcProperties = cdcProperties;
		this.functionProperties = functionProperties;
	}

	public String bindingName() {

		if (StringUtils.hasText(cdcProperties.getOverrideBindingName())) {
			return cdcProperties.getOverrideBindingName();
		}
		else if (StringUtils.hasText(functionProperties.getDefinition())) {
			return functionProperties.getDefinition() + DEFAULT_SUFFIX;
		}

		return DEFAULT_FUNCTION_DEFINITION_NAME + DEFAULT_SUFFIX;
	}
}
