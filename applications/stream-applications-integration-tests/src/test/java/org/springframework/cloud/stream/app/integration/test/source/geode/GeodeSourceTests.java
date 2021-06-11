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

package org.springframework.cloud.stream.app.integration.test.source.geode;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.fn.test.support.geode.GeodeContainer;
import org.springframework.cloud.stream.app.test.integration.OutputMatcher;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainerTestUtils;
import org.springframework.cloud.stream.app.test.integration.junit.jupiter.BaseContainerExtension;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.integration.test.common.Configuration.DEFAULT_DURATION;
@Tag("integration")
@ExtendWith(BaseContainerExtension.class)
abstract class GeodeSourceTests {
	private static int locatorPort = StreamAppContainerTestUtils.findAvailablePort();

	private static int cacheServerPort = StreamAppContainerTestUtils.findAvailablePort();

	private static Region<Object, Object> clientRegion;

	private static ClientCache clientCache;

	protected static StreamAppContainer source;

	@Autowired
	private OutputMatcher outputMatcher;

	@Container
	private static final GeodeContainer geode = (GeodeContainer) new GeodeContainer(new ImageFromDockerfile()
			.withFileFromClasspath("Dockerfile", "geode-test/Dockerfile")
			.withBuildArg("CACHE_SERVER_PORT", String.valueOf(cacheServerPort))
			.withBuildArg("LOCATOR_PORT", String.valueOf(locatorPort)),
			locatorPort, cacheServerPort)
					.withCreateContainerCmdModifier(
							(Consumer<CreateContainerCmd>) createContainerCmd -> createContainerCmd
									.withHostName("geode").withHostConfig(new HostConfig().withPortBindings(
											new PortBinding(Ports.Binding.bindPort(cacheServerPort),
													new ExposedPort(cacheServerPort)),
											new PortBinding(Ports.Binding.bindPort(locatorPort),
													new ExposedPort(locatorPort)))))
					.withCommand("tail", "-f", "/dev/null")
					.withStartupTimeout(DEFAULT_DURATION.multipliedBy(2));

	@BeforeAll
	static void initializeGeodeCacheThenStartSource() {
		// Not using locator is faster.
		System.out.println(geode.execGfsh(
				"start server --name=Server1 " + "--hostname-for-clients=geode" + " --server-port="
						+ cacheServerPort + " --J=-Dgemfire.jmx-manager=true --J=-Dgemfire.jmx-manager-start=true")
				.getStdout());
		System.out.println(geode.execGfsh("connect --jmx-manager=localhost[1099]",
				"create region --name=myRegion --type=REPLICATE").getStdout());
		clientCache = new ClientCacheFactory().addPoolServer("localhost", cacheServerPort)
				.create();
		clientCache.readyForEvents();
		clientRegion = clientCache
				.createClientRegionFactory(ClientRegionShortcut.PROXY)
				.create("myRegion");

		source = BaseContainerExtension.containerInstance().withEnv("GEODE_POOL_CONNECT_TYPE", "server")
				.withEnv("GEODE_REGION_REGION_NAME", "myRegion")
				.withEnv("GEODE_POOL_HOST_ADDRESSES",
						StreamAppContainerTestUtils.localHostAddress() + ":" + cacheServerPort)
				.waitingFor(Wait.forLogMessage(".*Started GeodeSource.*", 1));
		source.start();
	}

	@Test
	void test() throws InterruptedException {
		String random = UUID.randomUUID().toString();
		clientRegion.put(random, random);
		await().atMost(Duration.ofSeconds(30))
				.until(outputMatcher.payloadMatches((String s) -> s.contains(random)));
	}

	@AfterAll
	static void cleanup() {
		source.stop();
		clientCache.close();
	}
}
