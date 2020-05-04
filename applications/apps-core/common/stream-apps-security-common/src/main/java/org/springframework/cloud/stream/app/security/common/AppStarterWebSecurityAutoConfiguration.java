/*
 * Copyright 2018-2019 the original author or authors.
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

package org.springframework.cloud.stream.app.security.common;

import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * @author Christian Tzolov
 * @author Artem Bilan
 *
 * @since 2.1
 */
@Conditional(OnHttpCsrfOrSecurityDisabled.class)
@Configuration
@ConditionalOnClass(WebSecurityConfigurerAdapter.class)
@ConditionalOnMissingBean(WebSecurityConfigurerAdapter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@AutoConfigureBefore(value = { ManagementWebSecurityAutoConfiguration.class, SecurityAutoConfiguration.class })
@EnableConfigurationProperties(AppStarterWebSecurityAutoConfigurationProperties.class)
@EnableWebSecurity
public class AppStarterWebSecurityAutoConfiguration {

	@Bean
	WebSecurityConfigurerAdapter appStarterWebSecurityConfigurerAdapter(
			AppStarterWebSecurityAutoConfigurationProperties securityProperties) {


		return new WebSecurityConfigurerAdapter() {

			@Override
			protected void configure(HttpSecurity http) throws Exception {
				super.configure(http);
				if (!securityProperties.isCsrfEnabled()) {
					http.csrf().disable();
				}
			}

			@Override
			public void configure(WebSecurity builder) {
				if (!securityProperties.isEnabled()) {
					builder.ignoring().antMatchers("/**");
				}
			}

		};
	}

}
