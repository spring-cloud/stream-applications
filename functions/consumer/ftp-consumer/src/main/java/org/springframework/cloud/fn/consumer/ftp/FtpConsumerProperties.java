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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("ftp.consumer")
@Validated
public class FtpConsumerProperties {

	/**
	 * The remote FTP directory.
	 */
	private String remoteDir = "/";

	/**
	 * The suffix to use while the transfer is in progress.
	 */
	private String tmpFileSuffix = ".tmp";

	/**
	 * The remote file separator.
	 */
	private String remoteFileSeparator = "/";

	/**
	 * A temporary directory where the file will be written if '#isUseTemporaryFilename()'
	 * is true.
	 */
	private String temporaryRemoteDir = "/";

	/**
	 * Whether or not to create the remote directory.
	 */
	private boolean autoCreateDir = true;

	/**
	 * Action to take if the remote file already exists.
	 */
	private FileExistsMode mode = FileExistsMode.REPLACE;

	/**
	 * Whether or not to write to a temporary file and rename.
	 */
	private boolean useTemporaryFilename = true;

	/**
	 * A SpEL expression to generate the remote file name.
	 */
	private String filenameExpression;

	@NotBlank
	public String getTemporaryRemoteDir() {
		return this.temporaryRemoteDir;
	}

	public void setTemporaryRemoteDir(String temporaryRemoteDir) {
		this.temporaryRemoteDir = temporaryRemoteDir;
	}

	public boolean isAutoCreateDir() {
		return this.autoCreateDir;
	}

	public void setAutoCreateDir(boolean autoCreateDir) {
		this.autoCreateDir = autoCreateDir;
	}

	@NotNull
	public FileExistsMode getMode() {
		return this.mode;
	}

	public void setMode(FileExistsMode mode) {
		this.mode = mode;
	}

	public boolean isUseTemporaryFilename() {
		return this.useTemporaryFilename;
	}

	public void setUseTemporaryFilename(boolean useTemporaryFilename) {
		this.useTemporaryFilename = useTemporaryFilename;
	}

	public String getFilenameExpression() {
		return this.filenameExpression;
	}

	public void setFilenameExpression(String filenameExpression) {
		this.filenameExpression = filenameExpression;
	}

	@NotBlank
	public String getRemoteDir() {
		return this.remoteDir;
	}

	public final void setRemoteDir(String remoteDir) {
		this.remoteDir = remoteDir;
	}

	@NotBlank
	public String getTmpFileSuffix() {
		return this.tmpFileSuffix;
	}

	public void setTmpFileSuffix(String tmpFileSuffix) {
		this.tmpFileSuffix = tmpFileSuffix;
	}

	@NotBlank
	public String getRemoteFileSeparator() {
		return this.remoteFileSeparator;
	}

	public void setRemoteFileSeparator(String remoteFileSeparator) {
		this.remoteFileSeparator = remoteFileSeparator;
	}
}
