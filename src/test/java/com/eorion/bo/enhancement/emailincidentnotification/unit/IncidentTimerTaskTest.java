package com.eorion.bo.enhancement.emailincidentnotification.unit;

import com.eorion.bo.enhancement.emailincidentnotification.handler.BufferingIncidentHandler;
import com.eorion.bo.enhancement.emailincidentnotification.handler.IncidentTimerTask;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class IncidentTimerTaskTest {
    @Test
    void givenIncidentHandler_whenRun_callTheRightMethod() {
        // Given
        final BufferingIncidentHandler incidentHandler = mock(BufferingIncidentHandler.class);
        final IncidentTimerTask incidentTimerTask = new IncidentTimerTask(incidentHandler);

        // When
        incidentTimerTask.run();

        // Then
        verify(incidentHandler).sendEmailIfNecessary();
    }
}