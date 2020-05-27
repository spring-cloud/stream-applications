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

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class GeodeContainer extends GenericContainer {
	private static Logger logger = LoggerFactory.getLogger(GeodeContainer.class);

	/**
	 * The default geode image.
	 */
	private final static String DEFAULT_IMAGE = "apachegeode/geode:1.12.0";

	private final int locatorPort;

	private final int cacheServerPort;

	public GeodeContainer(@NonNull String dockerImageName, int locatorPort, int cacheServerPort) {
		super(dockerImageName);
		this.locatorPort = locatorPort;
		this.cacheServerPort = cacheServerPort;

	}

	public GeodeContainer(@NonNull Future<String> image, int locatorPort, int cacheServerPort) {
		super(image);
		this.locatorPort = locatorPort;
		this.cacheServerPort = cacheServerPort;
	}

	public String connect() {
		return "connect --locator=" + locators();
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
