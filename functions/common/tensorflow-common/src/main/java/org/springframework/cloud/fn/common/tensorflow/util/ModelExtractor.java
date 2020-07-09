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

package org.springframework.cloud.fn.common.tensorflow.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Optional;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;


/**
 * Extracts a pre-trained (frozen) Tensorflow model URI into byte array. The 'http://', 'file://' and 'classpath://'
 * URI schemas are supported.
 *
 * Models can be extract either from raw files or form compressed archives. When  extracted from an archive the model
 * file name can optionally be provided as an URI fragment. For example for resource: http://myarchive.tar.gz#model.pb
 * the myarchive.tar.gz is traversed to uncompress and extract the model.pb file as byte array.
 * If the file name is not provided as URI fragment then the first file in the archive with extension .pb is extracted.
 *
 * @author Christian Tzolov
 */
public class ModelExtractor {

	private static final String DEFAULT_FROZEN_GRAPH_FILE_EXTENSION = ".pb";

	/**
	 * When an archive resource if referred, but no fragment URI is provided (to specify the target file name in
	 * the archive) then the extractor selects the first file in the archive with the extension that match
	 * the frozenGraphFileExtension (defaults to .pb).
	 */
	public final String frozenGraphFileExtension;

	public ModelExtractor() {
		this(DEFAULT_FROZEN_GRAPH_FILE_EXTENSION);
	}

	public ModelExtractor(String frozenGraphFileExtension) {
		this.frozenGraphFileExtension = frozenGraphFileExtension;
	}

	public byte[] getModel(String modelUri) {
		return getModel(new DefaultResourceLoader().getResource(modelUri));
	}

	public byte[] getModel(Resource modelResource) {

		Validate.notNull(modelResource, "Not null model resource is required!");

		try (InputStream is = modelResource.getInputStream(); InputStream bi = new BufferedInputStream(is)) {

			String[] archiveCompressor = detectArchiveAndCompressor(modelResource.getFilename());
			String archive = archiveCompressor[0];
			String compressor = archiveCompressor[1];
			String fragment = modelResource.getURI().getFragment();


			if (StringUtils.isNotBlank(compressor)) {
				try (CompressorInputStream cis = new CompressorStreamFactory().createCompressorInputStream(compressor, bi)) {
					if (StringUtils.isNotBlank(archive)) {
						try (ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(archive, cis)) {
							// Compressor fromMemory Archive
							return findInArchiveStream(fragment, ais);
						}
					}
					else { // Compressor only
						return IOUtils.toByteArray(cis);
					}
				}
			}
			else if (StringUtils.isNotBlank(archive)) { // Archive only
				try (ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(archive, bi)) {
					return findInArchiveStream(fragment, ais);
				}
			}
			else {
				// No compressor nor Archive
				return IOUtils.toByteArray(bi);
			}
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to extract a model from: " + modelResource.getDescription(), e);
		}
	}

	/**
	 * Traverses the Archive to find either an entry that matches the modelFileNameInArchive name (if not empty) or
	 * and entry that ends in .pb if the modelFileNameInArchive is empty.
	 *
	 * @param modelFileNameInArchive Optional name of the archive entry that represents the frozen model file. If empty
	 *                               the archive will be searched for the first entry that ends in .pb
	 * @param archive Archive stream to be traversed
	 *
	 */
	private byte[] findInArchiveStream(String modelFileNameInArchive, ArchiveInputStream archive) throws IOException {
		ArchiveEntry entry;
		while ((entry = archive.getNextEntry()) != null) {
			//System.out.println(entry.getName() + " : " + entry.isDirectory());

			if (archive.canReadEntryData(entry) && !entry.isDirectory()) {
				if ((StringUtils.isNotBlank(modelFileNameInArchive) && entry.getName().endsWith(modelFileNameInArchive)) ||
						(!StringUtils.isNotBlank(modelFileNameInArchive) && entry.getName().endsWith(this.frozenGraphFileExtension))) {
					return IOUtils.toByteArray(archive);
				}
			}
		}
		throw new IllegalArgumentException("No model is found in the archive");
	}

	/**
	 * Detect the Archive and the Compressor from the file extension.
	 *
	 * @param fileName File name with extension.
	 * @return Returns a tuple of the detected (Archive, Compressor). Null stands for not available
	 * archive or detector. The (null, null) response stands for no Archive or Compressor discovered.
	 */
	private String[] detectArchiveAndCompressor(String fileName) {

		String normalizedFileName = fileName.trim().toLowerCase();

		if (normalizedFileName.endsWith(".tar.gz")
				|| normalizedFileName.endsWith(".tgz")
				|| normalizedFileName.endsWith(".taz")) {
			return new String[] { ArchiveStreamFactory.TAR, CompressorStreamFactory.GZIP };
		}
		else if (normalizedFileName.endsWith(".tar.bz2")
				|| normalizedFileName.endsWith(".tbz2")
				|| normalizedFileName.endsWith(".tbz")) {
			return new String[] { ArchiveStreamFactory.TAR, CompressorStreamFactory.BZIP2 };
		}
		else if (normalizedFileName.endsWith(".cpgz")) {
			return new String[] { ArchiveStreamFactory.CPIO, CompressorStreamFactory.GZIP };
		}
		else if (hasArchive(normalizedFileName)) {
			return new String[] { findArchive(normalizedFileName).get(), null };
		}
		else if (hasCompressor(normalizedFileName)) {
			return new String[] { null, findCompressor(normalizedFileName).get() };
		}
		else if (normalizedFileName.endsWith(".gzip")) {
			return new String[] { null, CompressorStreamFactory.GZIP };
		}
		else if (normalizedFileName.endsWith(".bz2")
				|| normalizedFileName.endsWith(".bz")) {
			return new String[] { null, CompressorStreamFactory.BZIP2 };
		}

		// No archived/compressed
		return new String[] { null, null };
	}

	private boolean hasArchive(String normalizedFileName) {
		return findArchive(normalizedFileName).isPresent();
	}

	private Optional<String> findArchive(String normalizedFileName) {
		return new ArchiveStreamFactory().getInputStreamArchiveNames()
				.stream().filter(arch -> normalizedFileName.endsWith("." + arch)).findFirst();
	}

	private boolean hasCompressor(String normalizedFileName) {
		return findCompressor(normalizedFileName).isPresent();
	}

	private Optional<String> findCompressor(String normalizedFileName) {
		return new CompressorStreamFactory().getInputStreamCompressorNames()
				.stream().filter(compressor -> normalizedFileName.endsWith("." + compressor)).findFirst();
	}

	static {
		disableSslVerification();
	}

	private static void disableSslVerification() {
		try {
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			}
			};

			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};

			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		catch (KeyManagementException e) {
			e.printStackTrace();
		}
	}

}
