/*
 * Copyright 2018-2025 the original author or authors.
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
import java.io.InputStream;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.dataflow.app.plugin.MetadataAggregationMojo.SPRING_CLOUD_DATAFLOW_OPTION_GROUPS_PROPERTIES;

/**
 * @author Max Brauer
 */
class OptionGroupsTest {

	@TempDir
	File tempDir;

	@Test
	void shouldWriteOptionGroupsToMetadataJar() throws Exception {
		MetadataAggregationMojo mojo = new MetadataAggregationMojo();
		MavenProject project = createMockMavenProject();
		setField(mojo, "mavenProject", project);
		setField(mojo, "projectHelper", mock(MavenProjectHelper.class));
		setField(mojo, "classifier", "metadata");

		ConfigurationMetadata metadata = new ConfigurationMetadata();

		Properties ports = new Properties(); // no ports

		Properties optionGroups = new Properties();
		optionGroups.setProperty("com.vmware.tanzu.dataflow.configuration-properties.option-groups.common", "prop1,prop2,prop3");
		optionGroups.setProperty("com.vmware.tanzu.dataflow.configuration-properties.option-groups.readers.filereader", "reader.prop1,reader.prop2");
		optionGroups.setProperty("com.vmware.tanzu.dataflow.configuration-properties.option-groups.writers.filewriter", "writer.prop1,writer.prop2,writer.prop3");

		MetadataAggregationMojo.Result result = new MetadataAggregationMojo.Result(metadata, ports, optionGroups);

		mojo.produceArtifact(result);

		File output = new File(tempDir, "target/test-artifact-1.0.0-SNAPSHOT-metadata.jar");
		assertThat(output).exists();

		try (ZipFile zipFile = new ZipFile(output)) {
			ZipEntry optionGroupsEntry = zipFile.getEntry("META-INF/" + SPRING_CLOUD_DATAFLOW_OPTION_GROUPS_PROPERTIES);
			assertThat(optionGroupsEntry).isNotNull();

			Properties optionGroupsProps = new Properties();
			try (InputStream is = zipFile.getInputStream(optionGroupsEntry)) {
				optionGroupsProps.load(is);
			}
			assertThat(optionGroupsProps)
					.as("Option groups file should contain option groups properties")
					.containsKey("com.vmware.tanzu.dataflow.configuration-properties.option-groups.common")
					.containsKey("com.vmware.tanzu.dataflow.configuration-properties.option-groups.readers.filereader")
					.containsKey("com.vmware.tanzu.dataflow.configuration-properties.option-groups.writers.filewriter");
			assertThat(optionGroupsProps.getProperty("com.vmware.tanzu.dataflow.configuration-properties.option-groups.common"))
					.isEqualTo("prop1,prop2,prop3");
		}
	}

	@Test
	void shouldWriteEmptyOptionGroupsToMetadataJar() throws Exception {
		MetadataAggregationMojo mojo = new MetadataAggregationMojo();
		MavenProject project = createMockMavenProject();
		setField(mojo, "mavenProject", project);
		setField(mojo, "projectHelper", mock(MavenProjectHelper.class));
		setField(mojo, "classifier", "metadata");

		ConfigurationMetadata metadata = new ConfigurationMetadata();

		Properties ports = new Properties(); // no ports
		Properties optionGroups = new Properties(); // no option groups

		MetadataAggregationMojo.Result result = new MetadataAggregationMojo.Result(metadata, ports, optionGroups);

		mojo.produceArtifact(result);

		File output = new File(tempDir, "target/test-artifact-1.0.0-SNAPSHOT-metadata.jar");
		assertThat(output).exists();

		try (ZipFile zipFile = new ZipFile(output)) {
			ZipEntry optionGroupsEntry = zipFile.getEntry("META-INF/" + SPRING_CLOUD_DATAFLOW_OPTION_GROUPS_PROPERTIES);
			assertThat(optionGroupsEntry)
					.as("Option groups file should exist even when empty")
					.isNotNull();

			Properties optionGroupsProps = new Properties();
			try (InputStream is = zipFile.getInputStream(optionGroupsEntry)) {
				optionGroupsProps.load(is);
			}
			assertThat(optionGroupsProps)
					.as("Option groups should be empty when no groups provided")
					.isEmpty();
		}
	}

	private MavenProject createMockMavenProject() {
		MavenProject project = mock(MavenProject.class);
		File targetDir = new File(tempDir, "target");
		targetDir.mkdirs();
		when(project.getBasedir()).thenReturn(tempDir);
		when(project.getArtifactId()).thenReturn("test-artifact");
		when(project.getVersion()).thenReturn("1.0.0-SNAPSHOT");
		return project;
	}

	private void setField(Object target, String fieldName, Object value) throws Exception {
		java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}
