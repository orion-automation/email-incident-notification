package com.eorion.bo.enhancement.emailincidentnotification.unit;

import com.eorion.bo.enhancement.emailincidentnotification.config.EmailIncidentNotificationConfigurationProperty;
import com.eorion.bo.enhancement.emailincidentnotification.domain.IncidentInformation;
import com.eorion.bo.enhancement.emailincidentnotification.domain.RecipientInfo;
import com.eorion.bo.enhancement.emailincidentnotification.handler.BufferingIncidentHandler;
import com.eorion.bo.enhancement.emailincidentnotification.handler.IncidentTimerTask;
import com.eorion.bo.enhancement.emailincidentnotification.util.ITimeProvider;
import com.sun.mail.smtp.SMTPTransport;
import org.apache.commons.lang3.time.DateUtils;
import org.camunda.bpm.engine.impl.incident.IncidentContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.IncidentEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.runtime.Incident;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BufferingIncidentHandlerTest {
    public static final String MESSAGE = "Test message";

    private final EmailIncidentNotificationConfigurationProperty config = new EmailIncidentNotificationConfigurationProperty(
            true, 123L, "smtp", "http://localhost:8080/camunda", "backupReceiver@provider.com", "username",
            "password", "Incident on Camunda DEV", "host", 587, false, false,
            "", false, true, "Camunda DEV <no-reply@a1.at>", "PREFIX @INCIDENTS SUFFIX",
            """
                    Activity ID: @ACTIVITY
                    Process Instance ID: @PROCESS_INSTANCE_ID
                    Message: @MESSAGE
                    Incident Type: @INCIDENT_TYPE
                    URL: @URL
                    Time: @TIME""", 0, 0, 0);

    @Test
    public void givenIncident_whenHandleIncident_thenAddIncidentInfoToList() {
        // Given
        final BufferingIncidentHandler ruleBasedIncidentHandler = Mockito.spy(new BufferingIncidentHandler("", config));

        final IncidentContext ctx = mock(IncidentContext.class);
        final IncidentEntity incEnt = mock(IncidentEntity.class);
        doReturn(incEnt).when(ruleBasedIncidentHandler).superHandleIncident(ctx, MESSAGE);

        final IncidentInformation incidentInfo = new IncidentInformation("", "", "", "", "", "", "", null);
        doReturn(incidentInfo).when(ruleBasedIncidentHandler).createIncidentInfo(incEnt);

        // When
        final Incident actRes = ruleBasedIncidentHandler.handleIncident(ctx, MESSAGE);

        // Then
        verify(ruleBasedIncidentHandler).superHandleIncident(ctx, MESSAGE);
        assertSame(incEnt, actRes);
        assertTrue(ruleBasedIncidentHandler.getIncidentInfos().contains(incidentInfo));
    }

    @Test
    public void givenInputData_whenCreateIncidentInfo_thenReturnCorrectValue() throws ParseException {
        // Given
        final IncidentEntity incEnt = mock(IncidentEntity.class);
        final ITimeProvider timeProvider = mock(ITimeProvider.class);
        final BufferingIncidentHandler ruleBasedIncidentHandler = new BufferingIncidentHandler(timeProvider, "",
                config);
        final String emailReceiver = "emailReceiver@provider.com";
        final String ccReceiver = "ccReceiver@provider.com";

        final ExecutionEntity execution = mock(ExecutionEntity.class);
        when(incEnt.getExecution()).thenReturn(execution);
        when(execution.getVariable(BufferingIncidentHandler.INCIDENT_NOTIFICATION_RECEIVER)).thenReturn(emailReceiver);
        when(execution.getVariable(BufferingIncidentHandler.INCIDENT_NOTIFICATION_CC)).thenReturn(ccReceiver);

        when(incEnt.getIncidentType()).thenReturn("failedJob");
        when(incEnt.getActivityId()).thenReturn("activityId");
        when(incEnt.getIncidentMessage()).thenReturn("Message with {curly braces} and a $ dollar sign");
        when(incEnt.getProcessInstanceId()).thenReturn("processInstanceId");

        final Date now = DateUtils.parseDate("2020-11-23 10:22", "yyyy-MM-dd HH:mm");
        when(timeProvider.now()).thenReturn(now);

        when(incEnt.getProcessDefinitionId()).thenReturn("processDefinitionId");

        final ProcessDefinitionEntity processDefinition = mock(ProcessDefinitionEntity.class);
        when(incEnt.getProcessDefinition()).thenReturn(processDefinition);
        when(processDefinition.getName()).thenReturn("Process Definition Name");

        // When
        final IncidentInformation actRes = ruleBasedIncidentHandler.createIncidentInfo(incEnt);

        // Then
        verify(timeProvider).now();
        assertNotNull(actRes);
        assertEquals("failedJob", actRes.type());
        assertEquals("activityId", actRes.activityId());
        assertEquals("Message with curly braces and a  dollar sign", actRes.message());
        assertEquals("processInstanceId", actRes.processInstanceId());
        assertEquals("2020-11-23 10:22:00 CST", actRes.time());
        assertEquals("processDefinitionId", actRes.processDefinitionId());
        assertEquals("Process Definition Name", actRes.processDefinitionName());
        assertNotNull(actRes.recipientInfo());
        assertEquals("emailReceiver@provider.com", actRes.recipientInfo().receiver());
        assertEquals("ccReceiver@provider.com", actRes.recipientInfo().cc());
    }

    @Test
    public void givenConfig_whenStartTimer_thenStartTimerWithCorrectInterval() {
        // Given
        final BufferingIncidentHandler ruleBasedIncidentHandler = Mockito.spy(new BufferingIncidentHandler("", config));
        final IncidentTimerTask timerTask = mock(IncidentTimerTask.class);
        doReturn(timerTask).when(ruleBasedIncidentHandler).createTimerTask();

        final Timer timer = mock(Timer.class);
        doReturn(timer).when(ruleBasedIncidentHandler).createTimer();

        // When
        ruleBasedIncidentHandler.startTimer();

        // Then
        verify(ruleBasedIncidentHandler).createTimer();
        verify(ruleBasedIncidentHandler).createTimerTask();
        verify(timer).scheduleAtFixedRate(timerTask, 123L, 123L);
    }

    @Test
    public void givenMultipleRecipientInfos_whenSendMailToRecipients_thenSendEmailsToCorrectRecipients() {
        // Given
        final RecipientInfo recipientInfo1 = new RecipientInfo("receiver1", "cc1");
        final RecipientInfo recipientInfo2 = new RecipientInfo("receiver2", "cc2");
        final IncidentInformation incidentInfo1 = new IncidentInformation("", "", "",
                "", "", "", "", recipientInfo1);
        final IncidentInformation incidentInfo2 = new IncidentInformation("", "", "",
                "", "", "", "", recipientInfo1);
        final IncidentInformation incidentInfo3 = new IncidentInformation("", "", "",
                "", "", "", "", recipientInfo2);
        final IncidentInformation incidentInfo4 = new IncidentInformation("", "", "",
                "", "", "", "", recipientInfo2);
        final IncidentInformation incidentInfo5 = new IncidentInformation("", "", "",
                "", "", "", "", recipientInfo2);

        final BufferingIncidentHandler ruleBasedIncidentHandler = Mockito.spy(new BufferingIncidentHandler("", config));
        ruleBasedIncidentHandler.setIncidentInfos(Arrays.asList(incidentInfo1, incidentInfo2, incidentInfo3, incidentInfo4,
                incidentInfo5));

        doAnswer(invocationOnMock -> {
            final List<IncidentInformation> incidents = invocationOnMock.getArgument(0);

            if (incidents.contains(incidentInfo1) && incidents.contains(incidentInfo2)) {
                return "message for recipientInfo1";
            } else if (!incidents.contains(incidentInfo1) && !incidents.contains(incidentInfo2)
                    && incidents.contains(incidentInfo3) && incidents.contains(incidentInfo4)
                    && incidents.contains(incidentInfo5)) {
                return "message for recipientInfo2";
            } else {
                fail("Invalid call");
            }
            return null;
        }).when(ruleBasedIncidentHandler).composeMessage(anyList());

        doReturn(true).when(ruleBasedIncidentHandler).sendEmail(any(), anyString());

        // When
        final boolean actRes = ruleBasedIncidentHandler.sendEmailsToRecipients();

        // Then
        assertTrue(actRes);
        verify(ruleBasedIncidentHandler, times(2)).composeMessage(anyList());
        verify(ruleBasedIncidentHandler).sendEmail(recipientInfo1, "message for recipientInfo1");
        verify(ruleBasedIncidentHandler).sendEmail(recipientInfo2, "message for recipientInfo2");
    }

    @Test
    public void givenEmailSendingFailure_whenSendMailToRecipients_thenReturnFalse() {
        // Given
        final RecipientInfo recipientInfo1 = new RecipientInfo("receiver1", "cc1");
        final RecipientInfo recipientInfo2 = new RecipientInfo("receiver2", "cc2");
        final IncidentInformation incidentInfo1 = new IncidentInformation("", "", "",
                "", "", "", "", recipientInfo1);
        final IncidentInformation incidentInfo2 = new IncidentInformation("", "", "",
                "", "", "", "", recipientInfo1);
        final IncidentInformation incidentInfo3 = new IncidentInformation("", "", "",
                "", "", "", "", recipientInfo2);
        final IncidentInformation incidentInfo4 = new IncidentInformation("", "", "",
                "", "", "", "", recipientInfo2);
        final IncidentInformation incidentInfo5 = new IncidentInformation("", "", "",
                "", "", "", "", recipientInfo2);

        final BufferingIncidentHandler ruleBasedIncidentHandler = Mockito.spy(new BufferingIncidentHandler("", config));
        ruleBasedIncidentHandler.setIncidentInfos(Arrays.asList(incidentInfo1, incidentInfo2, incidentInfo3, incidentInfo4,
                incidentInfo5));

        doAnswer(invocationOnMock -> {
            final List<IncidentInformation> incidents = invocationOnMock.getArgument(0);

            if (incidents.contains(incidentInfo1) && incidents.contains(incidentInfo2)) {
                return "message for recipientInfo1";
            } else if (!incidents.contains(incidentInfo1) && !incidents.contains(incidentInfo2)
                    && incidents.contains(incidentInfo3) && incidents.contains(incidentInfo4)
                    && incidents.contains(incidentInfo5)) {
                return "message for recipientInfo2";
            } else {
                fail("Invalid call");
            }
            return null;
        }).when(ruleBasedIncidentHandler).composeMessage(anyList());

        doAnswer(new Answer<>() {
            private boolean firstTime = true;

            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                if (firstTime) {
                    firstTime = false;
                    return true;
                }
                return false;
            }
        }).when(ruleBasedIncidentHandler).sendEmail(any(), anyString());

        // When
        final boolean actRes = ruleBasedIncidentHandler.sendEmailsToRecipients();

        // Then
        assertFalse(actRes);
        verify(ruleBasedIncidentHandler, times(2)).composeMessage(anyList());
        verify(ruleBasedIncidentHandler).sendEmail(recipientInfo1, "message for recipientInfo1");
        verify(ruleBasedIncidentHandler).sendEmail(recipientInfo2, "message for recipientInfo2");
    }

    @Test
    public void givenIncidents_whenComposeMessage_thenReturnCorrectText() {
        // Given
        final IncidentInformation incident1 = new IncidentInformation("failedJob", "activityId1", "Message 1",
                "processInstanceId1", "2020-11-23 11:28:00 CET", "processDefinitionId1",
                "Process definition name 1", null);
        final IncidentInformation incident2 = new IncidentInformation("failedJob", "activityId2", "Message 2",
                "processInstanceId2", "2020-11-23 11:29:01 CET", "processDefinitionId2",
                "Process definition name 2", null);
        final BufferingIncidentHandler ruleBasedIncidentHandler = new BufferingIncidentHandler("", config);

        // When
        final String actRes = ruleBasedIncidentHandler.composeMessage(Arrays.asList(incident1, incident2));

        // Then
        assertEquals("""
                PREFIX Activity ID: activityId1
                Process Instance ID: processInstanceId1
                Message: Message 1
                Incident Type: failedJob
                URL: http://localhost:8080/camunda/processInstanceId1
                Time: 2020-11-23 11:28:00 CET
                Activity ID: activityId2
                Process Instance ID: processInstanceId2
                Message: Message 2
                Incident Type: failedJob
                URL: http://localhost:8080/camunda/processInstanceId2
                Time: 2020-11-23 11:29:01 CET
                 SUFFIX""", actRes);
    }

    @Test
    public void givenNoReceiverInProcess_whenDetermineReceiver_thenReturnFallbackReceiver() {
        // Given
        final BufferingIncidentHandler ruleBasedIncidentHandler = new BufferingIncidentHandler("", config);
        final RecipientInfo recipientInfo = new RecipientInfo("", "ccReceiver@provider.com");

        // When
        final String actRes = ruleBasedIncidentHandler.determineReceiver(recipientInfo);

        // Then
        assertEquals("backupReceiver@provider.com", actRes);
    }

    @Test
    public void givenReceiverInProcess_whenDetermineReceiver_thenReturnReceiverInProcess() {
        // Given
        final BufferingIncidentHandler ruleBasedIncidentHandler = new BufferingIncidentHandler("", config);
        final RecipientInfo recipientInfo = new RecipientInfo("receiverSpecifiedInProcessVariable@provider.com",
                "ccReceiver@provider.com");

        // When
        final String actRes = ruleBasedIncidentHandler.determineReceiver(recipientInfo);

        // Then
        assertEquals("receiverSpecifiedInProcessVariable@provider.com", actRes);
    }

    @Test
    public void givenConfig_whenSendMail_thenSendMessageWithCorrectParameters() throws MessagingException {
        // Given
        final BufferingIncidentHandler ruleBasedIncidentHandler = Mockito.spy(new BufferingIncidentHandler("", config));

        final RecipientInfo recipientInfo = new RecipientInfo("recipient@provider.com",
                "ccRecipient1@provider.com,ccRecipient2@provider.com");

        final SMTPTransport transport = mock(SMTPTransport.class);
        doReturn(transport).when(ruleBasedIncidentHandler).getTransport(any());

        doAnswer(invocationOnMock -> {
            final Message msg = invocationOnMock.getArgument(0);

            assertEquals(1, msg.getFrom().length);
            assertEquals("Camunda DEV <no-reply@a1.at>", msg.getFrom()[0].toString());
            final Address[] toRecipients = msg.getRecipients(Message.RecipientType.TO);
            assertEquals(1, toRecipients.length);
            assertEquals("recipient@provider.com", toRecipients[0].toString());

            final Address[] ccRecipients = msg.getRecipients(Message.RecipientType.CC);
            assertEquals(2, ccRecipients.length);
            assertEquals("ccRecipient1@provider.com", ccRecipients[0].toString());
            assertEquals("ccRecipient2@provider.com", ccRecipients[1].toString());

            assertEquals("Incident on Camunda DEV", msg.getSubject());
            assertEquals("messageText", msg.getContent());

            return null;
        }).when(transport).sendMessage(any(), any());

        // When
        final boolean actRes = ruleBasedIncidentHandler.sendEmail(recipientInfo, "messageText");

        // Then
        assertTrue(actRes);
        verify(ruleBasedIncidentHandler).getTransport(any());
        verify(transport).connect("host", "username", "password");
        verify(transport).sendMessage(any(), any());
        verify(transport).close();
    }

    @Test
    public void givenEmailSendingFailure_whenSendEmail_thenReturnFalse() throws MessagingException {
        // Given
        final BufferingIncidentHandler ruleBasedIncidentHandler = Mockito.spy(new BufferingIncidentHandler("", config));

        final RecipientInfo recipientInfo = new RecipientInfo("recipient@provider.com",
                "ccRecipient1@provider.com,ccRecipient2@provider.com");

        final SMTPTransport transport = mock(SMTPTransport.class);
        doReturn(transport).when(ruleBasedIncidentHandler).getTransport(any());

        doThrow(new MessagingException()).when(transport).sendMessage(any(), any());

        // When
        final boolean actRes = ruleBasedIncidentHandler.sendEmail(recipientInfo, "messageText");

        // Then
        assertFalse(actRes);
        verify(ruleBasedIncidentHandler).getTransport(any());
        verify(transport).connect("host", "username", "password");
        verify(transport).sendMessage(any(), any());
        verify(transport).close();

    }
}
