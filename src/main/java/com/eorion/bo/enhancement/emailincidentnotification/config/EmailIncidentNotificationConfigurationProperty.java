package com.eorion.bo.enhancement.emailincidentnotification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "eorion.bo.enhancement.ein")
public record EmailIncidentNotificationConfigurationProperty(
        @DefaultValue("false") Boolean enabled,
        @DefaultValue("300000") Long intervalMs,
        @DefaultValue("smtp") String protocol,
        String url,
        String fallbackMailReceiver,
        String username,
        String password,
        String subject,
        String host,
        Integer port,
        @DefaultValue("true") Boolean auth,
        @DefaultValue("false") Boolean ssl,
        String trust,
        @DefaultValue("false") Boolean tls,
        @DefaultValue("false") Boolean debug,
        String mailSender,
        String mailBodyTemplate,
        String incidentTemplate,
        @DefaultValue("30000") Integer connectionTimeout,
        @DefaultValue("30000") Integer timeout,
        @DefaultValue("30000") Integer writeTimeout
) {
    @ConstructorBinding
    public EmailIncidentNotificationConfigurationProperty {
    }

}
