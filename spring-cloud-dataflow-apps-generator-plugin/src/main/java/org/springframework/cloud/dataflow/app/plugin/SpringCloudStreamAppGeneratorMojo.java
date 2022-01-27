/*
 * Copyright 2020-2022 the original author or authors.
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
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryBase;
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
 * @author Soby Chacko
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
	private Global global = new Global();

	@Parameter(required = true)
	private Application application;

	@Parameter
	private Map<String, Binder> binders = new HashMap<>();

	@Override
	public void execute() throws MojoFailureException, MojoExecutionException {

		// ----------------------------------------------------------------------------------------------------------
		//                               Application Configuration
		// ----------------------------------------------------------------------------------------------------------

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

		String applicationGroupId = StringUtils.isEmpty(this.application.getGroupId()) ?
				this.global.getApplication().getGroupId() : this.application.getGroupId();
		app.setGroupId(applicationGroupId); // optional parameter

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
		Map<String, String> allApplicationProperties = new TreeMap<>(this.global.getApplication().getProperties());
		allApplicationProperties.putAll(this.application.getProperties());
		app.setProperties(allApplicationProperties.entrySet().stream()
				.map(e -> e.getKey() + "=" + (StringUtils.hasText(e.getValue()) ?
						e.getValue().replaceAll("^\"|\"$", "") : ""))
				.collect(Collectors.toList()));

		// Maven properties
		Map<String, String> allMavenProperties = new TreeMap<>(this.global.getApplication().getMaven().getProperties());
		allMavenProperties.putAll(this.application.getMaven().getProperties());
		app.getMaven().setProperties(allMavenProperties.entrySet().stream()
				.map(es -> "<" + es.getKey() + ">" + (StringUtils.hasText(es.getValue()) ?
						es.getValue().replaceAll("^\"|\"$", "") : "") + "</" + es.getKey() + ">")
				.collect(Collectors.toList()));

		//Maven BOM. For DependencyManagement it is important to retain the exact definition order!
		// Override the global dependencies with matching app dependency definitions. Retain the definition order!
		List<Dependency> allDeps = this.global.getApplication().getMaven().getDependencyManagement().stream()
				.map(globalDep -> this.application.getMaven().getDependencyManagement().stream()
						.filter(d -> isSameArtifact(globalDep, d))
						.findFirst().orElse(globalDep))
				.collect(Collectors.toList());

		// Add remaining app dependencies that have not be used for overriding. Retain the order!
		this.application.getMaven().getDependencyManagement()
				.forEach(appDep -> {
					if (allDeps.stream().noneMatch(d -> isSameArtifact(appDep, d))) {
						allDeps.add(appDep);
					}
				});

		app.getMaven().setDependencyManagement(allDeps.stream()
				.filter(Objects::nonNull)
				.peek(dependency -> {
					dependency.setScope("import");
					dependency.setType("pom");
				})
				.map(MavenXmlWriter::toXml)
				.map(xml -> MavenXmlWriter.indent(xml, 12))
				.collect(Collectors.toList()));

		//Maven Dependencies
		Map<String, Dependency> allDependenciesMap = this.global.getApplication().getMaven().getDependencies().stream()
				.collect(Collectors.toMap(d -> d.getGroupId() + ":" + d.getArtifactId(), d -> d));
		// Ensure that for dependencies with same maven coordinates that application definition overrides the global one.
		allDependenciesMap.putAll(this.application.getMaven().getDependencies().stream()
				.collect(Collectors.toMap(d -> d.getGroupId() + ":" + d.getArtifactId(), d -> d)));
		app.getMaven().setDependencies(allDependenciesMap.values().stream()
				.map(MavenXmlWriter::toXml)
				.map(xml -> MavenXmlWriter.indent(xml, 8))
				.collect(Collectors.toList()));

		//Maven Plugins
		Map<String, Plugin> allPluginsMap = this.global.getApplication().getMaven().getPlugins().stream()
				.collect(Collectors.toMap(p -> p.getGroupId() + ":" + p.getArtifactId(), p -> p));
		// Ensure that for plugins with same maven coordinates that application definition overrides the global one.
		allPluginsMap.putAll(this.application.getMaven().getPlugins().stream()
				.collect(Collectors.toMap(p -> p.getGroupId() + ":" + p.getArtifactId(), p -> p)));
		app.getMaven().setPlugins(allPluginsMap.values().stream()
				.map(MavenXmlWriter::toXml)
				.map(d -> MavenXmlWriter.indent(d, 12))
				.collect(Collectors.toList()));

		// Maven Repositories
		Map<String, Repository> allRepositoriesMap = this.global.getApplication().getMaven().getRepositories().stream()
				.collect(Collectors.toMap(RepositoryBase::getId, r -> r));
		// Ensure that for repos with same maven coordinates that application definition overrides the global one.
		allRepositoriesMap.putAll(this.application.getMaven().getRepositories().stream()
				.collect(Collectors.toMap(RepositoryBase::getId, r -> r)));
		app.getMaven().setRepositories(allRepositoriesMap.values().stream()
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

		if (StringUtils.hasText(this.application.getContainerImage().getBaseImage())) {
			app.getContainerImage().setBaseImage(this.application.getContainerImage().getBaseImage());
		}
		else if (StringUtils.hasText(this.global.getApplication().getContainerImage().getBaseImage())) {
			app.getContainerImage().setBaseImage(this.global.getApplication().getContainerImage().getBaseImage());
		}

		// Generator Properties
		ProjectGeneratorProperties generatorProperties = new ProjectGeneratorProperties();

		// ----------------------------------------------------------------------------------------------------------
		//                               Binders Configuration
		// ----------------------------------------------------------------------------------------------------------

		Map<String, Binder> allBinders = new HashMap<>(this.global.getBinders());
		// Note: The Application concrete Binder definitions will replace any global
		// Binder definitions with the same names. E.g. you can replace but not extend global binders configurations.
		allBinders.putAll(this.binders);

		if (CollectionUtils.isEmpty(allBinders)) {
			throw new MojoExecutionException("At least one Binder configuration is required!");
		}

		// Converts Mojo's Binder configurations into BinderDefinitions.
		List<BinderDefinition> bindersDefinitions = allBinders.entrySet().stream()
				.map(es -> {
					BinderDefinition bd = new BinderDefinition();
					bd.setName(es.getKey());
					bd.setProperties(es.getValue().getProperties());
					bd.getMaven().setProperties(es.getValue().getMaven().getProperties()
							.entrySet().stream()
							.map(pes -> "<" + pes.getKey() + ">" + pes.getValue() + "</" + pes.getKey() + ">")
							.collect(Collectors.toList()));
					bd.getMaven().setDependencies(
							es.getValue().getMaven().getDependencies().stream()
									.map(MavenXmlWriter::toXml)
									.map(xml -> MavenXmlWriter.indent(xml, 8))
									.collect(Collectors.toList()));
					bd.getMaven().setDependencyManagement(
							es.getValue().getMaven().getDependencyManagement().stream()
									.filter(Objects::nonNull)
									.peek(dependency -> {
										dependency.setScope("import");
										dependency.setType("pom");
									})
									.map(MavenXmlWriter::toXml)
									.map(xml -> MavenXmlWriter.indent(xml, 12))
									.collect(Collectors.toList()));
					bd.getMaven().setPlugins(
							es.getValue().getMaven().getPlugins().stream()
									.map(MavenXmlWriter::toXml)
									.map(d -> MavenXmlWriter.indent(d, 12))
									.collect(Collectors.toList()));
					bd.getMaven().setRepositories(es.getValue().getMaven().getRepositories().stream()
							.map(MavenXmlWriter::toXml)
							.map(d -> MavenXmlWriter.indent(d, 12))
							.collect(Collectors.toList()));

					return bd;
				})
				.collect(Collectors.toList());

		app.setBootPluginConfiguration(this.application.getBootPluginConfiguration());

		// ----------------------------------------------------------------------------------------------------------
		//                                 Project Generator
		// ----------------------------------------------------------------------------------------------------------

		generatorProperties.setAppDefinition(app);
		generatorProperties.setBinderDefinitions(bindersDefinitions);
		generatorProperties.setOutputFolder(new File(this.generatedProjectHome));
		generatorProperties.setProjectResourcesDirectory(this.projectResourcesDir);

		try {
			ProjectGenerator.getInstance().generate(generatorProperties);
		}
		catch (IOException e) {
			throw new MojoFailureException("Project generation failure", e);
		}
	}

	private boolean isSameArtifact(Dependency dep1, Dependency dep2) {
		return dep1.getGroupId().equalsIgnoreCase(dep2.getGroupId())
				&& dep1.getArtifactId().equalsIgnoreCase(dep2.getArtifactId());
	}

	/**
	 * If the visible metadata properties files are provided in the source project, add theirs' type and name filters
	 * to the existing visible configurations.
	 *
	 * @param sourceTypeFilters existing source type filters configured via the mojo parameter.
	 * @param nameFilters       existing name filters configured via the mojo parameter.
	 */
	private void populateVisiblePropertiesFromFile(List<String> sourceTypeFilters, List<String> nameFilters) {
		if (this.projectResourcesDir == null || !projectResourcesDir.exists()) {
			return;
		}
		Optional<Properties> optionalProperties =
				loadVisiblePropertiesFromResource(FileUtils.getFile(projectResourcesDir, "META-INF",
						VISIBLE_PROPERTIES_FILE_NAME));
		optionalProperties.ifPresent(properties -> {
			addToFilters(properties.getProperty(CONFIGURATION_PROPERTIES_CLASSES), sourceTypeFilters);
			addToFilters(properties.getProperty(CONFIGURATION_PROPERTIES_NAMES), nameFilters);
		});
		if (optionalProperties.isPresent()) {
			return;
		}

		loadVisiblePropertiesFromResource(FileUtils.getFile(
				projectResourcesDir, "META-INF", DEPRECATED_WHITELIST_FILE_NAME))
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

	/**
	 * Section that allow to define Application and Binder configurations common across multiple applications
	 * and binders.
	 */
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

	/**
	 * Allow defining the target application configuration, excluding the Binder specific configurations.
	 */
	public static class Application {

		/**
		 * Unique name of the application for the given type.
		 * The generated binder specific, applications have names that combine this application name, type as well
		 * as the name of the binder they are produced for.
		 */
		private String name;

		/**
		 * Application type. Cloud be of type (source, processor, sink).
		 */
		private AppDefinition.AppType type;

		/**
		 * Version to be used for the generated application.
		 */
		private String version;

		/**
		 * The Spring Boot version the generated application is configured to inherit from.
		 */
		private String bootVersion;

		/**
		 * Spring configuration class used to instantiate the application.
		 */
		private String configClass;

		/**
		 * Group Id to use with the application.
		 */
		private String groupId = "org.springframework.cloud.stream.app";

		/**
		 * The Spring Cloud Function definition. Could be a composition definition as well.
		 */
		private String functionDefinition;

		/**
		 * Custom application properties to contribute to the generated application.properties file.
		 */
		private Map<String, String> properties = new HashMap<>();

		/**
		 * Parameters used to tune the Application metadata generation.
		 */
		private final Metadata metadata = new Metadata();

		private final ContainerImage containerImage = new ContainerImage();

		private final Maven maven = new Maven();

		private String bootPluginConfiguration;

		public String getBootPluginConfiguration() {
			return bootPluginConfiguration;
		}

		public void setBootPluginConfiguration(String bootPluginConfiguration) {
			this.bootPluginConfiguration = bootPluginConfiguration;
		}

		public Map<String, String> getProperties() {
			return properties;
		}

		public void setProperties(Map<String, String> properties) {
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

		public String getGroupId() {
			return groupId;
		}

		public void setGroupId(String appGroupId) {
			this.groupId = appGroupId;
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

		/**
		 * Parameters used to tune the Application metadata generation. It allows to set a type or name based filters
		 * that filer-in the subset all application configuration metadata. In addition to the explicitly specified
		 * filters, the Mojo automatically populates the filters with values found in the
		 * 'dataflow-configuration-metadata-whitelist.properties" or 'dataflow-configuration-metadata.properties' files.
		 *
		 * You can also specify the version of the metadata generation plugin.
		 */
		public static class Metadata {

			/**
			 * list of source types from application's configuration metadata to include in the visible metadata.
			 */
			private List<String> sourceTypeFilters = new ArrayList<>();

			/**
			 * list of property names from application's configuration metadata to include in the visible metadata.
			 */
			private List<String> nameFilters = new ArrayList<>();

			/**
			 * Version of the metadata maven plugin to be used.
			 */
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

		/**
		 * Configuration for injection the application's visible, configuration metadata into the Container image.
		 */
		public static class ContainerImage {

			/**
			 * Target container image. Docker and OCI are supported. If not specified the Docker is selected.
			 */
			private AppDefinition.ContainerImageFormat format = null;

			/**
			 * Target container image organization name.
			 */
			private String orgName;

			/**
			 * Base images to be used by the target container image.
			 */
			private String baseImage;

			/**
			 * Enable or disable the inclusion of application's metadata into the image's labels.
			 */
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

			public String getBaseImage() {
				return baseImage;
			}

			public void setBaseImage(String baseImage) {
				this.baseImage = baseImage;
			}
		}
	}

	/**
	 * Binder specific properties to be applied in addition to the {@link Application} parameters.
	 */
	public static class Binder {

		/**
		 * Application properties specific for the binder. Contributed to the generated application.properties file.
		 */
		private List<String> properties = new ArrayList<>();

		/**
		 * Binder specific maven configurations. Applied in addition to the {@link Application}'s maven configurations.
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

	/**
	 * Maven configuration used inside the {@link Application} and {@link Binder} configurations.
	 */
	public static class Maven {

		/**
		 * Custom maven properties. Contributed to the generated POM's properties section.
		 */
		private Map<String, String> properties = new HashMap<>();

		/**
		 * Custom maven management dependencies. Contributed to the generated POM's dependencyManagement section.
		 */
		private List<Dependency> dependencyManagement = new ArrayList<>();

		/**
		 * Custom maven dependencies. Contributed to the generated POM's dependencies section.
		 */
		private List<Dependency> dependencies = new ArrayList<>();

		/**
		 * Custom maven plugins. Contributed to the generated POM's plugins section.
		 *
		 * Note: if the plugin definition uses a configuration block then the content must be wrapped within a
		 * <pre>@code{
		 * <![CDATA[ ... ]]>
		 * }</pre> section! Find more: {@link MavenXmlWriter#toXml(Plugin)}.
		 */
		private List<Plugin> plugins = new ArrayList<>();

		/**
		 * Custom maven repositories. Contributed to the generated POM's repositories section.
		 */
		private List<Repository> repositories = new ArrayList<>();

		public Map<String, String> getProperties() {
			return properties;
		}

		public void setProperties(Map<String, String> properties) {
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

		public List<Repository> getRepositories() {
			return repositories;
		}

		public void setRepositories(List<Repository> repositories) {
			this.repositories = repositories;
		}
	}

}
