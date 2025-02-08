package com.eorion.bo.enhancement.emailincidentnotification.handler;

import com.eorion.bo.enhancement.emailincidentnotification.config.EmailIncidentNotificationConfigurationProperty;

public class FailedJobIncidentHandler extends BufferingIncidentHandler {
    public FailedJobIncidentHandler(final EmailIncidentNotificationConfigurationProperty config) {
        super("failedJob", config);
    }
}
