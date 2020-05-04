/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.stream.app.file.remote;

import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.transaction.IntegrationResourceHolder;
import org.springframework.integration.transaction.TransactionSynchronizationProcessor;

/**
 * A {@link TransactionSynchronizationProcessor} that deletes a remote file on
 * success.
 *
 * @author Gary Russell
 *
 */
public class RemoteFileDeletingTransactionSynchronizationProcessor implements TransactionSynchronizationProcessor {

	private final RemoteFileTemplate<?> template;

	private final String remoteFileSeparator;

	/**
	 * Construct an instance with the provided template and separator.
	 * @param template the template.
	 * @param remoteFileSeparator the separator.
	 */
	public RemoteFileDeletingTransactionSynchronizationProcessor(RemoteFileTemplate<?> template,
			String remoteFileSeparator) {
		this.template = template;
		this.remoteFileSeparator = remoteFileSeparator;
	}

	@Override
	public void processBeforeCommit(IntegrationResourceHolder holder) {
	}

	@Override
	public void processAfterRollback(IntegrationResourceHolder holder) {
	}

	@Override
	public void processAfterCommit(IntegrationResourceHolder holder) {
		String remoteDir = (String) holder.getMessage().getHeaders().get(FileHeaders.REMOTE_DIRECTORY);
		String remoteFile = (String) holder.getMessage().getHeaders().get(FileHeaders.REMOTE_FILE);
		this.template.remove(remoteDir + this.remoteFileSeparator + remoteFile);
	}

}
