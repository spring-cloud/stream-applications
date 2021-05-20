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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A Test Container that starts a Geode Locator and Server on configured ports. This also
 * provides methods for executing one or more Gfsh commands.
 */
public class GeodeContainer extends GenericContainer {
	private static Logger logger = LoggerFactory.getLogger(GeodeContainer.class);

	private final int locatorPort;

	private final int cacheServerPort;

	private final boolean useLocator;

	/**
	 * Create a Geode container from a Docker image.
	 * @param dockerImageName the name of the image.
	 * @param locatorPort the locator port.
	 * @param cacheServerPort the cache server port.
	 * @param useLocator set to use a locator.
	 */
	public GeodeContainer(@NonNull String dockerImageName, int locatorPort, int cacheServerPort, boolean useLocator) {
		super(dockerImageName);
		this.locatorPort = locatorPort;
		this.cacheServerPort = cacheServerPort;
		this.useLocator = useLocator;
	}

	public GeodeContainer(@NonNull String dockerImageName, int locatorPort, int cacheServerPort) {
		this(dockerImageName, locatorPort, cacheServerPort, false);
	}

	/**
	 * Create a Geode Container from a {@code Future<String>}. Test containers provides some
	 * implementations as image builders, such as
	 * {@link org.testcontainers.images.builder.ImageFromDockerfile}.
	 * @param image the image builder.
	 * @param locatorPort the locator port.
	 * @param cacheServerPort the server port.
	 * @param useLocator set to use a locator.
	 */
	public GeodeContainer(@NonNull Future<String> image, int locatorPort, int cacheServerPort, boolean useLocator) {
		super(image);
		this.locatorPort = locatorPort;
		this.cacheServerPort = cacheServerPort;
		this.useLocator = useLocator;
	}

	public GeodeContainer(@NonNull Future<String> image, int locatorPort, int cacheServerPort) {
		this(image, locatorPort, cacheServerPort, false);
	}

	/**
	 * A convenience method to connect to a locator with Gfsh.
	 * @return the connect command String.
	 */
	public String connect() {
		return useLocator ? "connect --locator=" + locators() : "connect --jmx-manager=localhost[1099]";
	}

	/**
	 * Get the locator port.
	 * @return the locator port.
	 */
	public int getLocatorPort() {
		return locatorPort;
	}

	/**
	 * Get the cache server port.
	 * @return the cache server port.
	 */
	public int getCacheServerPort() {
		return cacheServerPort;
	}

	/**
	 *
	 * @return Geode locators as host[port],...
	 */
	public String locators() {
		return "localhost[" + locatorPort + "]";
	}

	/**
	 * Invoke the `gfsh` shell, Connect to the locator and execute the commands.
	 * @param command a list of commands to execute in a single `gfsh` invocation.
	 * @return the {@link org.testcontainers.containers.Container.ExecResult}
	 */
	public ExecResult connectAndExecGfsh(String... command) {
		ArrayList<String> args = new ArrayList<>(Arrays.asList(command));
		args.add(0, connect());
		return execInContainer(Gfsh.command(args.toArray(new String[args.size()])).commandParts());
	}

	/**
	 * Invoke the `gfsh` shell, and execute the commands.
	 * @param command a list of commands to execute in a single `gfsh` invocation.
	 * @return the {@link org.testcontainers.containers.Container.ExecResult}
	 */
	public ExecResult execGfsh(String... command) {
		return execInContainer(Gfsh.command(command).commandParts());
	}

	/**
	 * Executes a command in the container, logging stdout and stderr and wrapping checked
	 * exceptions.
	 * @see GenericContainer#execInContainer(String...)
	 * @param command the command to execute.
	 * @return the {@link org.testcontainers.containers.Container.ExecResult}
	 */
	@Override
	public ExecResult execInContainer(String... command) {
		try {
			ExecResult execResult = super.execInContainer(command);
			logger.debug("stdout: {}", execResult.getStdout());
			if (execResult.getExitCode() != 0) {
				logger.warn("stdout: {}", execResult.getStdout());
				logger.warn("stderr: {}", execResult.getStderr());
			}
			return execResult;
		}
		catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Builds a Gfsh command.
	 */
	public final static class Gfsh {

		public static Command command(String... gfshCommands) {
			return new Command(gfshCommands);
		}

		public final static class Command {
			private final List<String> commandParts = new LinkedList<>();

			private Command(String... gfshCommands) {
				Assert.notEmpty(gfshCommands, "at least one command is required");
				for (String gfshCommand : gfshCommands) {
					Assert.hasText(gfshCommand, "command must contain text");
					if (commandParts.size() == 0) {
						commandParts.add("gfsh");
					}
					commandParts.add("-e");
					commandParts.add(gfshCommand);
				}
			}

			public String[] commandParts() {
				return commandParts.toArray(new String[commandParts.size()]);
			}

			public String toString() {
				return StringUtils.collectionToDelimitedString(commandParts, ",");
			}
		}
	}

}
