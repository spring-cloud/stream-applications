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
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.cloud.dataflow.app.plugin.generator.AppDefinition;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Christian Tzolov
 * @author Soby Chacko
 */
public class SpringCloudStreamAppGeneratorMojoTest {

	@Rule
	public TemporaryFolder projectHome = new TemporaryFolder();

	private SpringCloudStreamAppGeneratorMojo springCloudStreamAppMojo = new SpringCloudStreamAppGeneratorMojo();

	private Class<? extends SpringCloudStreamAppGeneratorMojo> mojoClazz = springCloudStreamAppMojo.getClass();

	private SpringCloudStreamAppGeneratorMojo.Application application;

	@Before
	public void before() throws NoSuchFieldException {

		application = new SpringCloudStreamAppGeneratorMojo.Application();
		application.setName("log");
		application.setType(AppDefinition.AppType.sink);
		application.setVersion("3.0.0.BUILD-SNAPSHOT");
		application.setConfigClass("io.pivotal.java.function.log.consumer.LogConsumerConfiguration.class");

		application.getContainerImage().setFormat(AppDefinition.ContainerImageFormat.Docker);
		application.getContainerImage().setBaseImage("base/image");

		application.getMetadata().getSourceTypeFilters().add("io.pivotal.java.function.log.consumer.LogConsumerProperties");
		application.getMetadata().getNameFilters().add("server.port");

		application.getProperties().put("spring.cloud.streamapp.security.enabled", "false");
		application.getProperties().put("spring.cloud.streamapp.security.csrf-enabled", "false");

		Dependency dep = new Dependency();
		dep.setGroupId("io.pivotal.java.function");
		dep.setArtifactId("log-consumer");
		dep.setVersion("1.0.0.BUILD-SNAPSHOT");

		application.getMaven().getDependencies().add(dep);

		// BOM
		application.setBootVersion("3.3.0.M3");
		application.getMetadata().setMavenPluginVersion("1.0.2.BUILD-SNAPSHOT");

		setMojoProperty("application", application);

		//Binders
		SpringCloudStreamAppGeneratorMojo.Binder kafkaBinder = new SpringCloudStreamAppGeneratorMojo.Binder();
		Dependency kafkaDep = new Dependency();
		kafkaDep.setGroupId("org.springframework.cloud");
		kafkaDep.setArtifactId("spring-cloud-stream-binder-kafka");
		kafkaBinder.getMaven().getDependencies().add(kafkaDep);

		SpringCloudStreamAppGeneratorMojo.Binder rabbitBinder = new SpringCloudStreamAppGeneratorMojo.Binder();
		Dependency rabbitDep = new Dependency();
		rabbitDep.setGroupId("org.springframework.cloud");
		rabbitDep.setArtifactId("spring-cloud-stream-binder-rabbit");
		rabbitBinder.getMaven().getDependencies().add(rabbitDep);

		Map<String, SpringCloudStreamAppGeneratorMojo.Binder> binders = new HashMap<>();
		binders.put("kafka", kafkaBinder);
		binders.put("rabbit", rabbitBinder);

		setMojoProperty("binders", binders);

		//setMojoProperty("generatedProjectHome", "./target/apps");
		setMojoProperty("generatedProjectHome", projectHome.getRoot().getAbsolutePath());
	}

	@Test
	public void testWithDisabledContainerMetadata() throws Exception {

		// disable metadata in container image (Default)
		application.getContainerImage().setEnableMetadata(false);

		springCloudStreamAppMojo.execute();

		//Model pomModel = getModel(new File("./target/apps"));
		Model pomModel = getModel(new File(projectHome.getRoot().getAbsolutePath()));
		List<Plugin> plugins = pomModel.getBuild().getPlugins();

		// The properties-maven-plugin should not be defined if the container metadata is not enabled.
		assertThat(plugins.stream().filter(p -> p.getArtifactId().equals("properties-maven-plugin")).count()).isEqualTo(0);

		assertThat(plugins.stream().filter(p -> p.getArtifactId().equals("jib-maven-plugin")).count()).isEqualTo(1);

		Plugin jibPlugin = plugins.stream().filter(p -> p.getArtifactId().equals("jib-maven-plugin")).findFirst().get();
		assertThat(jibPlugin.getConfiguration().toString())
				.doesNotContain("<org.springframework.cloud.dataflow.spring-configuration-metadata.json>" +
						"${org.springframework.cloud.dataflow.spring.configuration.metadata.json}" +
						"</org.springframework.cloud.dataflow.spring-configuration-metadata.json>");
	}

	@Test
	public void testDefaultProjectCreationByPlugin() throws Exception {

		// Enable Metadata in Container Image!
		application.getContainerImage().setEnableMetadata(true);

		springCloudStreamAppMojo.execute();

		//assertGeneratedPomXml(new File("./target/apps"));
		assertGeneratedPomXml(new File(projectHome.getRoot().getAbsolutePath()));
	}

	@Test
	public void testCustomBootMavenPluginConfiguration() throws Exception {
		application.setBootPluginConfiguration("<![CDATA[\n" +
				"                            <requiresUnpack>\n" +
				"                                <dependency>\n" +
				"                                    <groupId>org.python</groupId>\n" +
				"                                    <artifactId>jython-standalone</artifactId>\n" +
				"                                </dependency>\n" +
				"                            </requiresUnpack>\n" +
				"                            ]]>");

		springCloudStreamAppMojo.execute();

		Model pomModel = getModel(new File(projectHome.getRoot().getAbsolutePath()));
		List<Plugin> plugins = pomModel.getBuild().getPlugins();
		final Optional<Plugin> bootPlugin = plugins.stream().filter(p -> p.getArtifactId().equals("spring-boot-maven-plugin")).findFirst();
		assertThat(bootPlugin.isPresent()).isTrue();
		final Plugin plugin = bootPlugin.get();
		final Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
		assertThat(configuration.getValue().contains("<requiresUnpack>")).isTrue();
		assertThat(configuration.getValue().contains("jython-standalone")).isTrue();
		assertThat(configuration.getValue().contains("</requiresUnpack>")).isTrue();
	}

	private void assertGeneratedPomXml(File rootPath) {

		Model pomModel = getModel(rootPath);

		List<Dependency> dependencies = pomModel.getDependencies();
		assertThat(dependencies.size()).isEqualTo(3);

		assertThat(dependencies.stream()
				.filter(d -> d.getArtifactId().equals("log-consumer")).count()).isEqualTo(1);

		assertThat(dependencies.stream()
				.filter(d -> d.getArtifactId().equals("spring-cloud-stream-binder-kafka")).count()).isEqualTo(1);

		Parent parent = pomModel.getParent();
		assertThat(parent.getArtifactId()).isEqualTo("spring-boot-starter-parent");
		assertThat(parent.getVersion()).isEqualTo("3.3.0.M3");

		assertThat(pomModel.getArtifactId()).isEqualTo("log-sink-kafka");
		assertThat(pomModel.getGroupId()).isEqualTo("org.springframework.cloud.stream.app");
		assertThat(pomModel.getName()).isEqualTo("log-sink-kafka");
		assertThat(pomModel.getVersion()).isEqualTo("3.0.0.BUILD-SNAPSHOT");
		assertThat(pomModel.getDescription()).isEqualTo("Spring Cloud Stream Log Sink Kafka Binder Application");

		List<Plugin> plugins = pomModel.getBuild().getPlugins();
		assertThat(plugins.stream().filter(p -> p.getArtifactId().equals("spring-boot-maven-plugin")).count()).isEqualTo(1);
		assertThat(plugins.stream().filter(p -> p.getArtifactId().equals("properties-maven-plugin")).count()).isEqualTo(1);
		assertThat(plugins.stream().filter(p -> p.getArtifactId().equals("jib-maven-plugin")).count()).isEqualTo(1);

		Plugin jibPlugin = plugins.stream().filter(p -> p.getArtifactId().equals("jib-maven-plugin")).findFirst().get();
		assertThat(jibPlugin.getConfiguration().toString())
				.contains("<org.springframework.cloud.dataflow.spring-configuration-metadata.json>" +
						"${org.springframework.cloud.dataflow.spring.configuration.metadata.json}" +
						"</org.springframework.cloud.dataflow.spring-configuration-metadata.json>");
		assertThat(jibPlugin.getConfiguration().toString()).contains("<image>base/image</image>");

		assertThat(pomModel.getRepositories().size()).isEqualTo(2);
	}

	private Model getModel(File rootPath) {
		File pomXml = new File(new File(rootPath, "log-sink-kafka"), "pom.xml");
		try (InputStream is = new FileInputStream(pomXml)) {
			return new MavenXpp3Reader().read(is);
		}
		catch (IOException | XmlPullParserException e) {
			throw new IllegalStateException(e);
		}
	}

	private void setMojoProperty(String propertyName, Object value) throws NoSuchFieldException {
		Field mojoProperty = mojoClazz.getDeclaredField(propertyName);
		mojoProperty.setAccessible(true);
		ReflectionUtils.setField(mojoProperty, springCloudStreamAppMojo, value);
	}
}
