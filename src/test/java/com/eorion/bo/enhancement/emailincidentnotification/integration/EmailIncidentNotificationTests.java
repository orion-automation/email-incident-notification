package com.eorion.bo.enhancement.emailincidentnotification.integration;

import com.eorion.bo.enhancement.emailincidentnotification.config.EmailIncidentNotificationConfigurationProperty;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.MessagingException;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
public class EmailIncidentNotificationTests {
    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private EmailIncidentNotificationConfigurationProperty property;

    @AfterAll
    static void afterAll() {
        greenMail.stop();
    }

    @Test
    void shouldSendMailToSmtpServer() throws InterruptedException, MessagingException, IOException {
        var pid = runtimeService.startProcessInstanceByKey("IncidentProcess");
        runtimeService.createIncident("failedJob", pid.getId(), "SomeActivityId", "A dummy incident message");

        Thread.sleep(1000);

        var messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);
        var message = messages[0];

        var froms = message.getFrom();
        assertEquals(1, froms.length);
        assertEquals(property.mailSender(), froms[0].toString());

        assertEquals(property.subject(), message.getSubject());

        var recipients = message.getAllRecipients();
        assertEquals(1, recipients.length);
        assertEquals(property.fallbackMailReceiver(), recipients[0].toString());

        var content = message.getContent().toString();
        assertTrue(content.contains("IncidentActivity"));
        assertTrue(content.contains(pid.getProcessInstanceId()));
        assertTrue(content.contains("A dummy incident message"));
        assertTrue(content.contains("failedJob"));
        assertTrue(content.contains(property.url() + "/" + pid.getId()));
    }
}
