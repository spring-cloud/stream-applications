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

package org.springframework.cloud.fn.consumer.sftp;

import java.nio.charset.StandardCharsets;

import com.jcraft.jsch.ChannelSftp;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

/**
 * Session factory configuration.
 *
 * @author Gary Russell
 *
 */
public class SftpConsumerSessionFactoryConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public SessionFactory<ChannelSftp.LsEntry> sftpSessionFactory(SftpConsumerProperties properties, BeanFactory beanFactory) {
		DefaultSftpSessionFactory sftpSessionFactory = new DefaultSftpSessionFactory();
		SftpConsumerProperties.Factory factory = properties.getFactory();
		sftpSessionFactory.setHost(factory.getHost());
		sftpSessionFactory.setPort(factory.getPort());
		sftpSessionFactory.setUser(factory.getUsername());
		sftpSessionFactory.setPassword(factory.getPassword());
		sftpSessionFactory.setPrivateKey(factory.getPrivateKey());
		sftpSessionFactory.setPrivateKeyPassphrase(factory.getPassPhrase());
		sftpSessionFactory.setAllowUnknownKeys(factory.isAllowUnknownKeys());
		if (factory.getKnownHostsExpression() != null) {
			// TODO probably a better way
			String knownHosts = factory.getKnownHostsExpression()
					.getValue(IntegrationContextUtils.getEvaluationContext(beanFactory), String.class);
			ByteArrayResource knownHostsResource = new ByteArrayResource(knownHosts.getBytes(StandardCharsets.UTF_8));
			sftpSessionFactory.setKnownHostsResource(knownHostsResource);
		}
		if (factory.getCacheSessions() != null) {
			CachingSessionFactory<ChannelSftp.LsEntry> csf = new CachingSessionFactory<>(sftpSessionFactory);
			return csf;
		}
		else {
			return sftpSessionFactory;
		}
	}
}
