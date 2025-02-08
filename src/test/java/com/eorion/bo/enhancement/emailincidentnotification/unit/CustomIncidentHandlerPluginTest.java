package com.eorion.bo.enhancement.emailincidentnotification.unit;

import com.eorion.bo.enhancement.emailincidentnotification.config.EmailIncidentNotificationConfigurationProperty;
import com.eorion.bo.enhancement.emailincidentnotification.handler.FailedExternalTaskIncidentHandler;
import com.eorion.bo.enhancement.emailincidentnotification.handler.FailedJobIncidentHandler;
import com.eorion.bo.enhancement.emailincidentnotification.plugin.EmailIncidentNotificationHandlerPlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.incident.IncidentHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CustomIncidentHandlerPluginTest {
    public static final Long INTERVAL_MS = 1000L;
    public static final Integer PORT = 587;
    public static final String URL = "http://localhost:8080/camunda";
    public static final String FALLBACK_MAIL_RECEIVER = "fallbackreceiver@provider.com";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String SUBJECT = "subject";
    public static final String HOST = "smtp.gmail.com";
    public static final String MAIL_SENDER = "mailsender@provider.com";
    public static final String MAIL_BODY_TEMPLATE = "mailBodyTemplate";
    public static final String INCIDENT_TEMPLATE = "incidentTemplate";

    private final EmailIncidentNotificationConfigurationProperty config = new EmailIncidentNotificationConfigurationProperty(
            true, INTERVAL_MS, "smtp", URL, FALLBACK_MAIL_RECEIVER, USERNAME, PASSWORD, SUBJECT, HOST, PORT, false,
            false, "", false, true, MAIL_SENDER, MAIL_BODY_TEMPLATE, INCIDENT_TEMPLATE, 0, 0, 0
    );

    @Test
    public void givenConfig_whenPreInit_thenCallTheRightMethods() {
        // Given
        final EmailIncidentNotificationHandlerPlugin customIncidentHandlerPlugin = spy(new EmailIncidentNotificationHandlerPlugin(config));
        final ProcessEngineConfigurationImpl processEngineConfig = mock(ProcessEngineConfigurationImpl.class);
        final FailedJobIncidentHandler failedJobHandler = mock(FailedJobIncidentHandler.class);
        final FailedExternalTaskIncidentHandler failedExternalTaskHandler = mock(FailedExternalTaskIncidentHandler.class);

        doAnswer(inv -> {
            assertConfigCorrect(config);
            return failedJobHandler;
        }).when(customIncidentHandlerPlugin).createFailedJobIncidentHandler();

        doAnswer(inv -> {
            assertConfigCorrect(config);
            return failedExternalTaskHandler;
        }).when(customIncidentHandlerPlugin).createFailedExternalTaskIncidentHandler();

        doAnswer(inv -> {
            final List<IncidentHandler> incidentListeners = inv.getArgument(0);
            assertSame(failedJobHandler, incidentListeners.get(0));
            assertSame(failedExternalTaskHandler, incidentListeners.get(1));
            return null;
        }).when(processEngineConfig).setCustomIncidentHandlers(any());

        // When
        customIncidentHandlerPlugin.preInit(processEngineConfig);

        // Then
        assertNotNull(failedJobHandler);
        assertNotNull(failedExternalTaskHandler);

        verify(customIncidentHandlerPlugin).createFailedExternalTaskIncidentHandler();
        verify(customIncidentHandlerPlugin).createFailedExternalTaskIncidentHandler();
        verify(processEngineConfig).setCustomIncidentHandlers(any());
        verify(failedJobHandler).startTimer();
        verify(failedExternalTaskHandler).startTimer();
    }

    private void assertConfigCorrect(final EmailIncidentNotificationConfigurationProperty actConfig) {
        assertEquals(INTERVAL_MS, (long) actConfig.intervalMs());
        assertEquals(PORT, (int) actConfig.port());
        assertEquals(URL, actConfig.url());
        assertEquals(FALLBACK_MAIL_RECEIVER, actConfig.fallbackMailReceiver());
        assertEquals(USERNAME, actConfig.username());
        assertEquals(PASSWORD, actConfig.password());
        assertEquals(SUBJECT, actConfig.subject());
        assertEquals(HOST, actConfig.host());
        assertEquals(MAIL_BODY_TEMPLATE, actConfig.mailBodyTemplate());
        assertEquals(INCIDENT_TEMPLATE, actConfig.incidentTemplate());
    }
}