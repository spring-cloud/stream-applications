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

package org.springframework.cloud.stream.app.integration.test.source.s3;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.github.dockerjava.api.command.CreateContainerCmd;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.app.test.integration.OutputMatcher;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.app.test.integration.junit.jupiter.BaseContainerExtension;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.integration.test.common.Configuration.DEFAULT_DURATION;
import static org.springframework.cloud.stream.app.test.integration.AppLog.appLog;
import static org.springframework.cloud.stream.app.test.integration.FluentMap.fluentMap;
import static org.springframework.cloud.stream.app.test.integration.StreamAppContainerTestUtils.localHostAddress;
import static org.springframework.cloud.stream.app.test.integration.StreamAppContainerTestUtils.resourceAsFile;
@Tag("integration")
@ExtendWith(BaseContainerExtension.class)
abstract class S3SourceTests {
	private static final Logger logger = LoggerFactory.getLogger(S3SourceTests.class);
	private static AmazonS3 s3Client;

	private StreamAppContainer source;

	@Autowired
	private OutputMatcher outputMatcher;

	@Container
	private static final GenericContainer minio = new GenericContainer(
			DockerImageName.parse("minio/minio:latest"))
				.withExposedPorts(9000)
				.withNetworkAliases("minio-host")
				.withEnv("MINIO_ACCESS_KEY", "minio")
				.withEnv("MINIO_SECRET_KEY", "minio123")
				.waitingFor(Wait.forHttp("/minio/health/live"))
				.withCreateContainerCmdModifier(
						(Consumer<CreateContainerCmd>) createContainerCmd -> createContainerCmd
								.withHostName("minio"))
				.withLogConsumer(appLog("minio"))
				.withCommand("minio", "server", "/data")
				.withStartupTimeout(Duration.ofSeconds(120))
				.withStartupAttempts(3);

	@BeforeAll
	static void initS3() {
		AWSCredentials credentials = new BasicAWSCredentials("minio", "minio123");
		ClientConfiguration clientConfiguration = new ClientConfiguration();
		String minioAddress = "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
		logger.info("minio:address={}", minioAddress);
		s3Client = AmazonS3ClientBuilder
				.standard()
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(minioAddress, Regions.US_EAST_1.name()))
				.withPathStyleAccessEnabled(true)
				.withClientConfiguration(clientConfiguration)
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.build();

	}

	@BeforeEach
	void configureSource() {
		source = BaseContainerExtension.containerInstance()
				.withEnv("S3_COMMON_ENDPOINT_URL", "http://minio-host:" + minio.getMappedPort(9000))
				.withEnv("S3_COMMON_PATH_STYLE_ACCESS", "true")
				.withEnv("CLOUD_AWS_STACK_AUTO", "false")
				.withEnv("CLOUD_AWS_CREDENTIALS_ACCESS_KEY", "minio")
				.withEnv("CLOUD_AWS_CREDENTIALS_SECRET_KEY", "minio123")
				.withEnv("LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_INTEGRATION", "DEBUG")
				.withEnv("CLOUD_AWS_REGION_STATIC", "us-east-1").log();
		s3Client.createBucket("bucket");
	}

	@Test
	void testLines() {
		startContainer(
				fluentMap().withEntry("FILE_CONSUMER_MODE", "lines"));
		s3Client.putObject(new PutObjectRequest("bucket", "test",
				resourceAsFile("minio/data")));

		await().atMost(DEFAULT_DURATION).until(outputMatcher.payloadMatches((String s) -> s.contains("Bart Simpson")));

	}

	@Test
	void testTaskLaunchRequest() {

		startContainer(fluentMap()
				.withEntry("SPRING_CLOUD_FUNCTION_DEFINITION", "s3Supplier|taskLaunchRequestFunction")
				.withEntry("TASK_LAUNCH_REQUEST_ARG_EXPRESSIONS", "filename=payload")
				.withEntry("TASK_LAUNCH_REQUEST_TASK_NAME", "myTask")
				.withEntry("FILE_CONSUMER_MODE", "ref"));

		s3Client.putObject(new PutObjectRequest("bucket", "test",
				resourceAsFile("minio/data")));
		await().atMost(DEFAULT_DURATION).until(outputMatcher.payloadMatches(s -> s.equals(
				"{\"args\":[\"filename=/tmp/s3-supplier/test\"],\"deploymentProps\":{},\"name\":\"myTask\"}")));
	}

	@Test
	void testListOnly() {
		startContainer(
				fluentMap()
						.withEntry("FILE_CONSUMER_MODE", "ref")
						.withEntry("S3_SUPPLIER_LIST_ONLY", "true"));

		s3Client.putObject(new PutObjectRequest("bucket", "test",
				resourceAsFile("minio/data")));
		await().atMost(DEFAULT_DURATION)
				.until(outputMatcher
						.payloadMatches((String s) -> s.contains("\"bucketName\":\"bucket\",\"key\":\"test\"")));
	}

	private void startContainer(Map<String, String> environment) {
		source.withEnv(environment);
		source.waitingFor(Wait.forLogMessage(".*Started S3Source.*", 1)).start();
		environment.keySet().forEach(k -> source.getEnvMap().remove(k));
	}

	@AfterEach
	void stop() {
		if (s3Client.doesBucketExistV2("bucket")) {
			s3Client.deleteObject("bucket", "test");
			s3Client.deleteBucket("bucket");
		}
		source.stop();
		outputMatcher.clearMessageMatchers();
	}

}
