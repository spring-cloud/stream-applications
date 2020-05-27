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

package org.springframework.cloud.fn.common.geode;

import java.util.List;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.client.ClientCache;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.client.Interest;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * Client region configuration common to Geode functions. This configures the
 * 'regionName', 'spring.application.name' by default and injects the pool. Also, any
 * beans of type {@link Interest} will be registered to the client region to control which
 * keys will be automatically synced to the client. At least one of these is required for
 * Geode Suppliers.
 *
 * @author David Turanski
 */
@Configuration
@Import(GeodeClientCacheConfiguration.class)
@EnableConfigurationProperties(GeodeRegionProperties.class)
public class GeodeClientRegionConfiguration {

	@Bean(name = "clientRegion")
	@SuppressWarnings({ "rawtype", "unchecked" })
	public ClientRegionFactoryBean clientRegionFactoryBean(ClientCache clientCache,
			@Nullable List<Interest> keyInterests, GeodeRegionProperties properties) {
		ClientRegionFactoryBean clientRegionFactoryBean = new ClientRegionFactoryBean();
		clientRegionFactoryBean.setRegionName(properties.getRegionName());
		clientRegionFactoryBean.setDataPolicy(DataPolicy.EMPTY);
		if (!CollectionUtils.isEmpty(keyInterests)) {
			clientRegionFactoryBean.setInterests(keyInterests.toArray(new Interest[keyInterests.size()]));
		}

		try {
			clientRegionFactoryBean.setCache(clientCache);
		}
		catch (Exception e) {
			throw new BeanCreationException(e.getMessage(), e);
		}
		return clientRegionFactoryBean;
	}

}
