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

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Chris Bono
 */
public class XmppContainer extends GenericContainer<XmppContainer> {

	private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("fishbowler/openfire:v4.7.0");

	private static final int XMPP_INTERNAL_PORT = 5222;

	public XmppContainer() {
		this(DEFAULT_IMAGE_NAME);
	}

	public XmppContainer(DockerImageName dockerImageName) {
		super(dockerImageName);
		dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
		withExposedPorts(XMPP_INTERNAL_PORT);
		withClasspathResourceMapping("xmpp/conf", "/var/lib/openfire/conf", BindMode.READ_ONLY);
		withCommand("-demoboot");
		setWaitStrategy(Wait.defaultWaitStrategy());
	}

}
