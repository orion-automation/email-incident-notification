package com.eorion.bo.enhancement.emailincidentnotification;

import com.eorion.bo.enhancement.emailincidentnotification.config.EmailIncidentNotificationConfigurationProperty;
import com.eorion.bo.enhancement.emailincidentnotification.plugin.EmailIncidentNotificationHandlerPlugin;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
@ComponentScan
@EnableConfigurationProperties(EmailIncidentNotificationConfigurationProperty.class)
@Configuration
@ConditionalOnProperty(value = "eorion.bo.enhancement.ein.enabled", havingValue = "true")
public class EmailIncidentNotificationAutoConfiguration {
    @Bean
    public EmailIncidentNotificationHandlerPlugin emailIncidentNotificationHandlerPlugin(
            EmailIncidentNotificationConfigurationProperty property
    ) {
        return new EmailIncidentNotificationHandlerPlugin(property);
    }
}
