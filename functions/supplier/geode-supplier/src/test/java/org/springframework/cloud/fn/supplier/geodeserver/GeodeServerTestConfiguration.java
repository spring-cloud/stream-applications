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

package org.springframework.cloud.fn.supplier.geodeserver;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Scope;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.ReplicatedRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.CacheServerApplication;

@CacheServerApplication
public class GeodeServerTestConfiguration {

	public static void main(String[] args) {

		AnnotationConfigApplicationContext applicationContext =
				new AnnotationConfigApplicationContext(GeodeServerTestConfiguration.class);

		applicationContext.registerShutdownHook();
	}

	@Bean("myRegion")
	public ReplicatedRegionFactoryBean<Object, Object> myRegion(GemFireCache gemfireCache) {

		ReplicatedRegionFactoryBean<Object, Object> regionFactoryBean = new ReplicatedRegionFactoryBean<>();
		regionFactoryBean.setScope(Scope.DISTRIBUTED_ACK);
		regionFactoryBean.setCache(gemfireCache);

		return regionFactoryBean;
	}
}
