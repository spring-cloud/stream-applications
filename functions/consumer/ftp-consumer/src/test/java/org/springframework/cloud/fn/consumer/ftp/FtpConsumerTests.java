/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.cloud.fn.consumer.ftp;

import java.io.File;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.test.support.ftp.FtpTestSupport;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"ftp.consumer.remoteDir = ftpTarget",
				"ftp.factory.username = foo",
				"ftp.factory.password = foo",
				"ftp.consumer.mode = FAIL",
				"ftp.consumer.filenameExpression = payload.name.toUpperCase()"
		})
public class FtpConsumerTests extends FtpTestSupport {

	@Autowired
	Consumer<Message<?>> ftpConsumer;

	@Test
	public void sendFiles() {
		for (int i = 1; i <= 2; i++) {
			String pathname = "/localSource" + i + ".txt";
			String upperPathname = pathname.toUpperCase();
			new File(getTargetRemoteDirectory() + upperPathname).delete();
			assertThat(new File(getTargetRemoteDirectory() + upperPathname).exists()).isFalse();
			ftpConsumer.accept(new GenericMessage<>(new File(getSourceLocalDirectory() + pathname)));
			File expected = new File(getTargetRemoteDirectory() + upperPathname);
			assertThat(expected.exists()).isTrue();
			// verify the uppercase on a case-insensitive file system
			File[] files = getTargetRemoteDirectory().listFiles();
			for (File file : files) {
				assertThat(file.getName().startsWith("LOCALSOURCE")).isTrue();
			}
		}
	}

	@Test
	public void serverRefreshed() { // noop test to test the dirs are refreshed properly
		String pathname = "/LOCALSOURCE1.TXT";
		assertThat(getTargetRemoteDirectory().exists()).isTrue();
		assertThat(new File(getTargetRemoteDirectory() + pathname).exists()).isFalse();
	}

	@SpringBootApplication
	static class FtpConsumerTestApplication {
	}
}
