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
 * @author Christian Tzolov
 */
public class BinderDefinition {

	private String name;

	private final Maven maven = new Maven();

	private List<String> properties = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Maven getMaven() {
		return maven;
	}

	public List<String> getProperties() {
		return properties;
	}

	public void setProperties(List<String> properties) {
		this.properties = properties;
	}

	public static class Maven {

		private List<String> properties = new ArrayList<>();

		private List<String> managedDependencies = new ArrayList<>();

		private List<String> dependencies = new ArrayList<>();

		private List<String> plugins = new ArrayList<>();

		public List<String> getProperties() {
			return properties;
		}

		public void setProperties(List<String> properties) {
			this.properties = properties;
		}

		public List<String> getManagedDependencies() {
			return managedDependencies;
		}

		public void setManagedDependencies(List<String> managedDependencies) {
			this.managedDependencies = managedDependencies;
		}

		public List<String> getDependencies() {
			return dependencies;
		}

		public void setDependencies(List<String> dependencies) {
			this.dependencies = dependencies;
		}

		public List<String> getPlugins() {
			return plugins;
		}

		public void setPlugins(List<String> plugins) {
			this.plugins = plugins;
		}
	}
}
