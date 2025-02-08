package com.eorion.bo.enhancement.emailincidentnotification.handler;

import com.eorion.bo.enhancement.emailincidentnotification.config.EmailIncidentNotificationConfigurationProperty;
import com.eorion.bo.enhancement.emailincidentnotification.domain.IncidentInformation;
import com.eorion.bo.enhancement.emailincidentnotification.domain.RecipientInfo;
import com.eorion.bo.enhancement.emailincidentnotification.util.DefaultTimeProvider;
import com.eorion.bo.enhancement.emailincidentnotification.util.ITimeProvider;
import com.sun.mail.smtp.SMTPTransport;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.impl.incident.DefaultIncidentHandler;
import org.camunda.bpm.engine.impl.incident.IncidentContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.IncidentEntity;
import org.camunda.bpm.engine.runtime.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
public class BufferingIncidentHandler extends DefaultIncidentHandler {
    public static final String TIME_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss z";
    public static final String INCIDENT_NOTIFICATION_RECEIVER = "incidentNotificationReceiver";
    public static final String INCIDENT_NOTIFICATION_CC = "incidentNotificationCc";
    private final static Logger LOGGER = LoggerFactory.getLogger(BufferingIncidentHandler.class);
    private final EmailIncidentNotificationConfigurationProperty config;
    private final ITimeProvider timeProvider;
    private List<IncidentInformation> incidentInfos = new CopyOnWriteArrayList<>();

    public BufferingIncidentHandler(final ITimeProvider timeProvider, final String type,
                                    final EmailIncidentNotificationConfigurationProperty config) {
        super(type);
        this.timeProvider = timeProvider;
        this.config = config;
    }

    public BufferingIncidentHandler(final String type, final EmailIncidentNotificationConfigurationProperty config) {
        this(new DefaultTimeProvider(), type, config);
    }

    public IncidentEntity superHandleIncident(final IncidentContext context, String message) {
        return (IncidentEntity) super.handleIncident(context, message);
    }

    @Override
    public synchronized Incident handleIncident(final IncidentContext ctx, final String message) {
        LOGGER.debug("An incident occurs");

        final IncidentEntity incEnt = superHandleIncident(ctx, message);
        this.incidentInfos.add(createIncidentInfo(incEnt));
        return incEnt;
    }

    public IncidentInformation createIncidentInfo(final IncidentEntity incEnt) {
        ExecutionEntity execution = incEnt.getExecution();
        final String emailReceiver = execution == null ? null : (String) execution.getVariable(INCIDENT_NOTIFICATION_RECEIVER);
        final String emailCc = execution == null ? null : (String) execution.getVariable(INCIDENT_NOTIFICATION_CC);
        return new IncidentInformation(incEnt.getIncidentType(), incEnt.getActivityId(), clean(incEnt.getIncidentMessage()),
                incEnt.getProcessInstanceId(), new SimpleDateFormat(TIME_FORMAT_STRING).format(timeProvider.now()),
                incEnt.getProcessDefinitionId(), incEnt.getProcessDefinition() == null ? null : incEnt.getProcessDefinition().getName(),
                new RecipientInfo(emailReceiver, emailCc));
    }

    private String clean(String input) {
        if (input == null) {
            return "";
        }
        String result = input.replaceAll("\\{", "");
        result = result.replaceAll("\\}", "");
        result = result.replaceAll("\\$", "");
        return result;
    }

    public void startTimer() {
        LOGGER.debug("Starting timer for Incident");
        createTimer().scheduleAtFixedRate(createTimerTask(), config.intervalMs(), config.intervalMs());
    }

    public IncidentTimerTask createTimerTask() {
        return new IncidentTimerTask(this);
    }

    public Timer createTimer() {
        return new Timer();
    }

    public synchronized void sendEmailIfNecessary() {
        if (!(incidentInfos.isEmpty())) {
            if (sendEmailsToRecipients()) {
                incidentInfos.clear();
            }
        }
    }

    public synchronized boolean sendEmailsToRecipients() {
        boolean success = true;
        final Map<RecipientInfo, List<IncidentInformation>> incidentInfosByRecipientInfo = new HashMap<>();

        for (final IncidentInformation curIncident : incidentInfos) {
            final RecipientInfo recipientInfo = curIncident.recipientInfo();
            List<IncidentInformation> targetList = incidentInfosByRecipientInfo.computeIfAbsent(recipientInfo, k -> new ArrayList<>());
            targetList.add(curIncident);
        }
        for (final Map.Entry<RecipientInfo, List<IncidentInformation>> curEntry : incidentInfosByRecipientInfo.entrySet()) {
            final RecipientInfo recipientInfo = curEntry.getKey();
            final List<IncidentInformation> incidents = curEntry.getValue();

            final String messageText = composeMessage(incidents);
            final boolean emailSent = sendEmail(recipientInfo, messageText);
            if (!emailSent) {
                success = false;
            }
        }
        return success;
    }

    public String composeMessage(final List<IncidentInformation> incidents) {
        final StringBuilder incidentDescriptions = new StringBuilder();
        for (final IncidentInformation curIncident : incidents) {
            incidentDescriptions.append(composeIncidentDescription(curIncident));
            incidentDescriptions.append("\n");
        }
        return config.mailBodyTemplate().replaceAll("@INCIDENTS", incidentDescriptions.toString());
    }

    private String composeIncidentDescription(final IncidentInformation incident) {
        String incidentText = config.incidentTemplate();
        incidentText = incidentText.replaceAll("@ACTIVITY", incident.activityId());
        incidentText = incidentText.replaceAll("@PROCESS_INSTANCE_ID", incident.processInstanceId());
        incidentText = incidentText.replaceAll("@MESSAGE", incident.message());
        incidentText = incidentText.replaceAll("@INCIDENT_TYPE", incident.type());
        incidentText = incidentText.replaceAll("@URL", String.format("%s/%s",
                config.url(), incident.processInstanceId()));
        incidentText = incidentText.replaceAll("@TIME", incident.time());
        return incidentText;
    }

    public boolean sendEmail(final RecipientInfo recipientInfo, final String messageText) {
        final Properties mailConfig = new Properties();
        SMTPTransport transport = null;
        String response;

        // For general Jakarta Mail properties : https://jakarta.ee/specifications/mail/2.0/apidocs/jakarta.mail/jakarta/mail/package-summary
        // For SMTP protocol by sun mail : https://javadoc.io/doc/com.sun.mail/jakarta.mail/2.0.1/jakarta.mail/com/sun/mail/smtp/package-summary.html
        mailConfig.put("mail.debug", Boolean.toString(config.debug()));
        switch (config.protocol()) {
            case "smtp":
                mailConfig.put("mail.smtp.host", config.host());
                if (org.springframework.util.StringUtils.hasLength(config.trust()))
                    mailConfig.put("mail.smtp.ssl.trust", config.trust());
                mailConfig.put("mail.smtp.auth", Boolean.toString(config.auth()));
                mailConfig.put("mail.smtp.port", Integer.toString(config.port()));
                mailConfig.put("mail.smtp.ssl.enable", Boolean.toString(config.ssl()));
                mailConfig.put("mail.smtp.starttls.enable", Boolean.toString(config.tls()));
                mailConfig.put("mail.smtp.connectiontimeout", Integer.toString(config.connectionTimeout()));
                mailConfig.put("mail.smtp.timeout", Integer.toString(config.timeout()));
                mailConfig.put("mail.smtp.writetimeout", Integer.toString(config.writeTimeout()));
                break;
            case "smtps":
                mailConfig.put("mail.smtps.host", config.host());
                mailConfig.put("mail.smtps.ssl.trust", config.trust());
                mailConfig.put("mail.smtps.auth", Boolean.toString(config.auth()));
                mailConfig.put("mail.smtps.port", Integer.toString(config.port()));
                mailConfig.put("mail.smtps.connectiontimeout", Integer.toString(config.connectionTimeout()));
                mailConfig.put("mail.smtps.timeout", Integer.toString(config.timeout()));
                mailConfig.put("mail.smtps.writetimeout", Integer.toString(config.writeTimeout()));
                break;
            default:
                throw new IllegalArgumentException("Unsupported protocol: " + config.protocol());
        }


        Session session = Session.getInstance(mailConfig, null);
        Message msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(config.mailSender()));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(determineReceiver(recipientInfo), false));
            if (StringUtils.isNotBlank(recipientInfo.cc())) {
                msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(recipientInfo.cc(), false));

            }

            msg.setSubject(config.subject());
            msg.setText(messageText);

            transport = (SMTPTransport) getTransport(session);

            transport.connect(config.host(), config.username(), config.password());
            transport.sendMessage(msg, msg.getAllRecipients());

            response = transport.getLastServerResponse();

            LOGGER.info("Received response from server: '{}'", response);
            return true;
        } catch (final MessagingException exception) {
            LOGGER.error("An error occurred, while sending incident report: '{}'", messageText, exception);
            return false;
        } finally {
            if (transport != null) {
                try {
                    transport.close();
                } catch (final MessagingException exception) {
                    LOGGER.error("An error occurred, while sending incident report: '{}'", messageText, exception);
                }
            }
        }
    }

    public Transport getTransport(Session session) throws NoSuchProviderException {
        return switch (config.protocol()) {
            case "smtp" -> session.getTransport("smtp");
            case "smtps" -> session.getTransport("smtps");
            default -> throw new IllegalArgumentException("Unsupported protocol: " + config.protocol());
        };
    }

    public String determineReceiver(RecipientInfo recipientInfo) {
        String receiver = recipientInfo.receiver();
        if (StringUtils.isBlank(receiver)) {
            receiver = config.fallbackMailReceiver();
            LOGGER.info("Using fallback receiver information {} because the process-related receiver is blank", receiver);
        }
        return receiver;
    }
}
