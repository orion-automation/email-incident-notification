package com.eorion.bo.enhancement.emailincidentnotification.handler;

import com.eorion.bo.enhancement.emailincidentnotification.config.EmailIncidentNotificationConfigurationProperty;

public class FailedExternalTaskIncidentHandler extends BufferingIncidentHandler {
    public FailedExternalTaskIncidentHandler(final EmailIncidentNotificationConfigurationProperty config) {
        super("failedExternalTask", config);
    }
}
