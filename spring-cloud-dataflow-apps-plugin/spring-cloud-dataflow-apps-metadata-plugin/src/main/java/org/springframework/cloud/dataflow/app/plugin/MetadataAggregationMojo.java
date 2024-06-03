/*
 * Copyright 2017-2020 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemHint;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import static org.springframework.boot.configurationprocessor.metadata.ItemHint.ValueHint;
import static org.springframework.boot.configurationprocessor.metadata.ItemHint.ValueProvider;

/**
 * A maven plugin that will gather all Spring Boot metadata files from all transitive dependencies and will aggregate
 * them in one metadata-only artifact.
 *
 * @author Eric Bottard
 * @author David Turanski
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 */
@Mojo(
		name = "aggregate-metadata",
		requiresDependencyResolution = ResolutionScope.RUNTIME,
		defaultPhase = LifecyclePhase.COMPILE
)
public class MetadataAggregationMojo extends AbstractMojo {

	static final String METADATA_PATH = "META-INF/spring-configuration-metadata.json";

	static final String VISIBLE_PROPERTIES_PATH = "META-INF/dataflow-configuration-metadata.properties";

	static final String DEPRECATED_WHITELIST_PATH = "META-INF/dataflow-configuration-metadata-whitelist.properties";

	static final String DEPRECATED_BACKUP_WHITELIST_PATH = "META-INF/spring-configuration-metadata-whitelist.properties";

	static final String CONFIGURATION_PROPERTIES_CLASSES = "configuration-properties.classes";

	static final String CONFIGURATION_PROPERTIES_NAMES = "configuration-properties.names";

	static final String CONFIGURATION_PROPERTIES_INBOUND_PORTS = "configuration-properties.inbound-ports";

	static final String CONFIGURATION_PROPERTIES_OUTBOUND_PORTS = "configuration-properties.outbound-ports";

	static final String SPRING_CLOUD_FUNCTION_DEFINITION = "spring.cloud.function.definition";

	static final String SPRING_CLOUD_STREAM_FUNCTION_DEFINITION = "spring.cloud.stream.function.definition";

	static final String SPRING_CLOUD_STREAM_FUNCTION_BINDINGS = "spring.cloud.stream.function.bindings";

	static final String SPRING_CLOUD_DATAFLOW_PORT_MAPPING_PROPERTIES = "dataflow-configuration-port-mapping.properties";

	static final String SPRING_CLOUD_DATAFLOW_OPTION_GROUPS_PROPERTIES = "dataflow-configuration-option-groups.properties";

	private static final Set<String> KNOWN_PROBLEMATIC_ENUMS = new HashSet<>();
	static {
		KNOWN_PROBLEMATIC_ENUMS.add("org.springframework.boot.autoconfigure.data.jdbc.JdbcDatabaseDialect");
	}

	@Parameter(defaultValue = "${project}")
	private MavenProject mavenProject;

	@Parameter(defaultValue = "metadata")
	private String classifier;

	@Component
	private MavenProjectHelper projectHelper;

	@Parameter
	private boolean storeFilteredMetadata;

	@Parameter
	private MetadataFilter metadataFilter;

	private final JsonMarshaller jsonMarshaller = new JsonMarshaller();

	public void execute() throws MojoExecutionException {
		Result result = new Result(gatherConfigurationMetadata(null), gatherVisibleMetadata());
		produceArtifact(result);

		if (storeFilteredMetadata) {
			getLog().debug("propertyClassFilter: " + metadataFilter);
			if (metadataFilter == null) {
				metadataFilter = new MetadataFilter();
			}
			if (result.visible.containsKey(CONFIGURATION_PROPERTIES_CLASSES)) {
				String[] sourceTypes = result.visible.getProperty(CONFIGURATION_PROPERTIES_CLASSES, "").split(",");
				if (sourceTypes != null && sourceTypes.length > 0) {
					if (metadataFilter.getSourceTypes() == null) {
						metadataFilter.setSourceTypes(new ArrayList<>());
					}
					for (String sourceType : sourceTypes) {
						sourceType = sourceType.trim();
						if (!metadataFilter.getSourceTypes().contains(sourceType)) {
							metadataFilter.getSourceTypes().add(sourceType);
						}
					}
				}
			}

			if (result.visible.containsKey(CONFIGURATION_PROPERTIES_NAMES)) {
				String[] names = result.visible.getProperty(CONFIGURATION_PROPERTIES_NAMES, "").split(",");
				if (names != null && names.length > 0) {
					if (metadataFilter.getNames() == null) {
						metadataFilter.setNames(new ArrayList<>());
					}
					for (String name : names) {
						name = name.trim();
						if (!metadataFilter.getNames().contains(name)) {
							metadataFilter.getNames().add(name);
						}
					}
				}
			}

			if (result.visible.containsKey(CONFIGURATION_PROPERTIES_CLASSES)) {
				String[] sourceTypes = result.visible.getProperty(CONFIGURATION_PROPERTIES_CLASSES, "").split(",");
				metadataFilter.getSourceTypes().addAll(Arrays.asList(sourceTypes));
			}

			storeFilteredMetadata();
		}
		//Add port mapping configuration based on the application configuration.
		storeInboundOutboundPortMappingConfigurations(result.getPortMappingProperties());
	}

	/**
	 * Store pre-filtered and json-escaped metadata into a property file.
	 */
	private void storeFilteredMetadata() throws MojoExecutionException {
		File projectMetaInfFolder = new File(mavenProject.getBuild().getOutputDirectory(), "META-INF");
		if (!projectMetaInfFolder.exists()) {
			if (!projectMetaInfFolder.mkdir()) {
				throw new MojoExecutionException("Error creating META-INF folder for port mapping file!");
			}
		}
		try (FileWriter fileWriter = new FileWriter(
				new File(projectMetaInfFolder, "spring-configuration-metadata-encoded.properties"))) {
			ConfigurationMetadata metadata = gatherConfigurationMetadata(metadataFilter);
			String escapedJson = StringEscapeUtils.escapeJson(toJson(metadata));
			fileWriter.write("org.springframework.cloud.dataflow.spring.configuration.metadata.json=" + escapedJson);
		}
		catch (IOException e) {
			throw new MojoExecutionException("Error creating file ", e);
		}
	}

	private void storeInboundOutboundPortMappingConfigurations(Properties properties) throws MojoExecutionException {
		File projectMetaInfFolder = new File(mavenProject.getBuild().getOutputDirectory(), "META-INF");
		if (!projectMetaInfFolder.exists()) {
			if (!projectMetaInfFolder.mkdir()) {
				throw new MojoExecutionException("Error creating META-INF folder for port mapping file!");
			}
		}
		try (FileWriter fileWriter = new FileWriter(
				new File(projectMetaInfFolder, SPRING_CLOUD_DATAFLOW_PORT_MAPPING_PROPERTIES))) {
			properties.store(fileWriter, "Spring Cloud DataFlow Port Mapping");
		}
		catch (IOException e) {
			throw new MojoExecutionException("Error creating file ", e);
		}
	}

	private String toJson(ConfigurationMetadata metadata) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		jsonMarshaller.write(metadata, baos);
		String json = baos.toString();

		// Hack to workaround the https://github.com/mojohaus/properties-maven-plugin/issues/27 and
		// https://github.com/mojohaus/properties-maven-plugin/pull/38 properties-maven-plugin issues.
		json = json.replaceAll("\\$\\{", "{");

		return json;
	}

	/**
	 * Read all existing metadata from this project runtime dependencies and merge them in a single object.
	 */
	/*default*/ Properties gatherVisibleMetadata() throws MojoExecutionException {
		Properties visible = new Properties();
		List<String> inboundPorts = new ArrayList<>();
		List<String> outboundPorts = new ArrayList<>();
		try {
			for (String path : mavenProject.getRuntimeClasspathElements()) {
				if (Files.isDirectory(Paths.get(path))) {
					for (String visibleProperties : new String[] { VISIBLE_PROPERTIES_PATH, DEPRECATED_WHITELIST_PATH,
							DEPRECATED_BACKUP_WHITELIST_PATH }) {
						Optional<Properties> properties;
						properties = getVisibleFromFile(Paths.get(path, visibleProperties));
						if (properties.isPresent()) {
							if (!visibleProperties.equals(VISIBLE_PROPERTIES_PATH)) {
								getLog().warn("Use of " + visibleProperties + " is deprecated." +
										" Please use " + VISIBLE_PROPERTIES_PATH);

							}
							visible = properties.get();
							break;
						}
					}
					File dir = new File(path);
					for (File file : dir.listFiles()) {
						Properties properties = new Properties();
						if (file.isFile() && file.canRead() && file.getName().endsWith(".properties")) {
							try (InputStream is = new FileInputStream(file)) {
								properties.load(is);
							}
						}
						if (file.isFile() && file.canRead() && (file.getName().endsWith(".yaml") || file.getName()
								.endsWith(".yml"))) {
							YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
							yamlPropertiesFactoryBean.setResources(new FileSystemResource(file));
							properties = yamlPropertiesFactoryBean.getObject();
						}
						if (!properties.isEmpty()) {
							String functionDefinitions = null;
							if (properties.containsKey(SPRING_CLOUD_FUNCTION_DEFINITION)) {
								functionDefinitions = properties.getProperty(SPRING_CLOUD_FUNCTION_DEFINITION);
							}
							else if (properties.containsKey(SPRING_CLOUD_STREAM_FUNCTION_DEFINITION)) {
								functionDefinitions = properties
										.getProperty(SPRING_CLOUD_STREAM_FUNCTION_DEFINITION);
							}
							for (String functionDefinition : StringUtils
									.delimitedListToStringArray(functionDefinitions, ";")) {
								if (functionDefinition != null) {
									for (Object propertyKey : properties.keySet()) {
										if (((String) propertyKey).startsWith(
												String.format("%s.%s-in-", SPRING_CLOUD_STREAM_FUNCTION_BINDINGS,
														functionDefinition))) {
											inboundPorts.add(properties.getProperty((String) propertyKey));
										}
										if (((String) propertyKey).startsWith(
												String.format("%s.%s-out-", SPRING_CLOUD_STREAM_FUNCTION_BINDINGS,
														functionDefinition))) {
											outboundPorts.add(properties.getProperty((String) propertyKey));
										}
									}
								}
							}
						}
					}
				}
				else {
					try (ZipFile zipFile = new ZipFile(new File(path))) {
						ZipEntry entry;
						for (String zipEntry : new String[] { VISIBLE_PROPERTIES_PATH, DEPRECATED_WHITELIST_PATH,
								DEPRECATED_BACKUP_WHITELIST_PATH }) {
							entry = zipFile.getEntry(zipEntry);
							if (entry != null) {
								if (!zipEntry.equals(VISIBLE_PROPERTIES_PATH)) {
									getLog().warn("Use of " + zipEntry + " is deprecated." +
											" Please use " + VISIBLE_PROPERTIES_PATH);
								}
								visible = getVisibleFromZipFile(visible, path, zipFile, entry);
								break;
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			throw new MojoExecutionException("Exception trying to read metadata from dependencies of project", e);
		}
		if (!inboundPorts.isEmpty()) {
			visible.put(CONFIGURATION_PROPERTIES_INBOUND_PORTS,
					StringUtils.arrayToCommaDelimitedString(inboundPorts.toArray(new String[0])));
		}
		if (!outboundPorts.isEmpty()) {
			visible.put(CONFIGURATION_PROPERTIES_OUTBOUND_PORTS,
					StringUtils.arrayToCommaDelimitedString(outboundPorts.toArray(new String[0])));
		}
		return visible;
	}

	/*default*/ ConfigurationMetadata gatherConfigurationMetadata(MetadataFilter metadataFilters)
			throws MojoExecutionException {
		ConfigurationMetadata metadata = new ConfigurationMetadata();
		try {
			for (String path : mavenProject.getRuntimeClasspathElements()) {
				File file = new File(path);
				if (file.isDirectory()) {
					File localMetadata = new File(file, METADATA_PATH);
					if (localMetadata.canRead()) {
						try (InputStream is = new FileInputStream(localMetadata)) {
							ConfigurationMetadata depMetadata = jsonMarshaller.read(is);
							depMetadata = filterMetadata(depMetadata, metadataFilters);
							getLog().debug("Merging metadata from " + path);
							addEnumHints(depMetadata, getClassLoader(path));
							metadata.merge(depMetadata);
						}
					}
				}
				else {
					try (ZipFile zipFile = new ZipFile(file)) {
						ZipEntry entry = zipFile.getEntry(METADATA_PATH);
						if (entry != null) {
							try (InputStream inputStream = zipFile.getInputStream(entry)) {
								ConfigurationMetadata depMetadata = jsonMarshaller.read(inputStream);
								depMetadata = filterMetadata(depMetadata, metadataFilters);
								getLog().debug("Merging metadata from " + path);
								addEnumHints(depMetadata, getClassLoader(path));
								metadata.merge(depMetadata);
							}
						}
					}
				}

				// Replace all escaped double quotes by a single one.
				metadata.getItems().stream().forEach(itemMetadata -> {
					if (!StringUtils.isEmpty(itemMetadata.getDescription()) && itemMetadata.getDescription()
							.contains("\"")) {
						itemMetadata.setDescription(itemMetadata.getDescription().replaceAll("\"", "'"));
					}
				});
			}
		}
		catch (Exception e) {
			throw new MojoExecutionException("Exception trying to read metadata from dependencies of project", e);
		}
		return metadata;
	}

	@SuppressWarnings("unchecked")
	private ConfigurationMetadata filterMetadata(ConfigurationMetadata metadata, MetadataFilter metadataFilters) {
		if (metadataFilters == null
				|| (CollectionUtils.isEmpty(metadataFilters.getNames()) && CollectionUtils
				.isEmpty(metadataFilters.getSourceTypes()))) {
			return metadata; // nothing to filter by so take all;
		}

		List<String> sourceTypeFilters = CollectionUtils.isEmpty(metadataFilters.getSourceTypes()) ?
				Collections.EMPTY_LIST : metadataFilters.getSourceTypes();

		List<String> nameFilters = CollectionUtils.isEmpty(metadataFilters.getNames()) ?
				Collections.EMPTY_LIST : metadataFilters.getNames();

		ConfigurationMetadata filteredMetadata = new ConfigurationMetadata();
		List<String> visibleNames = new ArrayList<>();
		for (ItemMetadata itemMetadata : metadata.getItems()) {
			String metadataName = itemMetadata.getName();
			String metadataSourceType = itemMetadata.getSourceType();
			if (StringUtils.hasText(metadataSourceType) && sourceTypeFilters.contains(metadataSourceType.trim())) {
				filteredMetadata.add(itemMetadata);
				visibleNames.add(itemMetadata.getName());
			}
			if (StringUtils.hasText(metadataName) && nameFilters.contains(metadataName.trim())) {
				filteredMetadata.add(itemMetadata);
				visibleNames.add(itemMetadata.getName());
			}

		}

		// copy the hits only for the visible metadata.
		for (ItemHint itemHint : metadata.getHints()) {
			if (itemHint != null && visibleNames.contains(itemHint.getName())) {
				filteredMetadata.add(itemHint);
			}
		}

		return filteredMetadata;
	}

	private Properties getVisibleFromZipFile(Properties visible, String path, ZipFile zipFile, ZipEntry entry)
			throws IOException {
		try (InputStream inputStream = zipFile.getInputStream(entry)) {
			getLog().debug("Merging visible metadata from " + path);
			visible = merge(visible, inputStream);
		}
		return visible;
	}

	private Optional<Properties> getVisibleFromFile(Path visiblePropertiesPath) throws IOException {
		File localVisible = visiblePropertiesPath.toFile();
		if (localVisible.canRead()) {
			Properties visible = new Properties();
			try (InputStream is = new FileInputStream(localVisible)) {
				getLog().debug("!!!! Merging visible metadata from " + visiblePropertiesPath.toString());
				visible = merge(visible, is);
				return Optional.of(visible);
			}
		}
		return Optional.empty();
	}

	Properties merge(Properties visible, InputStream is) throws IOException {
		Properties mergedProperties = new Properties();
		mergedProperties.load(is);

		if (!mergedProperties.containsKey(CONFIGURATION_PROPERTIES_CLASSES) && !mergedProperties
				.containsKey(CONFIGURATION_PROPERTIES_NAMES)) {
			getLog().warn(String.format("Visible properties does not contain any required keys: %s",
					StringUtils.arrayToCommaDelimitedString(new String[] {
							CONFIGURATION_PROPERTIES_CLASSES,
							CONFIGURATION_PROPERTIES_NAMES
					})));
			return visible;
		}

		if (!CollectionUtils.isEmpty(visible)) {
			mergeCommaDelimitedValue(visible, mergedProperties, CONFIGURATION_PROPERTIES_CLASSES);
			mergeCommaDelimitedValue(visible, mergedProperties, CONFIGURATION_PROPERTIES_NAMES);
		}

		return mergedProperties;
	}

	private void mergeCommaDelimitedValue(Properties currentProperties, Properties newProperties, String key) {
		if (currentProperties.containsKey(key) || newProperties.containsKey(key)) {
			Collection<String> values = StringUtils.commaDelimitedListToSet(currentProperties.getProperty(key));
			values.addAll(StringUtils.commaDelimitedListToSet(newProperties.getProperty(key)));
			if (newProperties.containsKey(key)) {
				getLog().debug(String.format("Merging visible property %s=%s", key, newProperties.getProperty(key)));
			}
			newProperties.setProperty(key, StringUtils.collectionToCommaDelimitedString(values));

		}
	}

	/**
	 * Create a jar file with the given metadata and "attach" it to the current maven project.
	 */
	/*default*/ void produceArtifact(Result result) throws MojoExecutionException {
		String artifactLocation = String
				.format("target/%s-%s-%s.jar", mavenProject.getArtifactId(), mavenProject.getVersion(), classifier);
		File output = new File(mavenProject.getBasedir(), artifactLocation);
		try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(output))) {
			ZipEntry entry = new ZipEntry(METADATA_PATH);
			jos.putNextEntry(entry);
			jsonMarshaller.write(result.metadata, jos);

			entry = new ZipEntry(VISIBLE_PROPERTIES_PATH);
			jos.putNextEntry(entry);
			result.visible.store(jos, "Describes visible properties for this app");

			entry = new ZipEntry(DEPRECATED_WHITELIST_PATH);
			jos.putNextEntry(entry);
			result.visible.store(jos, "DEPRECATED: Describes visible properties for this app");

			entry = new ZipEntry(DEPRECATED_BACKUP_WHITELIST_PATH);
			jos.putNextEntry(entry);
			result.visible.store(jos, "DEPRECATED: Describes visible properties for this app");

			entry = new ZipEntry("META-INF/" + SPRING_CLOUD_DATAFLOW_PORT_MAPPING_PROPERTIES);
			jos.putNextEntry(entry);

			entry = new ZipEntry("META-INF/" + SPRING_CLOUD_DATAFLOW_OPTION_GROUPS_PROPERTIES);
			jos.putNextEntry(entry);

			result.getPortMappingProperties().store(jos, "Describes visible port mapping properties for this app");

			getLog().info(String.format("Attaching %s to current project", output.getCanonicalPath()));
			projectHelper.attachArtifact(mavenProject, output, classifier);
		}
		catch (IOException e) {
			throw new MojoExecutionException("Error writing to file", e);
		}
	}

	void addEnumHints(ConfigurationMetadata configurationMetadata, ClassLoader classLoader) {

		Map<String, List<ValueProvider>> providers = new HashMap<>();

		Map<String, ItemHint> itemHints = new HashMap<>();

		for (ItemMetadata property : configurationMetadata.getItems()) {

			if (property.isOfItemType(ItemMetadata.ItemType.PROPERTY)) {

				if (ClassUtils.isPresent(property.getType(), classLoader)) {
					Class<?> clazz = ClassUtils.resolveClassName(property.getType(), classLoader);
					if (clazz.isEnum()) {
						List<ValueHint> valueHints = new ArrayList<>();
						Object[] enumConstants;
						try {
							enumConstants = clazz.getEnumConstants();
						}
						catch (NoClassDefFoundError ex) {
							String enumClass = clazz.getName();
							if (KNOWN_PROBLEMATIC_ENUMS.contains(enumClass)) {
								getLog().info("[EXPECTED] Failed to resolve enum constants for property = " + property + " and class = " + clazz);
								continue;
							}
							getLog().error("[UNEXPECTED] Failed to resolve enum constants for property = " + property + " and class = " + clazz, ex);
							throw ex;
						}
						for (Object enumConstant : enumConstants) {
							valueHints.add(new ValueHint(enumConstant, null));
						}

						if (!providers.containsKey(property.getType())) {
							providers.put(property.getType(), new ArrayList<ValueProvider>());
						}

						//Equals is not correct for ValueProvider

						boolean found = false;
						for (ValueProvider valueProvider : providers.get(property.getType())) {
							if (valueProvider.getName().equals(property.getType())) {
								found = true;
							}
						}

						if (!found) {
							providers.get(property.getType()).add(new ValueProvider(property.getType(), null));
						}

						itemHints.put(property.getType(), new ItemHint(property.getName(), valueHints,
								new ArrayList<>(providers.get(property.getType()))));

					}
				}
			}
		}
		if (!CollectionUtils.isEmpty(itemHints)) {
			for (ItemHint itemHint : itemHints.values()) {
				configurationMetadata.add(itemHint);
			}
		}
	}

	private ClassLoader getClassLoader(String jarPath) {
		ClassLoader classLoader = null;
		try {
			classLoader = new URLClassLoader(new URL[] { new URL("file://" + jarPath) },
					this.getClass().getClassLoader());
		}
		catch (MalformedURLException e) {
			// pass through
		}
		return classLoader;
	}

	public static class MetadataFilter {
		private List<String> names;

		private List<String> sourceTypes;

		public List<String> getNames() {
			return names;
		}

		public void setNames(List<String> names) {
			this.names = names;
		}

		public List<String> getSourceTypes() {
			return sourceTypes;
		}

		public void setSourceTypes(List<String> sourceTypes) {
			this.sourceTypes = sourceTypes;
		}

		@Override
		public String toString() {
			return "MetadataFilter{" +
					"name=" + names +
					", sourceType=" + sourceTypes +
					'}';
		}
	}

	/**
	 * A tuple holding both configuration metadata and the whitelist properties.
	 *
	 * @author Eric Bottard
	 */
	/*default*/ static final class Result {
		private final ConfigurationMetadata metadata;

		private final Properties visible;

		private Properties getPortMappingProperties() {
			Properties portMappingProperties = new Properties();
			visible.entrySet().stream()
					.filter(e -> e.getKey().equals(CONFIGURATION_PROPERTIES_OUTBOUND_PORTS) || e.getKey().equals(CONFIGURATION_PROPERTIES_INBOUND_PORTS))
					.forEach(e -> portMappingProperties.put(e.getKey(), e.getValue()));
			return portMappingProperties;
		}

		private Result(ConfigurationMetadata metadata, Properties visible) {
			this.metadata = metadata;
			this.visible = visible;
		}
	}
}
