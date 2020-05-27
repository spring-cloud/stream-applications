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

package org.springframework.cloud.fn.common.geode;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

/**
 * SSL configuration for Geode client applications.
 * @see <a href=https://geode.apache.org/docs/guide/14/managing/security/implementing_ssl.html>Implementing SSL in Geode</a>
 *
 * @author Christian Tzolov
 */
@ConfigurationProperties("geode.security.ssl")
@Validated
public class GeodeSslProperties {

	private static final String USER_HOME_DIRECTORY = System.getProperty("user.home");
	/**
	 * Name of the local trust store file copied in the local file system.
	 */
	public static final String LOCAL_TRUSTSTORE_FILE_NAME = "trusted.keystore";
	/**
	 * Name of the local trust store file copied in the local file system.
	 */
	public static final String LOCAL_KEYSTORE_FILE_NAME = "keystore.keystore";

	/**
	 * Local directory to cache the truststore and keystore files downloaded form the truststoreUri and keystoreUri locations.
	 */
	@NotBlank
	private String userHomeDirectory = USER_HOME_DIRECTORY;

	/**
	 * Location of the pre-created truststore URI to be used for connecting to the Geode cluster.
	 */
	private Resource truststoreUri;

	/**
	 * Password for accessing the trust store.
	 */
	private String sslTruststorePassword;

	/**
	 * Identifies the type of truststore used for SSL communications (e.g. JKS, PKCS11, etc.).
	 */
	@NotBlank
	private String truststoreType = "JKS";

	/**
	 * Location of the pre-created Keystore URI to be used for connecting to the Geode cluster.
	 */
	private Resource keystoreUri;

	/**
	 * Password for accessing the keys truststore.
	 */
	private String sslKeystorePassword;

	/**
	 * Identifies the type of Keystore used for SSL communications (e.g. JKS, PKCS11, etc.).
	 */
	@NotBlank
	private String keystoreType = "JKS";

	/**
	 * Configures the SSL ciphers used for secure Socket connections as an array of valid cipher names.
	 */
	@NotBlank
	private String ciphers = "any";

	public Resource getTruststoreUri() {
		return truststoreUri;
	}

	public void setTruststoreUri(Resource truststoreUri) {
		this.truststoreUri = truststoreUri;
	}

	public String getSslKeystorePassword() {
		return sslKeystorePassword;
	}

	public void setSslKeystorePassword(String sslKeystorePassword) {
		this.sslKeystorePassword = sslKeystorePassword;
	}

	public String getSslTruststorePassword() {
		return sslTruststorePassword;
	}

	public void setSslTruststorePassword(String sslTruststorePassword) {
		this.sslTruststorePassword = sslTruststorePassword;
	}

	public String getUserHomeDirectory() {
		return userHomeDirectory;
	}

	public void setUserHomeDirectory(String userHomeDirectory) {
		this.userHomeDirectory = userHomeDirectory;
	}

	public String getTruststoreType() {
		return truststoreType;
	}

	public void setTruststoreType(String truststoreType) {
		this.truststoreType = truststoreType;
	}

	public Resource getKeystoreUri() {
		return keystoreUri;
	}

	public void setKeystoreUri(Resource keystoreUri) {
		this.keystoreUri = keystoreUri;
	}

	public String getKeystoreType() {
		return keystoreType;
	}

	public void setKeystoreType(String keystoreType) {
		this.keystoreType = keystoreType;
	}

	public String getCiphers() {
		return ciphers;
	}

	public void setCiphers(String ciphers) {
		this.ciphers = ciphers;
	}

	public boolean isSslEnabled() {
		return this.truststoreUri != null && this.keystoreUri != null;
	}

	@AssertTrue(message = "The truststoreUri and keystoreUri should together be either empty or not!")
	private boolean isStoreUrisConsistent() {
		return ((this.truststoreUri == null) && (this.keystoreUri == null)) ||
				((this.truststoreUri != null) && (this.keystoreUri != null));
	}

	@AssertTrue(message = "The sslKeystorePassword and sslKeystorePassword must not be empty for non empty store URIs!")
	private boolean isStorePasswordRequiredForValidStoreUri() {
		return (!this.isSslEnabled()) ||
				((this.sslKeystorePassword != null) && (this.sslTruststorePassword != null));
	}

}
