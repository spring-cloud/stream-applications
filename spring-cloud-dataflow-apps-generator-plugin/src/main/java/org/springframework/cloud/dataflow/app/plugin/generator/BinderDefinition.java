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

package org.springframework.cloud.dataflow.app.plugin.generator;

import java.util.ArrayList;
import java.util.List;

/**
 * Binder configurations used to parameterize the project templates.
 *
 * @author Christian Tzolov
 */
public class BinderDefinition {

	/**
	 * Binder name.
	 */
	private String name;

	/**
	 * Binder specific, Maven configurations contributed to the generated poms.
	 */
	private final MavenDefinition maven = new MavenDefinition();

	/**
	 * Binder specific application properties contributed to the generated application.properties.
	 */
	private List<String> properties = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public MavenDefinition getMaven() {
		return maven;
	}

	public List<String> getProperties() {
		return properties;
	}

	public void setProperties(List<String> properties) {
		this.properties = properties;
	}
}
