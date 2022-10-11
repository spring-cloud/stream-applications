/*
 * Copyright 2015-2022 the original author or authors.
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

package org.springframework.cloud.fn.consumer.sftp;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.lang.Nullable;

/**
 * Session factory configuration.
 *
 * @author Gary Russell
 * @author Corneil du Plessis
 * @author Chris Bono
 * @author Artem Bilan
 */
public class SftpConsumerSessionFactoryConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public SessionFactory<SftpClient.DirEntry> sftpSessionFactory(
			SftpConsumerProperties properties,
			@Value("#{${sftp.consumer.factory.known-hosts-expression:null}}") @Nullable Resource knownHosts) {

		DefaultSftpSessionFactory sftpSessionFactory = new DefaultSftpSessionFactory();
		SftpConsumerProperties.Factory factory = properties.getFactory();
		sftpSessionFactory.setHost(factory.getHost());
		sftpSessionFactory.setPort(factory.getPort());
		sftpSessionFactory.setUser(factory.getUsername());
		sftpSessionFactory.setPassword(factory.getPassword());
		sftpSessionFactory.setPrivateKey(factory.getPrivateKey());
		sftpSessionFactory.setPrivateKeyPassphrase(factory.getPassPhrase());
		sftpSessionFactory.setAllowUnknownKeys(factory.isAllowUnknownKeys());
		sftpSessionFactory.setKnownHostsResource(knownHosts);
		if (factory.getCacheSessions() != null) {
			return new CachingSessionFactory<>(sftpSessionFactory);
		}
		else {
			return sftpSessionFactory;
		}
	}
}
