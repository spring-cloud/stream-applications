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

import java.util.HashMap;
import java.util.Map;

import com.jcraft.jsch.ChannelSftp.LsEntry;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.file.remote.aop.StandardRotationPolicy;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.DelegatingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.lang.Nullable;

/**
 * Session factory configuration.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author David Turanski
 *
 */
public class SftpSupplierFactoryConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public SessionFactory<LsEntry> sftpSessionFactory(SftpSupplierProperties properties, BeanFactory beanFactory) {
		return buildFactory(beanFactory, properties.getFactory());
	}

	@Bean
	public DelegatingFactoryWrapper delegatingFactoryWrapper(SftpSupplierProperties properties,
			SessionFactory<LsEntry> defaultFactory, BeanFactory beanFactory) {
		return new DelegatingFactoryWrapper(properties, defaultFactory, beanFactory);
	}

	@Bean
	StandardRotationPolicy rotationPolicy(SftpSupplierProperties properties, DelegatingFactoryWrapper factory) {

		return properties.isMultiSource()
				? new StandardRotationPolicy(factory.getFactory(),
				SftpSupplierProperties.keyDirectories(properties), properties.isFair())
				: null;
	}

	@Bean
	public SftpSupplierRotator rotatingAdvice(SftpSupplierProperties properties,
			@Nullable StandardRotationPolicy rotationPolicy) {
		return properties.isMultiSource()
				? new SftpSupplierRotator(properties, rotationPolicy)
				: null;
	}

	static SessionFactory<LsEntry> buildFactory(BeanFactory beanFactory, SftpSupplierProperties.Factory factory) {
		DefaultSftpSessionFactory sftpSessionFactory = new DefaultSftpSessionFactory(true);
		sftpSessionFactory.setHost(factory.getHost());
		sftpSessionFactory.setPort(factory.getPort());
		sftpSessionFactory.setUser(factory.getUsername());
		sftpSessionFactory.setPassword(factory.getPassword());
		sftpSessionFactory.setPrivateKey(factory.getPrivateKey());
		sftpSessionFactory.setPrivateKeyPassphrase(factory.getPassPhrase());
		sftpSessionFactory.setAllowUnknownKeys(factory.isAllowUnknownKeys());
		if (factory.getKnownHostsExpression() != null) {
			String path = factory.getKnownHostsExpression()
					.getValue(IntegrationContextUtils.getEvaluationContext(beanFactory), String.class);
			sftpSessionFactory.setKnownHostsResource(new FileSystemResource(path));
		}

		return new CachingSessionFactory<>(sftpSessionFactory);
	}

	public final static class DelegatingFactoryWrapper implements DisposableBean {

		private final DelegatingSessionFactory<LsEntry> delegatingSessionFactory;

		private final Map<Object, SessionFactory<LsEntry>> factories = new HashMap<>();

		DelegatingFactoryWrapper(SftpSupplierProperties properties, SessionFactory<LsEntry> defaultFactory,
				BeanFactory beanFactory) {
			properties.getFactories().forEach((key, factory) -> {
				this.factories.put(key, SftpSupplierFactoryConfiguration.buildFactory(beanFactory, factory));
			});
			this.delegatingSessionFactory = new DelegatingSessionFactory<>(this.factories, defaultFactory);
		}

		public DelegatingSessionFactory<LsEntry> getFactory() {
			return this.delegatingSessionFactory;
		}

		@Override
		public void destroy() {
			this.factories.values().forEach(f -> {
				if (f instanceof DisposableBean) {
					try {
						((DisposableBean) f).destroy();
					}
					catch (Exception e) {
						// empty
					}
				}
			});
		}

	}

}
