package org.springframework.cloud.fn.common.xmpp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.xmpp.config.XmppConnectionFactoryBean;

@Configuration
@EnableConfigurationProperties(XmppConnectionFactoryProperties.class)
public class XmppConnectionFactoryConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public XmppConnectionFactoryBean xmppConnectionFactoryBean(XmppConnectionFactoryProperties properties) {

        XmppConnectionFactoryBean xmppConnectionFactoryBean = new XmppConnectionFactoryBean();
        xmppConnectionFactoryBean.setResource(properties.getResource());
        xmppConnectionFactoryBean.setUser(properties.getUser());
        xmppConnectionFactoryBean.setPassword(properties.getPassword());
        xmppConnectionFactoryBean.setServiceName(properties.getServiceName());
        xmppConnectionFactoryBean.setHost(properties.getHost());
        xmppConnectionFactoryBean.setPort(properties.getPort());
        xmppConnectionFactoryBean.setSubscriptionMode(properties.getSubscriptionMode());
        xmppConnectionFactoryBean.setAutoStartup(properties.isAutoStartup());

        return xmppConnectionFactoryBean;
    }

}
