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
public class AppDefinition {

	private String name;
	private AppType type;
	private String version;
	private String configClass;
	private String functionDefinition;
	private List<String> metadataSourceTypeFilters = new ArrayList<>();
	private List<String> metadataNameFilters = new ArrayList<>();
	private List<String> additionalProperties = new ArrayList<>();
	private List<String> mavenManagedDependencies = new ArrayList<>();
	private List<String> mavenDependencies = new ArrayList<>();
	private List<String> mavenPlugins = new ArrayList<>();
	/**
	 * Allow to generate either Docker or OCI image formats
	 */
	private ContainerImageFormat containerImageFormat = ContainerImageFormat.Docker;
	/**
	 * True will attempt to inline the (white) filtered Spring Boot metadata as Base64 encoded property.
	 */
	private boolean enableContainerImageMetadata = false;
	private String containerImageOrgName = "springcloudstream";
	private String containerImageTag = "latest";

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public AppType getType() {
		return type;
	}

	public void setType(AppType type) {
		this.type = type;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public List<String> getAdditionalProperties() {
		return additionalProperties;
	}

	public void setAdditionalProperties(List<String> additionalProperties) {
		this.additionalProperties = additionalProperties;
	}

	public List<String> getMavenDependencies() {
		return mavenDependencies;
	}

	public void setMavenDependencies(List<String> mavenDependencies) {
		this.mavenDependencies = mavenDependencies;
	}

	public List<String> getMavenManagedDependencies() {
		return mavenManagedDependencies;
	}

	public void setMavenManagedDependencies(List<String> mavenManagedDependencies) {
		this.mavenManagedDependencies = mavenManagedDependencies;
	}

	public List<String> getMavenPlugins() {
		return mavenPlugins;
	}

	public void setMavenPlugins(List<String> mavenPlugins) {
		this.mavenPlugins = mavenPlugins;
	}

	public boolean isSupplier() {
		return type == AppType.source;
	}

	public boolean isConsumer() {
		return type == AppType.sink;
	}

	public boolean isFunction() {
		return type == AppType.processor;
	}

	public String getConfigClass() {
		return configClass;
	}

	public void setConfigClass(String configClass) {
		this.configClass = configClass;
	}

	public List<String> getMetadataSourceTypeFilters() {
		return metadataSourceTypeFilters;
	}

	public void setMetadataSourceTypeFilters(List<String> metadataSourceTypeFilters) {
		this.metadataSourceTypeFilters = metadataSourceTypeFilters;
	}

	public List<String> getMetadataNameFilters() {
		return metadataNameFilters;
	}

	public void setMetadataNameFilters(List<String> metadataNameFilters) {
		this.metadataNameFilters = metadataNameFilters;
	}

	public ContainerImageFormat getContainerImageFormat() {
		return containerImageFormat;
	}

	public void setContainerImageFormat(ContainerImageFormat containerImageFormat) {
		this.containerImageFormat = containerImageFormat;
	}

	public boolean isEnableContainerImageMetadata() {
		return enableContainerImageMetadata;
	}

	public void setEnableContainerImageMetadata(boolean enableContainerImageMetadata) {
		this.enableContainerImageMetadata = enableContainerImageMetadata;
	}

	public String getContainerImageOrgName() {
		return containerImageOrgName;
	}

	public void setContainerImageOrgName(String containerImageOrgName) {
		this.containerImageOrgName = containerImageOrgName;
	}

	public String getContainerImageTag() {
		return containerImageTag;
	}

	public void setContainerImageTag(String containerImageTag) {
		this.containerImageTag = containerImageTag;
	}

	public String getFunctionDefinition() {
		return functionDefinition;
	}

	public void setFunctionDefinition(String functionDefinition) {
		this.functionDefinition = functionDefinition;
	}

	public enum ContainerImageFormat {Docker, OCI}

	public enum AppType {source, processor, sink}
}
