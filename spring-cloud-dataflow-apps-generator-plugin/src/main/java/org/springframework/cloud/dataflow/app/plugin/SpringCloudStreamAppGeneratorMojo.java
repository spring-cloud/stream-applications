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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.springframework.cloud.dataflow.app.plugin.generator.AppDefinition;
import org.springframework.cloud.dataflow.app.plugin.generator.BinderDefinition;
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

	//@Parameter
	//private ContainerImage containerImage = new ContainerImage();

	@Parameter(required = true)
	private Application application;

	//@Parameter
	//List<String> additionalAppProperties;

	//@Parameter
	//List<String> metadataSourceTypeFilters = new ArrayList<>();
	//
	//@Parameter
	//List<String> metadataNameFilters = new ArrayList<>();

	//@Parameter
	//List<Dependency> boms = new ArrayList<>();

	//@Parameter
	//List<Dependency> dependencies = new ArrayList<>();

	@Parameter
	Global global = new Global();
	//@Parameter
	//List<Dependency> globalDependencies = new ArrayList<>();
	//
	//@Parameter
	//List<Plugin> additionalPlugins = new ArrayList<>();

	@Parameter
	Map<String, Binder> binders = new HashMap<>();

	// Versions
	//@Parameter(defaultValue = "2.3.3", required = true)
	//private String bootVersion;

//	@Parameter(defaultValue = "${app-metadata-maven-plugin-version}")
//	private String appMetadataMavenPluginVersion;

	@Override
	public void execute() throws MojoFailureException, MojoExecutionException {

		AppDefinition app = new AppDefinition();
		String bootVersion = StringUtils.isEmpty(this.application.getBootVersion()) ?
				this.global.getApplication().getBootVersion() : this.application.getBootVersion();
		if (StringUtils.isEmpty(bootVersion)) {
			throw new MojoExecutionException("The application.bootVersion parameter is required!");
		}
		app.setSpringBootVersion(bootVersion);

		String applicationName = StringUtils.isEmpty(this.application.getName()) ?
				this.global.getApplication().getName() : this.application.getName();
		if (StringUtils.isEmpty(applicationName)) {
			throw new MojoExecutionException("The application.name parameter is required!");
		}
		app.setName(applicationName);

		AppDefinition.AppType applicationType = (this.application.getType() == null) ?
				this.global.getApplication().getType() : this.application.getType();
		if (applicationType == null) {
			throw new MojoExecutionException("The application.type parameter is required!");
		}
		app.setType(applicationType);

		String applicationVersion = StringUtils.isEmpty(this.application.getVersion()) ?
				this.global.getApplication().getVersion() : this.application.getVersion();
		if (StringUtils.isEmpty(applicationVersion)) {
			throw new MojoExecutionException("The application.version parameter is required!");
		}
		app.setVersion(applicationVersion);

		String applicationConfigClass = StringUtils.isEmpty(this.application.getConfigClass()) ?
				this.global.getApplication().getConfigClass() : this.application.getConfigClass();
		app.setConfigClass(applicationConfigClass); // TODO is applicationConfigClass a required parameter?

		String applicationFunctionDefinition = StringUtils.isEmpty(this.application.getFunctionDefinition()) ?
				this.global.getApplication().getFunctionDefinition() : this.application.getFunctionDefinition();
		app.setFunctionDefinition(applicationFunctionDefinition); //TODO is applicationFunctionDefinition required?

		String metadataMavenPluginVersion = StringUtils.isEmpty(this.application.getMetadata().getMavenPluginVersion()) ?
				this.global.getApplication().getMetadata().getMavenPluginVersion() : this.application.getMetadata().getMavenPluginVersion();
		if (StringUtils.isEmpty(metadataMavenPluginVersion)) {
			throw new MojoExecutionException("The application.metadata.mavenPluginVersion parameter is required!");
		}
		app.getMetadata().setMavenPluginVersion(metadataMavenPluginVersion);

		List<String> allSourceTypeFilters = new ArrayList<>(this.global.getApplication().getMetadata().getSourceTypeFilters());
		allSourceTypeFilters.addAll(this.application.getMetadata().getSourceTypeFilters());
		List<String> allNamedFilters = new ArrayList<>(this.global.getApplication().getMetadata().getNameFilters());
		allNamedFilters.addAll(this.application.getMetadata().getNameFilters());

		this.populateVisiblePropertiesFromFile(allSourceTypeFilters, allNamedFilters);

		if (!CollectionUtils.isEmpty(allSourceTypeFilters)) {
			app.getMetadata().setSourceTypeFilters(allSourceTypeFilters);
		}

		if (!CollectionUtils.isEmpty(allNamedFilters)) {
			app.getMetadata().setNameFilters(allNamedFilters);
		}

		// Application properties
		List<String> allApplicationProperties = new ArrayList<>(this.global.getApplication().getProperties());
		allApplicationProperties.addAll(this.application.getProperties());
		app.setProperties(allApplicationProperties);

		// BOM
		List<Dependency> allManagedDependencies = new ArrayList<>(this.global.getApplication().getMaven().getDependencyManagement());
		allManagedDependencies.addAll(this.application.getMaven().getDependencyManagement());
		app.getMaven().setDependencyManagement(allManagedDependencies.stream()
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
		List<Dependency> allDependencies = new ArrayList<>(this.global.getApplication().getMaven().getDependencies());
		allDependencies.addAll(this.application.getMaven().getDependencies());
		app.getMaven().setDependencies(allDependencies.stream()
				.map(MavenXmlWriter::toXml)
				.map(xml -> MavenXmlWriter.indent(xml, 8))
				.collect(Collectors.toList()));

		// Plugins
		List<Plugin> allPlugins = new ArrayList<>(this.global.getApplication().getMaven().getPlugins());
		allPlugins.addAll(this.application.getMaven().getPlugins());
		app.getMaven().setPlugins(allPlugins.stream()
				.map(MavenXmlWriter::toXml)
				.map(d -> MavenXmlWriter.indent(d, 12))
				.collect(Collectors.toList()));

		// Container Image configuration
		AppDefinition.ContainerImageFormat containerImageFormat = (this.application.getContainerImage().getFormat() != null) ?
				this.application.getContainerImage().getFormat() : this.global.getApplication().getContainerImage().getFormat();
		containerImageFormat = (containerImageFormat == null) ? AppDefinition.ContainerImageFormat.Docker : containerImageFormat;
		app.getContainerImage().setFormat(containerImageFormat);

		// TODO how to choose between global an app metadata enabling?
		app.getContainerImage().setEnableMetadata(this.application.getContainerImage().isEnableMetadata());

		if (StringUtils.hasText(this.application.getContainerImage().getOrgName())) {
			app.getContainerImage().setOrgName(this.application.getContainerImage().getOrgName());
		}
		else if (StringUtils.hasText(this.global.getApplication().getContainerImage().getOrgName())) {
			app.getContainerImage().setOrgName(this.global.getApplication().getContainerImage().getOrgName());
		}

		app.getContainerImage().setTag(applicationVersion);

		// Generator Properties
		ProjectGeneratorProperties generatorProperties = new ProjectGeneratorProperties();

		Map<String, Binder> allBinders = new HashMap<>();
		allBinders.putAll(this.global.getBinders());
		allBinders.putAll(this.binders);

		generatorProperties.setBinders(allBinders.entrySet().stream()
				.map(es -> {
					BinderDefinition bd = new BinderDefinition();
					bd.setName(es.getKey());
					bd.setProperties(es.getValue().getProperties());
					bd.getMaven().setProperties(es.getValue().getMaven().getProperties());
					bd.getMaven().setDependencies(
							es.getValue().getMaven().getDependencies().stream()
									.map(MavenXmlWriter::toXml)
									.map(xml -> MavenXmlWriter.indent(xml, 8))
									.collect(Collectors.toList()));
					bd.getMaven().setManagedDependencies(
							es.getValue().getMaven().getDependencyManagement().stream()
									.map(MavenXmlWriter::toXml)
									.map(xml -> MavenXmlWriter.indent(xml, 8))
									.collect(Collectors.toList()));
					bd.getMaven().setPlugins(
							es.getValue().getMaven().getPlugins().stream()
									.map(MavenXmlWriter::toXml)
									.map(d -> MavenXmlWriter.indent(d, 12))
									.collect(Collectors.toList()));
					return bd;
				})
				.collect(Collectors.toList()));

		generatorProperties.setOutputFolder(new File(this.generatedProjectHome));
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

	public static class Global {

		private final Application application = new Application();

		private final Map<String, Binder> binders = new HashMap<>();

		public Application getApplication() {
			return application;
		}

		public Map<String, Binder> getBinders() {
			return binders;
		}
	}

	public static class Application {

		private String name;
		private String version;
		private AppDefinition.AppType type;
		private String configClass;
		private String functionDefinition;
		private String bootVersion;

		private List<String> properties = new ArrayList<>();

		private final Maven maven = new Maven();

		private final Metadata metadata = new Metadata();

		private final ContainerImage containerImage = new ContainerImage();

		public List<String> getProperties() {
			return properties;
		}

		public void setProperties(List<String> properties) {
			this.properties = properties;
		}

		public Maven getMaven() {
			return maven;
		}

		public Metadata getMetadata() {
			return metadata;
		}

		public ContainerImage getContainerImage() {
			return containerImage;
		}

		public String getBootVersion() {
			return bootVersion;
		}

		public void setBootVersion(String bootVersion) {
			this.bootVersion = bootVersion;
		}

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

		public static class Metadata {
			private List<String> sourceTypeFilters = new ArrayList<>();
			private List<String> nameFilters = new ArrayList<>();
			private String mavenPluginVersion;

			public List<String> getSourceTypeFilters() {
				return sourceTypeFilters;
			}

			public void setSourceTypeFilters(List<String> sourceTypeFilters) {
				this.sourceTypeFilters = sourceTypeFilters;
			}

			public List<String> getNameFilters() {
				return nameFilters;
			}

			public void setNameFilters(List<String> nameFilters) {
				this.nameFilters = nameFilters;
			}

			public String getMavenPluginVersion() {
				return mavenPluginVersion;
			}

			public void setMavenPluginVersion(String mavenPluginVersion) {
				this.mavenPluginVersion = mavenPluginVersion;
			}
		}
	}

	public static class ContainerImage {
		private AppDefinition.ContainerImageFormat format = null;
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

	public static class Binder {

		/**
		 * Application properties specific for the binder.
		 */
		private List<String> properties = new ArrayList<>();

		/**
		 * Maven configuraiton specific for the binder.
		 */
		private final Maven maven = new Maven();


		public Maven getMaven() {
			return maven;
		}

		public List<String> getProperties() {
			return properties;
		}

		public void setProperties(List<String> properties) {
			this.properties = properties;
		}
	}

	public static class Maven {

		private List<String> properties = new ArrayList<>();

		private List<Dependency> dependencyManagement = new ArrayList<>();

		private List<Dependency> dependencies = new ArrayList<>();

		private List<Plugin> plugins = new ArrayList<>();

		public List<String> getProperties() {
			return properties;
		}

		public void setProperties(List<String> properties) {
			this.properties = properties;
		}

		public List<Dependency> getDependencyManagement() {
			return dependencyManagement;
		}

		public void setDependencyManagement(List<Dependency> dependencyManagement) {
			this.dependencyManagement = dependencyManagement;
		}

		public List<Dependency> getDependencies() {
			return dependencies;
		}

		public void setDependencies(List<Dependency> dependencies) {
			this.dependencies = dependencies;
		}

		public List<Plugin> getPlugins() {
			return plugins;
		}

		public void setPlugins(List<Plugin> plugins) {
			this.plugins = plugins;
		}
	}

}
