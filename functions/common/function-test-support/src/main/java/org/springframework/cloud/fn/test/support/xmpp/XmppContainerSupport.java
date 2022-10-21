/*
 * Copyright 2014-2022 the original author or authors.
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

package org.springframework.cloud.fn.test.support.xmpp;

import java.io.File;
import java.io.IOException;

import org.testcontainers.containers.DockerComposeContainer;

import org.springframework.core.io.ClassPathResource;

public abstract class XmppContainerSupport {

	/**
	 * Default XMPP Host.
	 */
	public static final String XMPP_HOST = "localhost";

	/**
	 * Default XMPP Port.
	 */
	public static final int XMPP_PORT = 5222;

	/**
	 * Sample John User setup by config.
	 */
	public static final String JOHN_USER = "john";

	/**
	 * Sample Jane User setup by config.
	 */
	public static final String JANE_USER = "jane";


	/**
	 * Password for sample users.
	 */
	public static final String USER_PW = "secret";

	/**
	 * Default Service Name.
	 */
	public static final String SERVICE_NAME = "localhost";

	static final DockerComposeContainer XMPP_CONTAINER;

	static {

		try {

			File dockerComposeFile = new ClassPathResource("/docker-compose/xmpp/docker-compose.yml").getFile();

			XMPP_CONTAINER =
					new DockerComposeContainer(dockerComposeFile)
							.withExposedService("xmpp", XMPP_PORT);
			XMPP_CONTAINER.start();

		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

}
