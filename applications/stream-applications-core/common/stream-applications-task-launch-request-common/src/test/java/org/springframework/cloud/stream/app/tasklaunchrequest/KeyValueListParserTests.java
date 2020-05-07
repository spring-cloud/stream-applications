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

package org.springframework.cloud.stream.app.tasklaunchrequest;

import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Chris Schaefer
 * @author David Turanski
 */
public class KeyValueListParserTests {

	@Test
	public void testParseSimpleDeploymentProperty() {
		Map<String, String> deploymentProperties = KeyValueListParser.parseCommaDelimitedKeyValuePairs(
				"app.sftp.param=value");

		assertThat(deploymentProperties.size()).isEqualTo(1);
		assertThat(deploymentProperties.containsKey("app.sftp.param")).isTrue();
		assertThat(deploymentProperties.get("app.sftp.param")).isEqualTo("value");
	}

	@Test
	public void testParseSimpleDeploymentPropertyMultipleValues() {
		Map<String, String> deploymentProperties = KeyValueListParser.parseCommaDelimitedKeyValuePairs(
				"app.sftp.param=value1,value2,value3");

		assertThat(deploymentProperties.size()).isEqualTo(1);
		assertThat(deploymentProperties.containsKey("app.sftp.param")).isTrue();
		assertThat(deploymentProperties.get("app.sftp.param")).isEqualTo("value1,value2,value3");
	}

	@Test
	public void testParseSpelExpressionMultipleValues() {
		Map<String, String> argExpressions = KeyValueListParser.parseCommaDelimitedKeyValuePairs(
				"arg1=payload.substr(0,2),arg2=headers['foo'],arg3=headers['bar']==false");


		assertThat(argExpressions.size()).isEqualTo(3);
		assertThat(argExpressions.containsKey("arg1")).isTrue();
		assertThat(argExpressions.get("arg1")).isEqualTo("payload.substr(0,2)");
		assertThat(argExpressions.containsKey("arg2")).isTrue();
		assertThat(argExpressions.get("arg2")).isEqualTo("headers['foo']");
	}

	@Test
	public void testParseMultipleDeploymentPropertiesSingleValue() {
		Map<String, String> deploymentProperties = KeyValueListParser.parseCommaDelimitedKeyValuePairs(
				"app.sftp.param=value1,app.sftp.other.param=value2");

		assertThat(deploymentProperties.size()).isEqualTo(2);
		assertThat(deploymentProperties.containsKey("app.sftp.param")).isTrue();
		assertThat(deploymentProperties.get("app.sftp.param")).isEqualTo("value1");
		assertThat(deploymentProperties.containsKey("app.sftp.other.param")).isTrue();
		assertThat(deploymentProperties.get("app.sftp.other.param")).isEqualTo("value2");
	}

	@Test
	public void testParseMultipleDeploymentPropertiesMultipleValues() {
		DataflowTaskLaunchRequestProperties taskLaunchRequestProperties = new DataflowTaskLaunchRequestProperties();

		Map<String, String> deploymentProperties = KeyValueListParser.parseCommaDelimitedKeyValuePairs(
				"app.sftp.param=value1,value2,app.sftp.other.param=other1,other2");

		assertThat(deploymentProperties.size()).isEqualTo(2);
		assertThat(deploymentProperties.containsKey("app.sftp.param")).isTrue();
		assertThat(deploymentProperties.get("app.sftp.param")).isEqualTo("value1,value2");
		assertThat(deploymentProperties.get("app.sftp.other.param")).isEqualTo("other1,other2");
	}
}
