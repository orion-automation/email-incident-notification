package com.eorion.bo.enhancement.emailincidentnotification.plugin;

import com.eorion.bo.enhancement.emailincidentnotification.config.EmailIncidentNotificationConfigurationProperty;
import com.eorion.bo.enhancement.emailincidentnotification.handler.BufferingIncidentHandler;
import com.eorion.bo.enhancement.emailincidentnotification.handler.FailedExternalTaskIncidentHandler;
import com.eorion.bo.enhancement.emailincidentnotification.handler.FailedJobIncidentHandler;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.incident.IncidentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.util.Arrays.asList;

public class EmailIncidentNotificationHandlerPlugin extends AbstractProcessEnginePlugin {
    private final static Logger LOGGER = LoggerFactory.getLogger(EmailIncidentNotificationHandlerPlugin.class);

    private final EmailIncidentNotificationConfigurationProperty property;

    public EmailIncidentNotificationHandlerPlugin(EmailIncidentNotificationConfigurationProperty property) {
        this.property = property;
    }

    @Override
    public void preInit(final ProcessEngineConfigurationImpl processEngineConfig) {
        final BufferingIncidentHandler failedJobHandler = createFailedJobIncidentHandler();
        final BufferingIncidentHandler failedExternalTaskHandler = createFailedExternalTaskIncidentHandler();
        final List<IncidentHandler> incidentListeners = asList(failedJobHandler, failedExternalTaskHandler);
        processEngineConfig.setCustomIncidentHandlers(incidentListeners);

        incidentListeners.stream().map(incidentHandler -> (BufferingIncidentHandler) incidentHandler)
                .forEach(BufferingIncidentHandler::startTimer);

        LOGGER.debug("EmailIncidentNotificationHandlerPlugin initialized.");
    }

    public FailedExternalTaskIncidentHandler createFailedExternalTaskIncidentHandler() {
        return new FailedExternalTaskIncidentHandler(property);
    }

    public FailedJobIncidentHandler createFailedJobIncidentHandler() {
        return new FailedJobIncidentHandler(property);
    }
}
