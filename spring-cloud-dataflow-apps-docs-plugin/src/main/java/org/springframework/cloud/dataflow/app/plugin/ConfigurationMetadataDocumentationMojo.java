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
import java.util.List;
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
 * everything in between with a listing of visible configuration properties for a
 * Spring Cloud Stream/Task app.
 *
 * @author Eric Bottard
 * @author David Turanski
 * @see <a href=
 * "https://docs.spring.io/spring-cloud-dataflow/docs/1.1.0.M2/reference/html/spring-cloud-dataflow-register-apps.html#spring-cloud-dataflow-stream-app-whitelisting">Whitelisting
 * Properties</a>
 */
@Mojo(name = "generate-documentation", requiresDependencyResolution = ResolutionScope.RUNTIME)
public class ConfigurationMetadataDocumentationMojo extends AbstractMojo {

	private BootApplicationConfigurationMetadataResolver metadataResolver =
			new BootApplicationConfigurationMetadataResolver(imageName -> null);

	@Parameter(defaultValue = "${project}")
	private MavenProject mavenProject;

	@Parameter(defaultValue = "false")
	private boolean failOnMissingDescription;

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

			String line = null;
			do {
				line = reader.readLine();
				out.println(line);
			}
			while (line != null && !line.startsWith("//tag::configuration-properties[]"));
			if (line == null) {
				getLog().info("No documentation section marker found");
				return;
			}

			ScatteredArchive archive = new ScatteredArchive(mavenProject);
			BootClassLoaderFactory bootClassLoaderFactory = new BootClassLoaderFactory(archive, null);
			try (URLClassLoader classLoader = bootClassLoaderFactory.createClassLoader()) {
				debug(classLoader);

				List<ConfigurationMetadataProperty> properties = metadataResolver.listProperties(archive, false);
				Collections.sort(properties, new Comparator<ConfigurationMetadataProperty>() {

					@Override
					public int compare(ConfigurationMetadataProperty p1, ConfigurationMetadataProperty p2) {
						return p1.getId().compareTo(p2.getId());
					}
				});

				for (ConfigurationMetadataProperty property : properties) {
					getLog().debug("Documenting " + property.getId());
					out.println(asciidocFor(property, classLoader));
				}

				do {
					line = reader.readLine();
					// drop lines
				}
				while (!line.startsWith("//end::configuration-properties[]"));

				// Copy remaining lines, including //end::configuration-properties[]
				while (line != null) {
					out.println(line);
					line = reader.readLine();
				}
				getLog().info(String.format("Documented %d configuration properties", properties.size()));
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

	private void debug(ClassLoader classLoader) {
		if (classLoader instanceof URLClassLoader) {
			List<URL> urls = Arrays.asList(((URLClassLoader) classLoader).getURLs());
			getLog().debug("Classloader has the following URLs:\n" + urls.toString().replace(',', '\n'));
		}
	}

	private String asciidocFor(ConfigurationMetadataProperty property, ClassLoader classLoader) {
		return String.format("$$%s$$:: $$%s$$ *($$%s$$, default: `$$%s$$`%s)*",
				property.getId(),
				niceDescription(property),
				niceType(property),
				niceDefault(property),
				maybeHints(property, classLoader));
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
		return type.substring(Math.max(lastDot, lastDollar) + 1, type.length());
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
		public Manifest getManifest() throws IOException {
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
