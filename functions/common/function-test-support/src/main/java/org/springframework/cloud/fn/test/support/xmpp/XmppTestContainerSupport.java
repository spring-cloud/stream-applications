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

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Chris Bono
 */
@Testcontainers(disabledWithoutDocker = true)
public interface XmppTestContainerSupport {

	/**
	 * Default XMPP Host.
	 */
	String XMPP_HOST = "localhost";

	/**
	 * Sample John User setup by config.
	 */
	String JOHN_USER = "john";

	/**
	 * Sample Jane User setup by config.
	 */
	String JANE_USER = "jane";

	/**
	 * Password for sample users.
	 */
	String USER_PW = "secret";

	/**
	 * Default Service Name.
	 */
	String SERVICE_NAME = "localhost";

	/**
	 * The container.
	 */
	GenericContainer<?> XMPP_CONTAINER = new GenericContainer<>("fishbowler/openfire:v4.7.0")
			.withExposedPorts(5222)
			.withClasspathResourceMapping("xmpp/conf", "/var/lib/openfire/conf", BindMode.READ_ONLY)
			.withCommand("-demoboot");

	@BeforeAll
	static void startContainer() {
		XMPP_CONTAINER.start();
	}

	static String getXmppHost() {
		return XMPP_HOST;
	}

	static Integer getXmppMappedPort() {
		return XMPP_CONTAINER.getFirstMappedPort();
	}

}
