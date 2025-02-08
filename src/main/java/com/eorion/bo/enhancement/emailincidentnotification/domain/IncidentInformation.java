package com.eorion.bo.enhancement.emailincidentnotification.domain;

public record IncidentInformation(
        String type,
        String activityId,
        String message,
        String processInstanceId,
        String time,
        String processDefinitionId,
        String processDefinitionName,
        RecipientInfo recipientInfo
) {
}
