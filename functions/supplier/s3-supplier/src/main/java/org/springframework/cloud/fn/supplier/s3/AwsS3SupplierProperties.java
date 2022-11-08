/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.fn.supplier.s3;

import java.io.File;
import java.util.regex.Pattern;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @author Artem Bilan
 */
@ConfigurationProperties("s3.supplier")
@Validated
public class AwsS3SupplierProperties {

	/**
	 * AWS S3 bucket resource.
	 */
	private String remoteDir = "bucket";

	/**
	 * Temporary file suffix.
	 */
	private String tmpFileSuffix = ".tmp";

	/**
	 * Remote File separator.
	 */
	private String remoteFileSeparator = "/";

	/**
	 * Delete or not remote files after processing.
	 */
	private boolean deleteRemoteFiles = false;

	/**
	 * The local directory to store files.
	 */
	private File localDir = new File(System.getProperty("java.io.tmpdir"), "s3-supplier");

	/**
	 * Create or not the local directory.
	 */
	private boolean autoCreateLocalDir = true;

	/**
	 * The pattern to filter remote files.
	 */
	private String filenamePattern;

	/**
	 * The regexp to filter remote files.
	 */
	private Pattern filenameRegex;

	/**
	 * To transfer or not the timestamp of the remote file to the local one.
	 */
	private boolean preserveTimestamp = true;

	/**
	 * Set to true to return s3 object metadata without copying file to a local directory.
	 */
	private boolean listOnly = false;

	@Length(min = 3)
	public String getRemoteDir() {
		return this.remoteDir;
	}

	public final void setRemoteDir(String remoteDir) {
		this.remoteDir = remoteDir;
	}

	@NotBlank
	public String getTmpFileSuffix() {
		return tmpFileSuffix;
	}

	public void setTmpFileSuffix(String tmpFileSuffix) {
		this.tmpFileSuffix = tmpFileSuffix;
	}

	@NotBlank
	public String getRemoteFileSeparator() {
		return remoteFileSeparator;
	}

	public void setRemoteFileSeparator(String remoteFileSeparator) {
		this.remoteFileSeparator = remoteFileSeparator;
	}

	public boolean isAutoCreateLocalDir() {
		return autoCreateLocalDir;
	}

	public void setAutoCreateLocalDir(boolean autoCreateLocalDir) {
		this.autoCreateLocalDir = autoCreateLocalDir;
	}

	public boolean isDeleteRemoteFiles() {
		return deleteRemoteFiles;
	}

	public void setDeleteRemoteFiles(boolean deleteRemoteFiles) {
		this.deleteRemoteFiles = deleteRemoteFiles;
	}

	@NotNull
	public File getLocalDir() {
		return localDir;
	}

	public final void setLocalDir(File localDir) {
		this.localDir = localDir;
	}

	public String getFilenamePattern() {
		return filenamePattern;
	}

	public void setFilenamePattern(String filenamePattern) {
		this.filenamePattern = filenamePattern;
	}

	public Pattern getFilenameRegex() {
		return filenameRegex;
	}

	public void setFilenameRegex(Pattern filenameRegex) {
		this.filenameRegex = filenameRegex;
	}

	public boolean isPreserveTimestamp() {
		return preserveTimestamp;
	}

	public void setPreserveTimestamp(boolean preserveTimestamp) {
		this.preserveTimestamp = preserveTimestamp;
	}

	@AssertTrue(message = "filenamePattern and filenameRegex are mutually exclusive")
	public boolean isExclusivePatterns() {
		return !(this.filenamePattern != null && this.filenameRegex != null);
	}

	public boolean isListOnly() {
		return listOnly;
	}

	public void setListOnly(boolean listOnly) {
		this.listOnly = listOnly;
	}
}
