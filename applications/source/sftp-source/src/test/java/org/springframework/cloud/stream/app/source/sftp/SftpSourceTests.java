/*
 * Copyright 2020-2021 the original author or authors.
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

package org.springframework.cloud.stream.app.source.sftp;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.fn.supplier.sftp.SftpSupplierConfiguration;
import org.springframework.cloud.fn.supplier.sftp.SftpSupplierProperties;
import org.springframework.cloud.fn.test.support.sftp.SftpTestSupport;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

public class SftpSourceTests extends SftpTestSupport {
	@Test
	void simple() {
		TestChannelBinderConfiguration.applicationContextRunner(TestApp.class)
				.withPropertyValues("sftp.supplier.factory.username = foo",
						"sftp.supplier.factory.password = foo",
						"file.consumer.mode = ref",
						"sftp.supplier.factory.cacheSessions = true",
						"sftp.supplier.factory.port =${sftp.factory.port}",
						"sftp.supplier.factory.allowUnknownKeys=true",
						"sftp.supplier.remoteDir=sftpSource",
						"sftp.supplier.delete-remote-files=true",
						"spring.cloud.function.definition=sftpSupplier")
				.run(context -> {
					OutputDestination output = context.getBean(OutputDestination.class);
					SftpSupplierProperties config = context.getBean(SftpSupplierProperties.class);
					Message<byte[]> message = output.receive(10000, "sftpSupplier-out-0");
					assertThat(new File(new String(message.getPayload()).replaceAll("\"", ""))).isEqualTo(
							new File(config.getLocalDir(), "sftpSource1.txt"));
					message = output.receive(10000);
					assertThat(new File(new String(message.getPayload()).replaceAll("\"", ""))).isEqualTo(
							new File(config.getLocalDir(), "sftpSource2.txt"));
				});
	}

	@Test
	void taskLaunchRequest() {
		TestChannelBinderConfiguration.applicationContextRunner(TestApp.class)
				.withPropertyValues("sftp.supplier.factory.username = foo",
						"sftp.supplier.factory.password = foo",
						"file.consumer.mode = ref",
						"sftp.supplier.factory.cacheSessions = true",
						"sftp.supplier.factory.port =${sftp.factory.port}",
						"sftp.supplier.factory.allowUnknownKeys=true",
						"sftp.supplier.remoteDir=sftpSource",
						"sftp.supplier.localDir=" + this.targetLocalDirectory.toString(),
						"task.launch.request.arg-expressions=fileName=payload",
						"task.launch.request.task-name=myTask",
						"spring.cloud.function.definition=sftpSupplier|taskLaunchRequestFunction")
				.run(context -> {
					OutputDestination output = context.getBean(OutputDestination.class);
					SftpSupplierProperties config = context.getBean(SftpSupplierProperties.class);
					ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
					Message<byte[]> message = output.receive(10000, "sftpSuppliertaskLaunchRequestFunction-out-0");
					Map<String, Object> taskLaunchRequest = objectMapper.readValue(message.getPayload(), HashMap.class);
					assertThat(taskLaunchRequest.get("name")).isEqualTo("myTask");
					assertThat((List) taskLaunchRequest.get("args"))
							.contains("fileName=" + Paths
									.get(config.getLocalDir().toString(), "sftpSource1.txt")
									.toString());
					message = output.receive(10000);
					taskLaunchRequest = objectMapper.readValue(message.getPayload(), HashMap.class);
					assertThat(taskLaunchRequest.get("name")).isEqualTo("myTask");
					assertThat((List) taskLaunchRequest.get("args"))
							.containsExactly("fileName=" + Paths
									.get(config.getLocalDir().toString(), "sftpSource2.txt")
									.toString());
				});
	}

	@SpringBootApplication
	@Import(SftpSupplierConfiguration.class)
	public static class TestApp {

	}
}
