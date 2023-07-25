/*
 * Copyright 2020-2023 the original author or authors.
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

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.app.test.integration.OutputMatcher;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.app.test.integration.junit.jupiter.BaseContainerExtension;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.integration.test.common.Configuration.DEFAULT_DURATION;
import static org.springframework.cloud.stream.app.test.integration.AppLog.appLog;
import static org.springframework.cloud.stream.app.test.integration.FluentMap.fluentMap;
import static org.springframework.cloud.stream.app.test.integration.StreamAppContainerTestUtils.resourceAsFile;

@Tag("integration")
@ExtendWith(BaseContainerExtension.class)
abstract class S3SourceTests {
	private static final Logger logger = LoggerFactory.getLogger(S3SourceTests.class);

	private static S3Client s3Client;

	private static String minioAddress;

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
		minioAddress = "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
		AwsCredentials credentials = AwsBasicCredentials.create("minio", "minio123");
		logger.info("minio:address={}", minioAddress);
		s3Client = S3Client.builder()
				.endpointOverride(URI.create(minioAddress))
				.region(Region.US_EAST_1)
				.forcePathStyle(true)
				.credentialsProvider(StaticCredentialsProvider.create(credentials))
				.build();

	}

	@BeforeEach
	void configureSource() {
		source = BaseContainerExtension.containerInstance()
				.withEnv("SPRING_CLOUD_AWS_S3_ENDPOINT", minioAddress)
				.withEnv("SPRING_CLOUD_AWS_S3_PATH_STYLE_ACCESS_ENABLED", "true")
				.withEnv("SPRING_CLOUD_AWS_CREDENTIALS_ACCESS_KEY", "minio")
				.withEnv("SPRING_CLOUD_AWS_CREDENTIALS_SECRET_KEY", "minio123")
				.withEnv("LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_INTEGRATION", "DEBUG")
				.withEnv("SPRING_CLOUD_AWS_REGION_STATIC", Region.US_EAST_1.id())
				.log();

		s3Client.createBucket(r -> r.bucket("bucket"));
	}

	@Test
	void testLines() {
		startContainer(fluentMap().withEntry("FILE_CONSUMER_MODE", "lines"));
		s3Client.putObject(r -> r.bucket("bucket").key("test"), resourceAsFile("minio/data").toPath());

		await().atMost(DEFAULT_DURATION).until(outputMatcher.payloadMatches((String s) -> s.contains("Bart Simpson")));

	}

	@Test
	void testTaskLaunchRequest() {

		startContainer(fluentMap()
				.withEntry("SPRING_CLOUD_FUNCTION_DEFINITION", "s3Supplier|taskLaunchRequestFunction")
				.withEntry("TASK_LAUNCH_REQUEST_ARG_EXPRESSIONS", "filename=payload")
				.withEntry("TASK_LAUNCH_REQUEST_TASK_NAME", "myTask")
				.withEntry("FILE_CONSUMER_MODE", "ref"));

		s3Client.putObject(r -> r.bucket("bucket").key("test"), resourceAsFile("minio/data").toPath());

		await().atMost(DEFAULT_DURATION).until(outputMatcher.payloadMatches(s -> s.equals(
				"{\"args\":[\"filename=/tmp/s3-supplier/test\"],\"deploymentProps\":{},\"name\":\"myTask\"}")));
	}

	@Test
	void testListOnly() {
		startContainer(
				fluentMap()
						.withEntry("FILE_CONSUMER_MODE", "ref")
						.withEntry("S3_SUPPLIER_LIST_ONLY", "true"));

		s3Client.putObject(r -> r.bucket("bucket").key("test"), resourceAsFile("minio/data").toPath());

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
		try {
			s3Client.headBucket(r -> r.bucket("bucket"));
			// NoSuchBucketException - no deletion attempts
			s3Client.deleteObject(r -> r.bucket("bucket").key("test"));
			s3Client.deleteBucket(r -> r.bucket("bucket"));
		}
		catch (NoSuchBucketException exception) {
			logger.warn("No bucket 'bucket' to remove");
		}

		source.stop();
		outputMatcher.clearMessageMatchers();
	}

}
