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

package org.springframework.cloud.dataflow.app.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.springframework.cloud.dataflow.app.plugin.generator.AppBom;
import org.springframework.cloud.dataflow.app.plugin.generator.AppDefinition;
import org.springframework.cloud.dataflow.app.plugin.generator.ProjectGenerator;
import org.springframework.cloud.dataflow.app.plugin.generator.ProjectGeneratorProperties;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author Christian Tzolov
 * @author David Turanski
 */
@Mojo(name = "generate-app")
public class SpringCloudStreamAppGeneratorMojo extends AbstractMojo {

	private static final String DEPRECATED_WHITELIST_FILE_NAME = "dataflow-configuration-metadata-whitelist.properties";
	private static final String VISIBLE_PROPERTIES_FILE_NAME = "dataflow-configuration-metadata.properties";
	private static final String CONFIGURATION_PROPERTIES_CLASSES = "configuration-properties.classes";
	private static final String CONFIGURATION_PROPERTIES_NAMES = "configuration-properties.names";

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(defaultValue = "${project.resources[0].directory}", readonly = true, required = true)
	private File projectResourcesDir;

	@Parameter(defaultValue = "./apps", required = true)
	private String generatedProjectHome;

	@Parameter
	private ContainerImage containerImage = new ContainerImage();

	@Parameter(required = true)
	private GeneratedApp generatedApp;

	@Parameter
	List<String> additionalAppProperties;

	@Parameter
	List<String> metadataSourceTypeFilters = new ArrayList<>();

	@Parameter
	List<String> metadataNameFilters = new ArrayList<>();

	@Parameter
	List<Dependency> boms = new ArrayList<>();

	@Parameter
	List<Dependency> dependencies = new ArrayList<>();

	@Parameter
	List<Dependency> globalDependencies = new ArrayList<>();

	@Parameter
	List<Plugin> additionalPlugins = new ArrayList<>();

	@Parameter
	List<String> binders = new ArrayList<>();

	// Versions
	@Parameter(defaultValue = "2.2.4.RELEASE", required = true)
	private String bootVersion;

	@Parameter(defaultValue = "${app-metadata-maven-plugin-version}")
	private String appMetadataMavenPluginVersion;

	@Override
	public void execute() throws MojoFailureException {
		// Bom
		AppBom appBom = new AppBom()
				.withSpringBootVersion(this.bootVersion)
				.withAppMetadataMavenPluginVersion(this.appMetadataMavenPluginVersion);

		AppDefinition app = new AppDefinition();
		app.setName(this.generatedApp.getName());
		app.setType(this.generatedApp.getType());
		app.setVersion(this.generatedApp.getVersion());
		app.setConfigClass(this.generatedApp.getConfigClass());
		app.setFunctionDefinition(this.generatedApp.getFunctionDefinition());

		this.populateVisiblePropertiesFromFile(this.metadataSourceTypeFilters, this.metadataNameFilters);

		if (!CollectionUtils.isEmpty(this.metadataSourceTypeFilters)) {
			app.setMetadataSourceTypeFilters(this.metadataSourceTypeFilters);
		}

		if (!CollectionUtils.isEmpty(this.metadataNameFilters)) {
			app.setMetadataNameFilters(this.metadataNameFilters);
		}

		app.setAdditionalProperties(this.additionalAppProperties);

		// BOM
		app.setMavenManagedDependencies(this.boms.stream()
				.filter(Objects::nonNull)
				.map(dependency -> {
					dependency.setScope("import");
					dependency.setType("pom");
					return dependency;
				})
				.map(MavenXmlWriter::toXml)
				.map(xml -> MavenXmlWriter.indent(xml, 12))
				.collect(Collectors.toList()));

		// Dependencies
		List<Dependency> allDependenciesMerged = new ArrayList<>(this.dependencies);
		allDependenciesMerged.addAll(this.globalDependencies);
		app.setMavenDependencies(allDependenciesMerged.stream()
				.map(MavenXmlWriter::toXml)
				.map(xml -> MavenXmlWriter.indent(xml, 8))
				.collect(Collectors.toList()));

		// Plugins
		app.setMavenPlugins(this.additionalPlugins.stream()
				.map(MavenXmlWriter::toXml)
				.map(d -> MavenXmlWriter.indent(d, 12))
				.collect(Collectors.toList()));

		// Container Image configuration
		app.setContainerImageFormat(this.containerImage.getFormat());
		app.setEnableContainerImageMetadata(this.containerImage.isEnableMetadata());
		if (StringUtils.hasText(this.containerImage.getOrgName())) {
			app.setContainerImageOrgName(this.containerImage.getOrgName());
		}

		app.setContainerImageTag(this.generatedApp.getVersion());

		// Generator Properties
		ProjectGeneratorProperties generatorProperties = new ProjectGeneratorProperties();
		generatorProperties.setBinders(this.binders);
		generatorProperties.setOutputFolder(new File(this.generatedProjectHome));
		generatorProperties.setAppBom(appBom);
		generatorProperties.setAppDefinition(app);
		generatorProperties.setProjectResourcesDirectory(this.projectResourcesDir);

		try {
			ProjectGenerator.getInstance().generate(generatorProperties);
		}
		catch (IOException e) {
			throw new MojoFailureException("Project generation failure");
		}
	}

	/**
	 * If the visible metadata properties file is provided in the source project, add
	 * its type and name filters to the existing visible configurations.
	 *
	 * @param sourceTypeFilters existing source type filters configured via the mojo parameter.
	 * @param nameFilters       existing name filters configured via the mojo parameter.
	 */
	private void populateVisiblePropertiesFromFile(List<String> sourceTypeFilters, List<String> nameFilters) {
		if (this.projectResourcesDir == null || !projectResourcesDir.exists()) {
			return;
		}
		Optional<Properties> optionalProperties =
				loadVisiblePropertiesFromResource(FileUtils.getFile(projectResourcesDir, "META-INF", VISIBLE_PROPERTIES_FILE_NAME));
		optionalProperties.ifPresent(properties -> {
			addToFilters(properties.getProperty(CONFIGURATION_PROPERTIES_CLASSES), sourceTypeFilters);
			addToFilters(properties.getProperty(CONFIGURATION_PROPERTIES_NAMES), nameFilters);
		});
		if (optionalProperties.isPresent()) {
			return;
		}

		loadVisiblePropertiesFromResource(FileUtils.getFile(projectResourcesDir, "META-INF", DEPRECATED_WHITELIST_FILE_NAME))
				.ifPresent(properties -> {
					getLog().warn(DEPRECATED_WHITELIST_FILE_NAME + " is deprecated." +
							" Please use " + VISIBLE_PROPERTIES_FILE_NAME + " instead");
					addToFilters(properties.getProperty(CONFIGURATION_PROPERTIES_CLASSES), sourceTypeFilters);
					addToFilters(properties.getProperty(CONFIGURATION_PROPERTIES_NAMES), nameFilters);
				});
	}

	private Optional<Properties> loadVisiblePropertiesFromResource(File visiblePropertiesFile) {
		if (visiblePropertiesFile.exists()) {
			Properties properties = new Properties();
			try (InputStream is = new FileInputStream(visiblePropertiesFile)) {
				properties.load(is);

			}
			catch (Exception e) {
				return Optional.empty();
			}
			return Optional.of(properties);
		}
		return Optional.empty();
	}

	private void addToFilters(String csvFilterProperties, List<String> filterList) {
		if (!StringUtils.isEmpty(csvFilterProperties)) {
			for (String filterProperty : csvFilterProperties.trim().split(",")) {
				if (StringUtils.hasText(filterProperty)) {
					if (!filterList.contains(filterProperty.trim())) {
						filterList.add(filterProperty.trim());
					}
				}
			}
		}
	}

	public static class GeneratedApp {

		private String name;
		private String version;
		private AppDefinition.AppType type;
		private String configClass;
		private String functionDefinition;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		public AppDefinition.AppType getType() {
			return type;
		}

		public void setType(AppDefinition.AppType type) {
			this.type = type;
		}

		public String getConfigClass() {
			return configClass;
		}

		public void setConfigClass(String configClass) {
			this.configClass = configClass;
		}

		public String getFunctionDefinition() {
			return (!StringUtils.isEmpty(this.functionDefinition)) ? this.functionDefinition :
					this.name + this.functionType();
		}

		public void setFunctionDefinition(String functionDefinition) {
			this.functionDefinition = functionDefinition;
		}

		private String functionType() {
			switch (this.type) {
				case processor:
					return "Function";
				case sink:
					return "Consumer";
				case source:
					return "Supplier";
			}
			throw new IllegalArgumentException("Unknown App type:" + this.type);
		}
	}

	public static class ContainerImage {
		private AppDefinition.ContainerImageFormat format = AppDefinition.ContainerImageFormat.Docker;
		private String orgName = "springcloudstream";
		private boolean enableMetadata = true;

		public AppDefinition.ContainerImageFormat getFormat() {
			return format;
		}

		public void setFormat(AppDefinition.ContainerImageFormat format) {
			this.format = format;
		}

		public String getOrgName() {
			return orgName;
		}

		public void setOrgName(String orgName) {
			this.orgName = orgName;
		}

		public boolean isEnableMetadata() {
			return enableMetadata;
		}

		public void setEnableMetadata(boolean enableMetadata) {
			this.enableMetadata = enableMetadata;
		}
	}
}
