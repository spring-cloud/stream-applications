/*
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.fn.consumer.cassandra.cluster;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Scanner;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.cassandra.config.CqlSessionFactoryBean;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.data.cassandra.core.cql.CqlTemplate;
import org.springframework.data.cassandra.core.cql.ReactiveCqlOperations;
import org.springframework.data.cassandra.core.cql.generator.CreateKeyspaceCqlGenerator;
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification;
import org.springframework.util.StringUtils;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import reactor.core.publisher.Flux;

/**
 * @author Artem Bilan
 * @author Thomas Risberg
 * @author Rob Hardt
 */
@Configuration
@EnableConfigurationProperties(CassandraClusterProperties.class)
@Import(CassandraAppClusterConfiguration.CassandraPackageRegistrar.class)
public class CassandraAppClusterConfiguration {

	@Bean
	public CqlSessionBuilderCustomizer clusterBuilderCustomizer(
			CassandraClusterProperties cassandraClusterProperties) {

		PropertyMapper map = PropertyMapper.get();
		return builder ->
				map.from(cassandraClusterProperties::isSkipSslValidation)
						.whenTrue()
						.toCall(() -> {
							try {
								builder.withSslContext(TrustAllSSLContextFactory.getSslContext());
							}
							catch (NoSuchAlgorithmException | KeyManagementException e) {
								throw new BeanInitializationException(
										"Unable to configure a Cassandra cluster using SSL.", e);
							}

						});
	}

	@Bean
	@ConditionalOnProperty("cassandra.cluster.create-keyspace")
	public Object keyspaceCreator(CassandraProperties cassandraProperties, CqlSessionBuilder cqlSessionBuilder) {
		CreateKeyspaceSpecification createKeyspaceSpecification =
				CreateKeyspaceSpecification
						.createKeyspace(cassandraProperties.getKeyspaceName())
						.withSimpleReplication()
						.ifNotExists();

		String createKeySpaceQuery = new CreateKeyspaceCqlGenerator(createKeyspaceSpecification).toCql();
		CqlSession systemSession =
				cqlSessionBuilder.withKeyspace(CqlSessionFactoryBean.CASSANDRA_SYSTEM_SESSION).build();

		CqlTemplate template = new CqlTemplate(systemSession);
		template.execute(createKeySpaceQuery);

		return null;
	}

	@Bean
	@Lazy
	@DependsOn("keyspaceCreator")
	public CqlSession cassandraSession(CqlSessionBuilder cqlSessionBuilder) {
		return cqlSessionBuilder.build();
	}


	@Bean
	@ConditionalOnProperty("cassandra.cluster.init-script")
	public Object keyspaceInitializer(CassandraClusterProperties cassandraClusterProperties,
			ReactiveCassandraTemplate reactiveCassandraTemplate) throws IOException {

		String scripts =
				new Scanner(cassandraClusterProperties.getInitScript().getInputStream(),
						StandardCharsets.UTF_8.name())
						.useDelimiter("\\A")
						.next();

		ReactiveCqlOperations reactiveCqlOperations =
				reactiveCassandraTemplate.getReactiveCqlOperations();

		Flux.fromArray(StringUtils.delimitedListToStringArray(scripts, ";", "\r\n\f"))
				.filter(StringUtils::hasText) // an empty String after the last ';'
				.flatMap(script -> reactiveCqlOperations.execute(script + ";"))
				.blockLast();

		return null;

	}

	static class CassandraPackageRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

		private Environment environment;

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {

			Binder.get(this.environment)
					.bind("cassandra.cluster.entity-base-packages", String[].class)
					.map(Arrays::asList)
					.ifBound(packagesToScan -> EntityScanPackages.register(registry, packagesToScan));
		}

	}

}
