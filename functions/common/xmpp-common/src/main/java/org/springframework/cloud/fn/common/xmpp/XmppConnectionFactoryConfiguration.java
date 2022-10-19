/*
 * Copyright 2014-2022 the original author or authors.
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

package org.springframework.cloud.fn.common.xmpp;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppStringUtils;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.xmpp.config.XmppConnectionFactoryBean;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(XmppConnectionFactoryProperties.class)
public class XmppConnectionFactoryConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public XmppConnectionFactoryBean xmppConnectionFactoryBean(XmppConnectionFactoryProperties properties) throws XmppStringprepException {

		XmppConnectionFactoryBean xmppConnectionFactoryBean = new XmppConnectionFactoryBean();
		xmppConnectionFactoryBean.setSubscriptionMode(properties.getSubscriptionMode());

		XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();
		builder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
		builder.setHost(properties.getHost());
		builder.setPort(properties.getPort());
		if (StringUtils.hasText(properties.getResource())) {
			builder.setResource(properties.getResource());
		}

		if (StringUtils.hasText(properties.getServiceName())) {
			builder.setUsernameAndPassword(properties.getUser(), properties.getPassword())
					.setXmppDomain(properties.getServiceName());
		}
		else {
			builder.setUsernameAndPassword(XmppStringUtils.parseLocalpart(properties.getUser()), properties.getPassword())
					.setXmppDomain(properties.getUser());
		}

		xmppConnectionFactoryBean.setConnectionConfiguration(builder.build());

		return xmppConnectionFactoryBean;
	}

}
