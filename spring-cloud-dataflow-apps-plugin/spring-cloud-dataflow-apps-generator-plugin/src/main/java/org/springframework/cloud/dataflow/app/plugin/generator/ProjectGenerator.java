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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import org.apache.commons.io.FileUtils;

import org.springframework.util.Assert;

/**
 * @author Christian Tzolov
 */
public final class ProjectGenerator {

	private ProjectGenerator() {
	}

	public static ProjectGenerator getInstance() {
		return new ProjectGenerator();
	}

	// Utils
	public static File mkdirs(File dir) {
		if (!dir.exists()) {
			dir.mkdirs();
		}
		if (!dir.isDirectory()) {
			throw new IllegalStateException("Not a directory: " + dir);
		}
		return dir;
	}

	public static void copy(String content, File file) throws IOException {
		Files.copy(new ByteArrayInputStream(content.getBytes()), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	public static String toPkg(String text) {
		return text.replace("-", ".");
	}

	public static String capitalize(String text) {
		return text.substring(0, 1).toUpperCase() + text.substring(1);
	}

	public static String camelCase(String text) {
		return Arrays.stream(text.split("-")).reduce("", (r, p) -> r + capitalize(p));
	}

	public static File pkgToDir(File parent, String packageName) {
		String[] names = packageName.split("\\.");
		File result = parent;
		for (String p : names) {
			result = file(result, p);
		}
		return result;
	}

	public static File file(File parent, String child) {
		return new File(parent, child);
	}

	public void generate(ProjectGeneratorProperties generatorProperties) throws IOException {

		Map<String, Object> modulesTemplateProperties = new HashMap<>();
		// register {{#capitalize}}...{{/capitalize}} function.
		modulesTemplateProperties.put("capitalize", (Mustache.Lambda) (frag, out) -> out.write(capitalize(frag.execute().trim())));
		// register {{#camelCase}}...{{/camelCase}} function.
		modulesTemplateProperties.put("camelCase", (Mustache.Lambda) (frag, out) -> out.write(camelCase(frag.execute().trim())));

		modulesTemplateProperties.put("app", generatorProperties.getAppDefinition());
		modulesTemplateProperties.put("binders", generatorProperties.getBinders());

		// ---------------------------------
		// Generate apps container POM
		// ---------------------------------
		File appParentDir = mkdirs(generatorProperties.getOutputFolder());
		copy(materialize("template/apps-modules-pom.xml", modulesTemplateProperties),
				file(appParentDir, "pom.xml"));
		// maven wrapper
		copyMavenWrapper(appParentDir);

		// ---------------------------------
		// Generate App projects
		// ---------------------------------
		Assert.notEmpty(generatorProperties.getBinders(), "At least one Binder must be provided");
		for (BinderDefinition binder : generatorProperties.getBinders()) {
			generateAppProject(appParentDir, modulesTemplateProperties, generatorProperties.getAppDefinition(),
					generatorProperties.getProjectResourcesDirectory(), binder);
		}
	}

	private void generateAppProject(File appRootDirectory, Map<String, Object> containerTemplateProperties,
			AppDefinition appDefinition, File projectResourcesDirectory, BinderDefinition binder) throws IOException {

		String appClassName = String.format("%s%s%sApplication",
				camelCase(appDefinition.getName()),
				capitalize(appDefinition.getType().name()),
				capitalize(binder.getName()));

		String appPackageName = String.format("%s.%s.%s.%s",
				appDefinition.getGroupId(),
				toPkg(appDefinition.getName()),
				appDefinition.getType(),
				binder.getName());

		Map<String, Object> appTemplateProperties = new HashMap<>(containerTemplateProperties);

		// Shortcut substitutions. Prevent complicated expressions in the templates.
		appTemplateProperties.put("app-class-name", appClassName);
		appTemplateProperties.put("app-package-name", appPackageName);
		appTemplateProperties.put("app-binder", binder);

		// app POM
		File appDir =
				mkdirs(file(appRootDirectory, appDefinition.getName() + "-" + appDefinition.getType() + "-" + binder.getName()));

		copy(materialize("template/app-pom.xml", appTemplateProperties), file(appDir, "pom.xml"));

		File appMainSrcDir = mkdirs(pkgToDir(appDir, "src.main.java." + appPackageName));

		File appMainResourceDir = mkdirs(pkgToDir(appDir, "src.main.resources"));

		// copy the entire project's src/main/resources directory
		if (projectResourcesDirectory != null && projectResourcesDirectory.exists()) {
			FileUtils.copyDirectory(projectResourcesDirectory, appMainResourceDir);
		}

		// application.properties
		String materializedAppProperties = materialize("template/app.properties", appTemplateProperties);
		// Note: the application properties file may already exist from the parent's project src/main/resources dir.
		File appPropertyFile = file(appMainResourceDir, "application.properties");
		//FileUtils.writeStringToFile(appPropertyFile, materializedAppProperties, "UTF8", true);
		Properties appProps = new Properties();
		if (appPropertyFile.exists()) {
			appProps.load(new FileInputStream(appPropertyFile));
		}
		Properties genAppProps = new Properties();
		genAppProps.load(new ByteArrayInputStream(materializedAppProperties.getBytes(StandardCharsets.UTF_8)));

		// Generated properties override the inherited!
		appProps.putAll(genAppProps);
		appProps.store(new FileWriter(appPropertyFile), "App generator properties");

		copy(materialize("template/App.java", appTemplateProperties),
				file(appMainSrcDir, appClassName + ".java"));

		// TESTS
		File appTestSrcDir = mkdirs(pkgToDir(appDir, "src.test.java." + appPackageName));

		copy(materialize("template/AppTests.java", appTemplateProperties),
				file(appTestSrcDir, appClassName + "Tests.java"));

		// README
		copy(materialize("template/README.adoc", appTemplateProperties),
				file(appDir, "README.adoc"));

		// maven wrapper
		copyMavenWrapper(appDir);
	}

	private void copyMavenWrapper(File appDir) throws IOException {

		// mvnw
		copyResource("template/mvnw", file(appDir, "mvnw"));
		file(appDir, "mvnw").setExecutable(true);

		copyResource("template/mvnw.cmd", file(appDir, "mvnw.cmd"));

		File dotMavenDir = mkdirs(new File(appDir, ".mvn"));

		// .mvn/jvm.config
		copyResource("template/.mvn/jvm.config", file(dotMavenDir, "jvm.config"));

		// .mvn/maven.config
		copyResource("template/.mvn/maven.config", file(dotMavenDir, "maven.config"));

		File dotMavenWrapper = mkdirs(file(appDir, ".mvn/wrapper"));

		// .mvn/wrapper/maven-wrapper.jar
		copyResource("template/.mvn/wrapper/maven-wrapper.jar",
				file(dotMavenWrapper, "maven-wrapper.jar"));

		// .mvn/wrapper/maven-wrapper.properties
		copyResource("template/.mvn/wrapper/maven-wrapper.properties",
				file(dotMavenWrapper, "maven-wrapper.properties"));

		// .mvn/wrapper/MavenWrapperDownloader.java
		copyResource("template/.mvn/wrapper/MavenWrapperDownloader.java",
				file(dotMavenWrapper, "MavenWrapperDownloader.java"));
	}

	private String materialize(String templatePath, Map<String, Object> templateProperties) throws IOException {
		try (InputStreamReader resourcesTemplateReader = new InputStreamReader(
				Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(templatePath)))) {
			Template resourceTemplate = Mustache.compiler().escapeHTML(false).compile(resourcesTemplateReader);
			return resourceTemplate.execute(templateProperties);
		}
	}

	private void copyResource(String resourcePath, File toFile) throws IOException {
		try (InputStream resourcesStream = Objects.requireNonNull(
				this.getClass().getClassLoader().getResourceAsStream(resourcePath))) {
			Files.copy(resourcesStream, toFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
