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

package org.springframework.cloud.fn.test.support.geode;

import java.util.Optional;
import java.util.function.Consumer;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * Creates and starts a {@link GeodeContainer} using available random ports for locator
 * and server. Runs a {@code Consumer<GeodeContainer>} post processor if provided.
 * @author David Turanski
 */
public class GeodeContainerIntializer {

	private int locatorPort;

	private int cacheServerPort;

	private GeodeContainer geode;

	private Optional<Consumer<GeodeContainer>> postProcessor;

	private final boolean useLocator;

	/**
	 * Create, start, and perform post processing on a {@link GeodeContainer}.
	 * @param postProcessor a {@code Consumer<GeodeContainer>} to run after the container is
	 *     started.
	 */
	public GeodeContainerIntializer(Consumer<GeodeContainer> postProcessor) {
		this(postProcessor, false);
	}

	public GeodeContainerIntializer(Consumer<GeodeContainer> postProcessor, boolean useLocator) {
		this.useLocator = useLocator;

		cacheServerPort = SocketUtils.findAvailableTcpPort();

		locatorPort = SocketUtils.findAvailableTcpPort();

		this.postProcessor = Optional.ofNullable(postProcessor);

		geode = new GeodeContainer(new ImageFromDockerfile()
				.withFileFromClasspath("Dockerfile", "geode/Dockerfile")
				.withBuildArg("CACHE_SERVER_PORT", String.valueOf(cacheServerPort))
				.withBuildArg("LOCATOR_PORT", String.valueOf(locatorPort)),
				locatorPort, cacheServerPort, useLocator);
		startContainer();
	}

	/**
	 * Create and start a {@link GeodeContainer}.
	 */
	public GeodeContainerIntializer() {
		this(null, false);
	}

	private void startContainer() {
		// There is apparently no way to initialize Geode with random port mapping. Ports
		// must be the same on client and server.
		Consumer<CreateContainerCmd> cmd = e -> {
			e.withHostConfig(new HostConfig().withPortBindings(
					new PortBinding(Ports.Binding.bindPort(cacheServerPort), new ExposedPort(cacheServerPort)),
					new PortBinding(Ports.Binding.bindPort(locatorPort), new ExposedPort(locatorPort))));
		};

		// Wait forever

		geode.withCommand("tail", "-f", "/dev/null").withCreateContainerCmdModifier(cmd).start();

		if (useLocator) {
			geode.execGfsh("start locator --name=Locator1 --hostname-for-clients=localhost --port=" + locatorPort);
			geode.execGfsh(geode.connect(),
					"start server --name=Server1 --hostname-for-clients=localhost --server-port=" + cacheServerPort);
		}
		else {
			geode.execGfsh(
					"start server --name=Server1 --hostname-for-clients=localhost --server-port=" + cacheServerPort +
							" --J=-Dgemfire.jmx-manager=true --J=-Dgemfire.jmx-manager-start=true");
		}
		postProcessor.ifPresent(geodeContainerConsumer -> geodeContainerConsumer.accept(geode));
	}

	/**
	 * @return the {@link GeodeContainer} instance.
	 */
	public GeodeContainer geodeContainer() {
		return geode;
	}

}
