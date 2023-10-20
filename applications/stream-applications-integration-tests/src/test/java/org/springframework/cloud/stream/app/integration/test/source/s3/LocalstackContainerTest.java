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

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * The base contract for JUnit tests based on the container for Localstack.
 * The Testcontainers 'reuse' option must be disabled,so, Ryuk container is started
 * and will clean all the containers up from this test suite after JVM exit.
 * Since the Localstack container instance is shared via static property, it is going to be
 * started only once per JVM, therefore the target Docker container is reused automatically.
 *
 * @author Artem Bilan
 * @author Chris Bono
 * @author Corneil du Plessis
 */
@Testcontainers(disabledWithoutDocker = true)
public interface LocalstackContainerTest {

	LocalStackContainer LOCAL_STACK_CONTAINER =
		new LocalStackContainer(DockerImageName.parse("localstack/localstack:2.3"))
			.withNetwork(Network.SHARED)
			.withServices(LocalStackContainer.Service.S3)
			.withServices(LocalStackContainer.Service.EC2)
			.withNetworkAliases("localstack-aws", "localstack")
			.withEnv("PERSISTENCE", "1")
			.withEnv("EAGER_SERVICE_LOADING", "1")
			.withEnv("DEBUG", "1")
			.withEnv("HOSTNAME_EXTERNAL", "localstack")
			.withEnv(Optional.ofNullable(System.getenv("GH_TOKEN"))
				.map(value -> Map.of("GITHUB_API_TOKEN", value))
				.orElse(Map.of()));

	@BeforeAll
	static void startContainer() {
		LOCAL_STACK_CONTAINER.start();
		System.setProperty("software.amazon.awssdk.http.async.service.impl", "software.amazon.awssdk.http.crt.AwsCrtSdkHttpService");
		System.setProperty("software.amazon.awssdk.http.service.impl", "software.amazon.awssdk.http.apache.ApacheSdkHttpService");
	}

	static S3Client s3Client() {
		return applyAwsClientOptions(S3Client.builder().forcePathStyle(true));
	}

	static AwsCredentialsProvider credentialsProvider() {
		return StaticCredentialsProvider.create(
			AwsBasicCredentials.create(LOCAL_STACK_CONTAINER.getAccessKey(), LOCAL_STACK_CONTAINER.getSecretKey()));
	}

	private static <B extends AwsClientBuilder<B, T>, T> T applyAwsClientOptions(B clientBuilder) {
		return clientBuilder
			.region(Region.of(LOCAL_STACK_CONTAINER.getRegion()))
			.credentialsProvider(credentialsProvider())
			.endpointOverride(LOCAL_STACK_CONTAINER.getEndpoint())
			.build();
	}

}
