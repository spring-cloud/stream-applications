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

package org.springframework.cloud.fn.supplier.ftp;

import java.io.File;
import java.time.Duration;
import java.util.regex.Pattern;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @author David Turanski
 * @author Gary Russell
 * @author Artem Bilan
 */
@ConfigurationProperties("ftp.supplier")
@Validated
public class FtpSupplierProperties {

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
	 * Set to true to delete remote files after successful transfer.
	 */
	private boolean deleteRemoteFiles = false;

	/**
	 * The local directory to use for file transfers.
	 */
	private File localDir = new File(System.getProperty("java.io.tmpdir"), "ftp-source");

	/**
	 * Set to true to create the local directory if it does not exist.
	 */
	private boolean autoCreateLocalDir = true;

	/**
	 * A filter pattern to match the names of files to transfer.
	 */
	private String filenamePattern;

	/**
	 * A filter regex pattern to match the names of files to transfer.
	 */
	private Pattern filenameRegex;

	/**
	 * Set to true to preserve the original timestamp.
	 */
	private boolean preserveTimestamp = true;

	/**
	 * Duration of delay when no new files are detected.
	 */
	private Duration delayWhenEmpty = Duration.ofSeconds(1);

	public boolean isAutoCreateLocalDir() {
		return this.autoCreateLocalDir;
	}

	public void setAutoCreateLocalDir(boolean autoCreateLocalDir) {
		this.autoCreateLocalDir = autoCreateLocalDir;
	}

	public boolean isDeleteRemoteFiles() {
		return this.deleteRemoteFiles;
	}

	public void setDeleteRemoteFiles(boolean deleteRemoteFiles) {
		this.deleteRemoteFiles = deleteRemoteFiles;
	}

	@NotNull
	public File getLocalDir() {
		return this.localDir;
	}

	public final void setLocalDir(File localDir) {
		this.localDir = localDir;
	}

	public String getFilenamePattern() {
		return this.filenamePattern;
	}

	public void setFilenamePattern(String filenamePattern) {
		this.filenamePattern = filenamePattern;
	}

	public Pattern getFilenameRegex() {
		return this.filenameRegex;
	}

	public void setFilenameRegex(Pattern filenameRegex) {
		this.filenameRegex = filenameRegex;
	}

	public boolean isPreserveTimestamp() {
		return this.preserveTimestamp;
	}

	public void setPreserveTimestamp(boolean preserveTimestamp) {
		this.preserveTimestamp = preserveTimestamp;
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

	@AssertTrue(message = "filenamePattern and filenameRegex are mutually exclusive")
	public boolean isExclusivePatterns() {
		return !(this.filenamePattern != null && this.filenameRegex != null);
	}

	public Duration getDelayWhenEmpty() {
		return delayWhenEmpty;
	}

	public void setDelayWhenEmpty(Duration delayWhenEmpty) {
		this.delayWhenEmpty = delayWhenEmpty;
	}

}
