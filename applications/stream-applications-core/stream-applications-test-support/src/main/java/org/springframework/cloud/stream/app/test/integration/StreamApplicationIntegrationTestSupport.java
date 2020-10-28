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

package org.springframework.cloud.stream.app.test.integration;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.SocketUtils;

/**
 * Support utility for stream application integration testing .
 * @author David Turanski
 */
@Testcontainers
@Component
public abstract class StreamApplicationIntegrationTestSupport {

	protected static final String DOCKER_ORG = "springcloudstream";

	@Autowired
	private AbstractTestTopicListener testListener;

	protected static String prePackagedStreamAppImageName(String appName, String binderName, String version) {
		return DOCKER_ORG + "/" + appName + "-" + binderName + ":" + version;
	}

	public static String localHostAddress() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		}
		catch (UnknownHostException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	protected static File resourceAsFile(String path) {
		try {
			return new ClassPathResource(path).getFile();
		}
		catch (IOException e) {
			throw new IllegalStateException("Unable to access resource " + path);
		}
	}

	protected static final int findAvailablePort() {
		return SocketUtils.findAvailableTcpPort(10000, 20000);
	}

	protected Callable<Boolean> verifyOutputMessages() {
		return () -> testListener.isVerified().get();
	}

	protected <P> Callable<Boolean> verifyOutputPayload(Predicate<P> outputVerifier) {
		testListener.addOutputPayloadVerifier(outputVerifier);
		return () -> testListener.isVerified().get();
	}

	protected Callable<Boolean> verifyOutputMessage(Predicate<Message<?>> outputVerifier) {
		testListener.addOutputMessageVerifier(outputVerifier);
		return () -> testListener.isVerified().get();
	}
}
