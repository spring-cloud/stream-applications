/*
 * Copyright 2018-2020 the original author or authors.
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

package org.springframework.cloud.fn.supplier.sftp;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.jcraft.jsch.ChannelSftp;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.expression.Expression;
import org.springframework.integration.file.remote.aop.RotationPolicy;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Chris Schaefer
 * @author David Turanski
 */
@ConfigurationProperties("sftp.supplier")
@Validated
public class SftpSupplierProperties {
	/**
	 * Session factory properties.
	 */
	private final Factory factory = new Factory();

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
	 * A SpEL expression resolving to the new name remote files must be renamed to after successful transfer.
	 */
	private Expression renameRemoteFilesTo = null;

	/**
	 * The local directory to use for file transfers.
	 */
	private File localDir = new File(System.getProperty("java.io.tmpdir"), "sftp-supplier");

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
	 * Set to true to stream the file rather than copy to a local directory.
	 */
	private boolean stream = false;

	/**
	 * Set to true to return file metadata without the entire payload.
	 */
	private boolean listOnly = false;

	/**
	 * Duration of delay when no new files are detected.
	 */
	private Duration delayWhenEmpty = Duration.ofSeconds(1);

	/**
	 * The maximum number of remote files to fetch per poll; default unlimited. Does not apply
	 * when listing files or building task launch requests.
	 */
	private int maxFetch = Integer.MIN_VALUE;

	/**
	 * True for fair rotation of multiple servers/directories. This is false by default so if
	 * a source has more than one entry, these will be received before the other sources are
	 * visited.
	 */
	private boolean fair;

	/**
	 * A map of factory names to factories.
	 */
	private Map<String, Factory> factories = Collections.emptyMap();

	/**
	 * A list of factory "name.directory" pairs.
	 */
	private String[] directories;

	/**
	 * Sorting specification for remote files listings. If null, order of entries is undefined.
	 * Otherwise, entries are sorted by the specified field and direction, according to the type canonical ordering.
	 */
	private SortSpec sortBy;

	@NotBlank
	public String getRemoteDir() {
		return remoteDir;
	}

	public void setRemoteDir(String remoteDir) {
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

	public Expression getRenameRemoteFilesTo() {
		return renameRemoteFilesTo;
	}

	public void setRenameRemoteFilesTo(Expression renameRemoteFilesTo) {
		this.renameRemoteFilesTo = renameRemoteFilesTo;
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

	public boolean isMultiSource() {
		return this.directories != null && this.directories.length > 0;
	}

	public int getMaxFetch() {
		return maxFetch;
	}

	public void setMaxFetch(int maxFetch) {
		this.maxFetch = maxFetch;
	}

	public boolean isFair() {
		return this.fair;
	}

	public void setFair(boolean fair) {
		this.fair = fair;
	}

	public Map<String, Factory> getFactories() {
		return this.factories;
	}

	public void setFactories(Map<String, Factory> factories) {
		this.factories = factories;
	}

	public String[] getDirectories() {
		return this.directories;
	}

	public void setDirectories(String[] directories) {
		this.directories = directories;
	}

	public boolean isStream() {
		return stream;
	}

	public void setStream(boolean stream) {
		this.stream = stream;
	}

	public Factory getFactory() {
		return factory;
	}

	public Duration getDelayWhenEmpty() {
		return delayWhenEmpty;
	}

	public void setDelayWhenEmpty(Duration delayWhenEmpty) {
		this.delayWhenEmpty = delayWhenEmpty;
	}

	static List<RotationPolicy.KeyDirectory> keyDirectories(SftpSupplierProperties properties) {
		List<RotationPolicy.KeyDirectory> keyDirs = new ArrayList<>();
		Assert.isTrue(properties.getDirectories().length > 0, "At least one key.directory required");
		for (String keyDir : properties.getDirectories()) {
			String[] split = keyDir.split("\\.");
			Assert.isTrue(split.length == 2, () -> "key/directory can only have one '.': " + keyDir);
			keyDirs.add(new RotationPolicy.KeyDirectory(split[0], split[1]));
		}
		return keyDirs;
	}

	@Valid
	public SftpSupplierProperties.SortSpec getSortBy() {
		return sortBy;
	}

	public void setSortBy(SortSpec sortBy) {
		this.sortBy = sortBy;
	}

	@AssertTrue(message = "deleteRemoteFiles must be 'false' when renameRemoteFilesTo is set")
	public boolean isRenameRemoteFilesValid() {
		return renameRemoteFilesTo == null || !deleteRemoteFiles;
	}

	public static class Factory {

		/**
		 * The host name of the server.
		 */
		private String host = "localhost";

		/**
		 * The username to use to connect to the server.
		 */

		private String username;

		/**
		 * The password to use to connect to the server.
		 */
		private String password;

		/**
		 * The port of the server.
		 */
		private int port = 22;

		/**
		 * Resource location of user's private key.
		 */
		private Resource privateKey;

		/**
		 * Passphrase for user's private key.
		 */
		private String passPhrase = "";

		/**
		 * True to allow an unknown or changed key.
		 */
		private boolean allowUnknownKeys = false;

		/**
		 * A SpEL expression resolving to the location of the known hosts file.
		 */
		private Expression knownHostsExpression = null;

		@NotBlank
		public String getHost() {
			return this.host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		@NotBlank
		public String getUsername() {
			return this.username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return this.password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		@Range(min = 0, max = 65535)
		public int getPort() {
			return this.port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public Resource getPrivateKey() {
			return this.privateKey;
		}

		public void setPrivateKey(Resource privateKey) {
			this.privateKey = privateKey;
		}

		public String getPassPhrase() {
			return this.passPhrase;
		}

		public void setPassPhrase(String passPhrase) {
			this.passPhrase = passPhrase;
		}

		public boolean isAllowUnknownKeys() {
			return this.allowUnknownKeys;
		}

		public void setAllowUnknownKeys(boolean allowUnknownKeys) {
			this.allowUnknownKeys = allowUnknownKeys;
		}

		public Expression getKnownHostsExpression() {
			return this.knownHostsExpression;
		}

		public void setKnownHostsExpression(Expression knownHosts) {
			this.knownHostsExpression = knownHosts;
		}

	}

	public static class SortSpec {
		/**
		 * Attribute of the file listing entry to sort by (FILENAME, ATIME: last access time, MTIME: last modified time).
		 */
		private Attribute attribute;

		/**
		 * Sorting direction (ASC or DESC).
		 */
		private Dir dir = Dir.ASC;

		@NotNull
		public Attribute getAttribute() {
			return attribute;
		}

		public void setAttribute(Attribute attribute) {
			this.attribute = attribute;
		}

		@NotNull
		public Dir getDir() {
			return dir;
		}

		public void setDir(Dir dir) {
			this.dir = dir;
		}

		public enum Attribute {
			/**
			 * Filename attribute.
			 */
			FILENAME,

			/**
			 * Last access time attribute.
			 */
			ATIME,

			/**
			 * Last modified time attribute.
			 */
			MTIME
		}

		public enum Dir {
			/**
			 * Ascending sort direction.
			 */
			ASC,

			/**
			 * Descending sort direction.
			 */
			DESC
		}

		private Comparator<ChannelSftp.LsEntry> getAttributeComparator() {
			switch (attribute) {
				case FILENAME:
					return Comparator.comparing(ChannelSftp.LsEntry::getFilename);
				case ATIME:
					return Comparator.comparing(x -> x.getAttrs().getATime());
				case MTIME:
					return Comparator.comparing(x -> x.getAttrs().getMTime());
			}

			throw new UnsupportedOperationException("Unsupported sortBy attribute: " + attribute);
		}

		public Comparator<ChannelSftp.LsEntry> comparator() {
			Comparator<ChannelSftp.LsEntry> comparator = getAttributeComparator();
			return dir == Dir.ASC ? comparator : comparator.reversed();
		}
	}
}
