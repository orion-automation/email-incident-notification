package com.eorion.bo.enhancement.emailincidentnotification.unit;

import com.eorion.bo.enhancement.emailincidentnotification.config.EmailIncidentNotificationConfigurationProperty;
import com.eorion.bo.enhancement.emailincidentnotification.domain.IncidentInformation;
import com.eorion.bo.enhancement.emailincidentnotification.handler.BufferingIncidentHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

public class BufferingIncidentHandlerSendEmailIfNecessaryTest {
    private final EmailIncidentNotificationConfigurationProperty config = new EmailIncidentNotificationConfigurationProperty(
            true, 3000L, "smtp", "", "", "", "", "", "", 0,
            false, false, "", false, true, "", "", "",
            0, 0, 0);

    @Test
    public void givenNoIncidents_whenSendEmailIfNecessary_thenDontDoAnything() {
        // Given
        final BufferingIncidentHandler incidentHandler = spy(new BufferingIncidentHandler("", config));

        // When
        incidentHandler.sendEmailIfNecessary();

        // Then
        verify(incidentHandler, never()).sendEmailsToRecipients();
    }

    @Test
    public void givenIncidents_whenSendEmailIfNecessary_thenSendEmail() {
        // Given
        final BufferingIncidentHandler incidentHandler = spy(new BufferingIncidentHandler("", config));
        final List<IncidentInformation> incidentInfos = mock(List.class);
        when(incidentInfos.isEmpty()).thenReturn(false);
        incidentHandler.setIncidentInfos(incidentInfos);

        doReturn(true).when(incidentHandler).sendEmailsToRecipients();

        // When
        incidentHandler.sendEmailIfNecessary();

        // Then
        verify(incidentHandler).sendEmailsToRecipients();
        verify(incidentInfos).clear();
    }

    @Test
    public void givenIncidentsAndMailFailure_whenSendEmailIfNecessary_thenDontClearList() {
        // Given
        final BufferingIncidentHandler incidentHandler = spy(new BufferingIncidentHandler("", config));
        final List<IncidentInformation> incidentInfos = mock(List.class);
        when(incidentInfos.isEmpty()).thenReturn(false);
        incidentHandler.setIncidentInfos(incidentInfos);

        doReturn(false).when(incidentHandler).sendEmailsToRecipients();

        // When
        incidentHandler.sendEmailIfNecessary();

        // Then
        verify(incidentHandler).sendEmailsToRecipients();
        verify(incidentInfos, never()).clear();

    }
}
