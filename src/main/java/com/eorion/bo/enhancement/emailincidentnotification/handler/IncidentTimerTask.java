package com.eorion.bo.enhancement.emailincidentnotification.handler;

import java.util.TimerTask;

public class IncidentTimerTask extends TimerTask {
    private final BufferingIncidentHandler incidentHandler;

    public IncidentTimerTask(final BufferingIncidentHandler incidentHandler) {
        this.incidentHandler = incidentHandler;
    }

    @Override
    public void run() {
        incidentHandler.sendEmailIfNecessary();
    }
}
