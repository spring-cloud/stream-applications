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

package org.springframework.cloud.fn.common.geode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.pdx.ReflectionBasedAutoSerializer;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.data.gemfire.util.PropertiesBuilder;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * @author David Turanski
 * @author Christian Tzolov
 */
@EnableConfigurationProperties({ GeodeClientCacheProperties.class, GeodeSecurityProperties.class,
		GeodeSslProperties.class, GeodePoolProperties.class })
@Import(InetSocketAddressConverterConfiguration.class)
public class GeodeClientCacheConfiguration {

	private static final String SECURITY_CLIENT = "security-client-auth-init";

	private static final String SECURITY_USERNAME = "security-username";

	private static final String SECURITY_PASSWORD = "security-password";

	@Bean
	public ClientCache clientCache(GeodeClientCacheProperties clientCacheProperties,
			GeodeSecurityProperties securityProperties, GeodeSslProperties sslProperties,
			GeodePoolProperties geodePoolProperties) {

		Properties properties = new Properties();
		PropertiesBuilder pb = new PropertiesBuilder();
		if (StringUtils.hasText(securityProperties.getUsername())
				&& StringUtils.hasText(securityProperties.getPassword())) {

			properties
					.setProperty(SECURITY_CLIENT,
							GeodeSecurityProperties.UserAuthInitialize.class.getName() + ".create");
			properties.setProperty(SECURITY_USERNAME, securityProperties.getUsername());
			properties.setProperty(SECURITY_PASSWORD, securityProperties.getPassword());

		}

		if (sslProperties.isSslEnabled()) {
			pb.add(properties);
			pb.add(this.toGeodeSslProperties(sslProperties));
		}

		ClientCacheFactory clientCacheFactory = new ClientCacheFactory(pb.build());

		if (clientCacheProperties.isPdxReadSerialized()) {
			clientCacheFactory.setPdxSerializer(new ReflectionBasedAutoSerializer(".*"));
			clientCacheFactory.setPdxReadSerialized(true);
		}

		if (geodePoolProperties.getConnectType().equals(GeodePoolProperties.ConnectType.locator)) {
			for (InetSocketAddress address : geodePoolProperties.getHostAddresses()) {
				clientCacheFactory.addPoolLocator(address.getHostName(), address.getPort());
			}
		}
		else {
			for (InetSocketAddress address : geodePoolProperties.getHostAddresses()) {
				clientCacheFactory.addPoolServer(address.getHostName(), address.getPort());
			}
		}

		clientCacheFactory.setPoolSubscriptionEnabled(geodePoolProperties.isSubscriptionEnabled());
		ClientCache clientCache = clientCacheFactory.create();
		clientCache.readyForEvents();
		return clientCache;
	}

	/**
	 * Converts the App Starter properties into Geode native SSL properties.
	 * @param sslProperties App starter properties.
	 * @return Returns the geode native SSL properties.
	 */
	private Properties toGeodeSslProperties(GeodeSslProperties sslProperties) {

		PropertiesBuilder pb = new PropertiesBuilder();

		// locator - SSL communication with and between locators
		// server - SSL communication between clients and servers
		pb.setProperty("ssl-enabled-components", "server,locator");

		pb.setProperty("ssl-keystore", this.resolveRemoteStore(sslProperties.getKeystoreUri(),
				sslProperties.getUserHomeDirectory(), GeodeSslProperties.LOCAL_KEYSTORE_FILE_NAME));
		pb.setProperty("ssl-keystore-password", sslProperties.getSslKeystorePassword());
		pb.setProperty("ssl-keystore-type", sslProperties.getKeystoreType());

		pb.setProperty("ssl-truststore", this.resolveRemoteStore(sslProperties.getTruststoreUri(),
				sslProperties.getUserHomeDirectory(), GeodeSslProperties.LOCAL_TRUSTSTORE_FILE_NAME));
		pb.setProperty("ssl-truststore-password", sslProperties.getSslTruststorePassword());
		pb.setProperty("ssl-truststore-type", sslProperties.getTruststoreType());

		pb.setProperty("ssl-ciphers", sslProperties.getCiphers());

		return pb.build();
	}

	/**
	 * Copy the Trust store specified in the URI into a local accessible file.
	 *
	 * @param storeUri Either Keystore or Truststore remote resource URI
	 * @param userHomeDirectory local root directory to store the keystore and localsore files
	 * @param localStoreFileName local keystore or truststore file name
	 * @return Returns the absolute path of the local trust or keys store file copy
	 */
	private String resolveRemoteStore(Resource storeUri, String userHomeDirectory, String localStoreFileName) {

		File localStoreFile = new File(userHomeDirectory, localStoreFileName);
		try {
			FileCopyUtils.copy(storeUri.getInputStream(), new FileOutputStream(localStoreFile));
			return localStoreFile.getAbsolutePath();
		}
		catch (IOException e) {
			throw new IllegalStateException(String.format("Failed to copy the store from [%s] into %s",
					storeUri.getDescription(), localStoreFile.getAbsolutePath()), e);
		}
	}
}
