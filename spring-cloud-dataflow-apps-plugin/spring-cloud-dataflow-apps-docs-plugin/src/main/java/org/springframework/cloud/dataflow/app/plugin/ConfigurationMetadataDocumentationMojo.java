/*
 * Copyright 2016-2020 the original author or authors.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.cloud.dataflow.configuration.metadata.BootApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.configuration.metadata.BootClassLoaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * A maven plugin that will scan an asciidoc file for special comment markers and replace
 * everything in between with a listing of visible configuration properties for a Spring
 * Cloud Stream/Task app. Applications with multiple properties prefixes are grouped by default.
 * This can be disabled by using {@code //tag::configuration-properties[group=false]}.
 *
 * @author Eric Bottard
 * @author David Turanski
 * @see <a href=
 * "https://dataflow.spring.io/docs/feature-guides/general/application-metadata/#whitelisting-properties">Whitelisting
 * Properties</a>
 */
@Mojo(name = "generate-documentation", requiresDependencyResolution = ResolutionScope.RUNTIME)
public class ConfigurationMetadataDocumentationMojo extends AbstractMojo {

	static final String CONFIGURATION_PROPERTIES_START_TAG = "//tag::configuration-properties[";

	static final String CONFIGURATION_PROPERTIES_END_TAG = "//end::configuration-properties[]";

	private static final Map<String, String> APPTYPE_TO_FUNCTIONTYPE = Map.of(
			"source", "supplier",
			"processor", "function",
			"sink", "consumer");

	private BootApplicationConfigurationMetadataResolver metadataResolver = new BootApplicationConfigurationMetadataResolver(
			imageName -> null);

	@Parameter(defaultValue = "${project}")
	private MavenProject mavenProject;

	@Parameter(defaultValue = "false")
	private boolean failOnMissingDescription;

	private boolean grouped = true;

	public void execute() throws MojoExecutionException {

		File readme = new File(mavenProject.getBasedir(), "README.adoc");
		if (!readme.exists()) {
			getLog().info(String.format("No README.adoc file found in %s, skipping", mavenProject.getBasedir()));
			return;
		}

		Artifact artifact = mavenProject.getArtifact();
		if (artifact.getFile() == null) {
			getLog().info(String.format("Project in %s does not produce a build artifact, skipping",
					mavenProject.getBasedir()));
			return;
		}

		File tmp = new File(readme.getPath() + ".tmp");
		try (PrintWriter out = new PrintWriter(tmp);
			BufferedReader reader = new BufferedReader(new FileReader(readme))) {

			String line;
			do {
				line = reader.readLine();
				out.println(line);
			}
			while (line != null && !line.startsWith(CONFIGURATION_PROPERTIES_START_TAG));
			if (line == null) {
				getLog().info("No documentation section marker found");
				return;
			}

			Map<String, String> startTagAttributes = startTagAttributes(line);

			if ("false".equals(startTagAttributes.get("group"))) {
				grouped = false;
			}

			boolean linkToFunctionCatalog = "true".equals(startTagAttributes.get("link-to-catalog"));
			if (linkToFunctionCatalog) {
				handleExternalLinkToFunctionsCatalog(artifact, out);
			}
			else {
				handleInlineConfigProperties(out);
			}

			// Drop all lines between start/end tag
			do {
				line = reader.readLine();
			}
			while (!line.startsWith(CONFIGURATION_PROPERTIES_END_TAG));

			// Copy remaining lines, including //end::configuration-properties[]
			while (line != null) {
				out.println(line);
				line = reader.readLine();
			}
		}
		catch (Exception e) {
			tmp.delete();
			throw new MojoExecutionException("Error generating documentation", e);
		}

		try {
			Files.move(tmp.toPath(), readme.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException e) {
			throw new MojoExecutionException("Error moving tmp file to README.adoc", e);
		}
	}

	private void handleExternalLinkToFunctionsCatalog(Artifact artifact, PrintWriter out) {
		String artifactId = artifact.getArtifactId();
		String appName = artifactId.substring(0, artifactId.lastIndexOf('-'));
		String appType = artifactId.substring(artifactId.lastIndexOf('-') + 1);
		String functionType = APPTYPE_TO_FUNCTIONTYPE.get(appType);
		String functionName = "spring-%s-%s".formatted(appName, functionType);
		String url = "https://github.com/spring-cloud/spring-functions-catalog/tree/main/%s/%s#configuration-options[See Spring Functions Catalog for configuration options].".formatted(functionType, functionName);
		out.println(url);
	}

	private void handleInlineConfigProperties(PrintWriter out) throws IOException {
		ScatteredArchive archive = new ScatteredArchive(mavenProject);
		BootClassLoaderFactory bootClassLoaderFactory = new BootClassLoaderFactory(archive, null);
		try (URLClassLoader classLoader = bootClassLoaderFactory.createClassLoader()) {
			debug(classLoader);
			List<ConfigurationMetadataProperty> properties = metadataResolver.listProperties(archive, false);
			Collections.sort(properties, Comparator.comparing(ConfigurationMetadataProperty::getId));
			Map<String, List<ConfigurationMetadataProperty>> groupedProperties = groupProperties(properties);
			grouped = grouped && groupedProperties.size() > 1;
			if (grouped) {
				out.println("Properties grouped by prefix:\n");
				groupedProperties.forEach((group, props) -> {
					getLog().debug(" Documenting group " + group);
					out.println(asciidocForGroup(group));
					listProperties(props, out, classLoader, prop -> prop.getName());
				});
			}
			else {
				listProperties(properties, out, classLoader, prop -> prop.getId());
			}
			getLog().info(String.format("Documented %d configuration properties", properties.size()));
		}
	}

	private void listProperties(List<ConfigurationMetadataProperty> properties, PrintWriter out,
			ClassLoader classLoader,
			Function<ConfigurationMetadataProperty, String> propertyValue) {
		for (ConfigurationMetadataProperty property : properties) {
			getLog().debug("Documenting " + property.getId());
			out.println(asciidocFor(property, classLoader, propertyValue));
		}
	}

	private Map<String, List<ConfigurationMetadataProperty>> groupProperties(
			List<ConfigurationMetadataProperty> properties) {
		Map<String, List<ConfigurationMetadataProperty>> groupedProperties = new LinkedHashMap<>();
		properties.forEach(property -> {
			String group = group(property.getId());
			if (!groupedProperties.containsKey(group)) {
				groupedProperties.put(group, new LinkedList<>());
			}
			groupedProperties.get(group).add(property);
		});
		return groupedProperties;
	}

	private String group(String id) {
		return id.lastIndexOf('.') > 0 ? id.substring(0, id.lastIndexOf('.')) : "";
	}

	private void debug(ClassLoader classLoader) {
		if (classLoader instanceof URLClassLoader) {
			List<URL> urls = Arrays.asList(((URLClassLoader) classLoader).getURLs());
			getLog().debug("Classloader has the following URLs:\n" + urls.toString().replace(',', '\n'));
		}
	}

	private String asciidocFor(ConfigurationMetadataProperty property, ClassLoader classLoader,
			Function<ConfigurationMetadataProperty, String> propertyValue) {
		return String.format("$$%s$$:: $$%s$$ *($$%s$$, default: `$$%s$$`%s)*",
				propertyValue.apply(property),
				niceDescription(property),
				niceType(property),
				niceDefault(property),
				maybeHints(property, classLoader));
	}

	private String asciidocForGroup(String group) {
		return "\n=== " + group + "\n";
	}

	private Map<String, String> startTagAttributes(String startTag) {
		String attrs = startTag.substring(startTag.indexOf('[') + 1, startTag.indexOf(']'));
		Set<String> set = StringUtils.commaDelimitedListToSet(attrs);
		Map<String, String> attributes = new LinkedHashMap<>();
		set.forEach(attr -> {
			String[] keyValue = attr.split("=");
			attributes.put(keyValue[0].trim(), keyValue.length == 2 ? keyValue[1].trim() : "");
		});
		return attributes;
	}

	private String niceDescription(ConfigurationMetadataProperty property) {
		if (property.getDescription() == null) {
			if (failOnMissingDescription) {
				throw new RuntimeException("Missing description for property " + property.getId());
			}
			else {
				return "<documentation missing>";
			}
		}
		return property.getDescription();
	}

	private CharSequence maybeHints(ConfigurationMetadataProperty property, ClassLoader classLoader) {
		String type = property.getType();
		if (type == null) {
			return "";
		}

		type = type.replace('$', '.');
		if (ClassUtils.isPresent(type, classLoader)) {
			Class<?> clazz = ClassUtils.resolveClassName(type, classLoader);
			if (clazz.isEnum()) {
				return ", possible values: `" + StringUtils.arrayToDelimitedString(clazz.getEnumConstants(), "`,`")
						+ "`";
			}
		}
		return "";
	}

	private String niceDefault(ConfigurationMetadataProperty property) {
		if (property.getDefaultValue() == null) {
			return "<none>";
		}
		else if ("".equals(property.getDefaultValue())) {
			return "<empty string>";
		}
		else {
			return stringify(property.getDefaultValue());
		}
	}

	private String stringify(Object element) {
		Class<?> clazz = element.getClass();
		if (clazz == byte[].class) {
			return Arrays.toString((byte[]) element);
		}
		else if (clazz == short[].class) {
			return Arrays.toString((short[]) element);
		}
		else if (clazz == int[].class) {
			return Arrays.toString((int[]) element);
		}
		else if (clazz == long[].class) {
			return Arrays.toString((long[]) element);
		}
		else if (clazz == char[].class) {
			return Arrays.toString((char[]) element);
		}
		else if (clazz == float[].class) {
			return Arrays.toString((float[]) element);
		}
		else if (clazz == double[].class) {
			return Arrays.toString((double[]) element);
		}
		else if (clazz == boolean[].class) {
			return Arrays.toString((boolean[]) element);
		}
		else if (element instanceof Object[]) {
			return Arrays.deepToString((Object[]) element);
		}
		else {
			return element.toString();
		}
	}

	private String niceType(ConfigurationMetadataProperty property) {
		String type = property.getType();
		if (type == null) {
			return "<unknown>";
		}
		return niceType(type);
	}

	String niceType(String type) {
		List<String> parts = new ArrayList<>();
		int openBrackets = 0;
		int lastGenericPart = 0;
		for (int i = 0; i < type.length(); i++) {
			switch (type.charAt(i)) {
			case '<':
				if (openBrackets++ == 0) {
					parts.add(type.substring(0, i));
					lastGenericPart = i + 1;
				}
				break;
			case '>':
				if (--openBrackets == 0) {
					parts.add(type.substring(lastGenericPart, i));
				}
				break;
			case ',':
				if (openBrackets == 1) {
					parts.add(type.substring(lastGenericPart, i));
					lastGenericPart = i + 1;
				}
				break;
			case ' ':
				if (openBrackets == 1) {
					lastGenericPart++;
				}
				break;
			}
		}
		if (parts.isEmpty()) {
			return unqualify(type); // simple type
		}
		else { // type with generics
			StringBuilder sb = new StringBuilder(unqualify(parts.get(0)));
			for (int i = 1; i < parts.size(); i++) {
				if (i == 1) {
					sb.append('<');
				}
				sb.append(unqualify(niceType(parts.get(i))));
				if (i == parts.size() - 1) {
					sb.append('>');
				}
				else {
					sb.append(", ");
				}
			}
			return sb.toString();
		}
	}

	private String unqualify(String type) {
		int lastDot = type.lastIndexOf('.');
		int lastDollar = type.lastIndexOf('$');
		return type.substring(Math.max(lastDot, lastDollar) + 1);
	}

	/**
	 * An adapter to boot {@link Archive} that satisfies just enough of the API to craft a
	 * ClassLoader that "sees" all the properties that this Mojo tries to document.
	 * @author Eric Bottard
	 */
	private static final class ScatteredArchive implements Archive {

		private final MavenProject mavenProject;

		private ScatteredArchive(MavenProject mavenProject) {

			this.mavenProject = mavenProject;
		}

		@Override
		public URL getUrl() throws MalformedURLException {
			return mavenProject.getArtifact().getFile().toURI().toURL();
		}

		@Override
		public Manifest getManifest() {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Archive> getNestedArchives(EntryFilter ignored) throws IOException {
			try {
				List<Archive> archives = new ArrayList<>(mavenProject.getRuntimeClasspathElements().size());
				for (String dep : mavenProject.getRuntimeClasspathElements()) {
					File file = new File(dep);
					archives.add(file.isDirectory() ? new ExplodedArchive(file) : new JarFileArchive(file));
				}
				return archives;
			}
			catch (DependencyResolutionRequiredException e) {
				throw new IOException("Could not create boot archive", e);
			}

		}

		@Override
		public Iterator<Entry> iterator() {
			// BootClassLoaderFactory.createClassLoader (which uses this iterator call) is not
			// actually
			// used here. Returning the simples thing that works.
			return Collections.emptyIterator();
		}
	}
}
