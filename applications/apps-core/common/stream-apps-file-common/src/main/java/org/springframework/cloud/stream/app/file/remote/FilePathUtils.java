/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.app.file.remote;

import java.nio.file.Paths;

import org.springframework.integration.file.FileHeaders;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * @author David Turanski
 **/
public abstract class FilePathUtils {
	/**
	 * Returns a remote file path for a message with a file name as payload and {@link FileHeaders#REMOTE_DIRECTORY}
	 * included as a message header.
	 *
	 * @param message the message containing the header.
	 * @return the file path.
	 */
	@Nullable
	public static String getRemoteFilePath(Message message) {
		if (message.getHeaders().containsKey(FileHeaders.REMOTE_DIRECTORY)) {
			String filename = (String) message.getPayload();
			String remoteDirectory = (String) message.getHeaders().get(FileHeaders.REMOTE_DIRECTORY);
			return getPath(remoteDirectory, filename);
		}
		return null;
	}

	public static String getLocalFilePath(String localDirectory, String filename) {
		if (localDirectory != null) {
			return getPath(localDirectory, filename);
		}
		return filename;
	}

	private static String getPath(String dirName, String fileName) {
		return Paths.get(dirName, fileName).toString();
	}
}
