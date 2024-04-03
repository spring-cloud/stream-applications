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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.MojoRule;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
public class MojoHarnessTest {

	@Rule
	public TemporaryFolder projectHome = new TemporaryFolder();

	@Rule
	public MojoRule mojoRule = new MojoRule();

	@Test
	public void testSomething() throws Exception {

		File pomRoot = new File("src/test/resources/unit/http-source-apps/");

		SpringCloudStreamAppGeneratorMojo myMojo = (SpringCloudStreamAppGeneratorMojo)
				mojoRule.lookupConfiguredMojo(pomRoot, "generate-app");

		assertThat(myMojo).isNotNull();

		myMojo.execute();

		assertThat(new File("./target/apps/http-source-kafka/src/main/resources/test.txt")).exists();

		assertThat(new File("./target/apps/http-source-kafka/src/main/resources/application.properties")).exists();

		Properties applicationProperties = new Properties();
		applicationProperties.load(new FileReader("./target/apps/http-source-kafka/src/main/resources/application.properties"));
		assertThat(applicationProperties.getProperty("spring.cloud.function.definition")).isEqualTo("httpSupplier");

		assertThat(applicationProperties.getProperty("static.property")).isEqualTo("bla");

		Model pomModel = getModel(new File("./target/apps"));

		List<Dependency> dependencies = pomModel.getDependencies();
		assertThat(dependencies.size()).isEqualTo(15);

		assertThat(dependencies.stream()
				.filter(d -> d.getArtifactId().equals("http-supplier")).count()).isEqualTo(1);

		assertThat(dependencies.stream()
				.filter(d -> d.getArtifactId().equals("stream-applications-postprocessor-common")).count()).isEqualTo(1);

		assertThat(dependencies.stream()
				.filter(d -> d.getArtifactId().equals("spring-cloud-stream-binder-kafka")).count()).isEqualTo(1);

		Parent parent = pomModel.getParent();
		assertThat(parent.getArtifactId()).isEqualTo("spring-boot-starter-parent");
		assertThat(parent.getVersion()).isEqualTo("3.3.0.M3");

		assertThat(pomModel.getArtifactId()).isEqualTo("http-source-kafka");
		assertThat(pomModel.getGroupId()).isEqualTo("org.springframework.cloud.stream.app.test");
		assertThat(pomModel.getName()).isEqualTo("http-source-kafka");
		assertThat(pomModel.getVersion()).isEqualTo("3.0.0.BUILD-SNAPSHOT");
		assertThat(pomModel.getDescription()).isEqualTo("Spring Cloud Stream Http Source Kafka Binder Application");

		List<Plugin> plugins = pomModel.getBuild().getPlugins();

		assertThat(plugins.stream().filter(p -> p.getArtifactId().equals("spring-boot-maven-plugin")).count()).isEqualTo(1);
		assertThat(plugins.stream().filter(p -> p.getArtifactId().equals("properties-maven-plugin")).count()).isEqualTo(1);
		assertThat(plugins.stream().filter(p -> p.getArtifactId().equals("jib-maven-plugin")).count()).isEqualTo(1);

		Plugin jibPlugin = plugins.stream().filter(p -> p.getArtifactId().equals("jib-maven-plugin")).findFirst().get();
		assertThat(jibPlugin.getConfiguration().toString())
				.contains("<org.springframework.cloud.dataflow.spring-configuration-metadata.json>" +
						"${org.springframework.cloud.dataflow.spring.configuration.metadata.json}" +
						"</org.springframework.cloud.dataflow.spring-configuration-metadata.json>");
		assertThat(jibPlugin.getConfiguration().toString()).contains("<image>testspringcloud/${project.artifactId}:3.0.0.BUILD-SNAPSHOT</image>");
		assertThat(jibPlugin.getConfiguration().toString()).contains("<image>globalBaseImage</image>");
		assertThat(pomModel.getRepositories().size()).isEqualTo(5);

		assertThat(pomModel.getRepositories().stream().map(r -> r.getId()).collect(Collectors.toList()))
				.contains("bintray-global", "bintray-application", "bintray-binder");
	}

	private Model getModel(File rootPath) {
		File pomXml = new File(new File(rootPath, "http-source-kafka"), "pom.xml");
		try (InputStream is = new FileInputStream(pomXml)) {
			return new MavenXpp3Reader().read(is);
		}
		catch (IOException | XmlPullParserException e) {
			throw new IllegalStateException(e);
		}
	}
}

