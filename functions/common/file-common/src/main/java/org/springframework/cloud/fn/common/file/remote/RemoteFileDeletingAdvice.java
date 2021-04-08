/*
 * Copyright 2020-2021 the original author or authors.
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

package org.springframework.cloud.fn.common.file.remote;

import org.springframework.integration.aop.MessageSourceMutator;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * A {@link MessageSourceMutator} that deletes a remote file on success.
 *
 * @author David Turanski
 * @author Artem Bilan
 *
 */
public class RemoteFileDeletingAdvice implements MessageSourceMutator {

	private final RemoteFileTemplate<?> template;

	private final String remoteFileSeparator;

	/**
	 * Construct an instance with the provided template and separator.
	 * @param template the template.
	 * @param remoteFileSeparator the separator.
	 */
	public RemoteFileDeletingAdvice(RemoteFileTemplate<?> template,
			String remoteFileSeparator) {
		this.template = template;
		this.remoteFileSeparator = remoteFileSeparator;
	}

	@Nullable
	@Override
	public Message<?> afterReceive(@Nullable Message<?> result, MessageSource<?> source) {
		if (result != null) {
			String remoteDir = (String) result.getHeaders().get(FileHeaders.REMOTE_DIRECTORY);
			String remoteFile = (String) result.getHeaders().get(FileHeaders.REMOTE_FILE);
			this.template.remove(remoteDir + this.remoteFileSeparator + remoteFile);
		}
		return result;
	}
}
