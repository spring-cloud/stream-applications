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
 * Maven configurations used by the {@link AppDefinition} and {@link BinderDefinition} to parametrize the generated POMs.
 *
 * @author Christian Tzolov
 */
public class MavenDefinition {
	/**
	 * Contributes the properties to the Maven's properties section.
	 */
	private List<String> properties = new ArrayList<>();

	/**
	 * Contributes the dependencyManagement entries to the Maven's dependencyManagement dependencies.
	 */
	private List<String> dependencyManagement = new ArrayList<>();

	/**
	 * Contributes the dependency entries to the Maven's dependencies section.
	 */
	private List<String> dependencies = new ArrayList<>();

	/**
	 * Contributes the plugin entries to the Maven's plugins section.
	 */
	private List<String> plugins = new ArrayList<>();

	/**
	 * Contributes the repository entries to the Maven's repositories section.
	 */
	private List<String> repositories = new ArrayList<>();

	public List<String> getProperties() {
		return properties;
	}

	public void setProperties(List<String> properties) {
		this.properties = properties;
	}

	public List<String> getDependencyManagement() {
		return dependencyManagement;
	}

	public void setDependencyManagement(List<String> dependencyManagement) {
		this.dependencyManagement = dependencyManagement;
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

	public List<String> getRepositories() {
		return repositories;
	}

	public void setRepositories(List<String> repositories) {
		this.repositories = repositories;
	}
}
