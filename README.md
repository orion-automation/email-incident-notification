# E-mail Incident Notification Plugin

## How can it benefit you?

The purpose of this plugin is to notify people about incidents in Camunda processes via e-mail.

## What is and is not an incident

According to the [docs](https://docs.camunda.org/manual/latest/user-guide/process-engine/incidents/), only failed jobs can occur incidents. 
The following cases for example are not incidents so that no e-mail notification will be sent.

- Errors in the thread which is completing a user task
- Errors in the thread which is starting a process instance
- etc.

## How does it work?

Whenever an incident occurs, its data are stored in a list (see method `BufferingIncidentHandler.handleIncident`).

Regularly (e.g. every five minutes), a piece of code checks whether there are incidents in that list.

If there are, e-mails are being sent out with the information about the incidents.

## Whom are the e-mails being sent to?

Two ways are available to configure the recipients by priorities as follows:

1. specified via process variables

| variable name                | description              |
|------------------------------|--------------------------|
| incidentNotificationReceiver | the mail address of `TO` |
| incidentNotificationCc       | the mail address of `CC` |


2. specified via Spring Boot configuration properties

| property name                                    | description                                                                                           |
|--------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| eorion.bo.enhancement.ein.fallback-mail-receiver | the default mail address of `TO` if `incidentNotificationReceiver` in the process instance is missing | |

## Configuration Properties

The prefix of all the following configuration properties is `eorion.bo.enhancement.ein`.

| name                   | type    | required                    | default value | description                                                                                      |
|------------------------|---------|-----------------------------|---------------|--------------------------------------------------------------------------------------------------|
| enabled                | boolean |                             | false         | whether enable the plugin                                                                        |
| interval-ms            | long    |                             | 300000        | [(see below)](#interval-ms)                                                                      |
| protocol               | string  |                             | smtp          | [(see below)](#protocol)                                                                         |
| host                   | string  | &#9989;                     |               | address (host) of the smtp(s) server                                                             |
| port                   | integer | &#9989;                     |               | port number of the smtp(s) server                                                                |
| ssl                    | boolean |                             | false         | the value of `mail.smtp.ssl.enable`                                                              |
| trust                  | string  |                             |               | the value of `mail.smtp(s).ssl.trust`                                                            |
| tls                    | boolean |                             | false         | the value of `mail.smtp.starttls.enable`                                                         |
| debug                  | boolean |                             | false         | the value of `mail.debug`                                                                        |
| auth                   | boolean |                             | true          | the value of `mail.smtp(s).auth`                                                                 |
| username               | string  | &#9989; if `auth` is `true` |               | the username to login to the smtp(s) server                                                      |
| password               | string  | &#9989; if `auth` is `true` |               | the password to login to the smtpo(s) server                                                     |
| url                    | string  |                             |               | URL of the Camunda cockpit. It is used to create a link to the incident page in Camunda cockpit. |
| fallback-mail-receiver | string  |                             |               | [(see above)](#whom-are-the-e-mails-being-sent-to)                                               |
| subject                | string  | &#9989;                     |               | Subject of the incident e-mails                                                                  |
| mail-sender            | string  | &#9989;                     |               | the `From` field of incident e-mails                                                             |
| mail-body-template     | string  |                             |               | [(see below)](#mail-body-template)                                                               |
| incident-template      | string  |                             |               | [(see below)](#incident-template)                                                                |
| connection-timeout     | integer |                             | 30000         | the value of `mail.smtp(s).connectiontimeout`                                                    |
| timeout                | integer |                             | 30000         | the value of `mail.smtp(s).timeout`                                                              |
| write-timeout          | integer |                             | 30000         | the value of `mail.smtp(s).writetimeout`                                                         |

### interval-ms

Interval in milliseconds in which the incident listener checks whether there are incidents to report.

If `interval-ms` is equal to 1 minute (60000 milliseconds), e-mails will be sent at most every minute 
(provided that there are incidents to report). That is, if there is a process in which an incident occurs every second, 
e-mails will be sent every minute.

If `interval-ms` is set to 5 minutes (300000 milliseconds) and incidents occur every second, e-mails will be sent every five minutes.

How many e-mails will be sent each time depends -- apart from
`interval-ms` and the presence of new incidents -- how many
address/CC pairs there are.

Imagine,

* the processes are configured so that all incident e-mails
  are sent to `bob@yourcompany.com`,
* incidents occur every second, and
* `intervalMs` is set to 5 minutes (`300000` milliseconds).

In this case, one e-mail will be sent to `bob@yourcompany.com`
every five minutes.

Now imagine that in some processes the incident e-mail
recipient is `bob@yourcompany.com`, and in
others -- `alice@yourcompany.com`.

Other things being equal, this means that every five minutes
at most 2 e-mails (one to `bob@yourcompany.com`, one to
`alice@yourcompany.com`) will be sent.

**Beware of OutOfMemoryError**

All the unsent incident information are stored in a `java.util.List` in JVM memory.
If huge numbers of incidents occur during the period of `interval-ms`, it will cause a `OutOfMemoryError`.
So, please be careful to adjust the value of the configuration property in order to be suitable for your use case.

### protocol

Only `smtp` or `smtps` is available. If using `smtp`, all configuration properties are `mail.smtp.*`; 
and if `smtps`, all configuration properties are `mail.smtps.*`.

For detail information about SMTP protocol configuration properties, please refer to the [docs](https://javadoc.io/doc/com.sun.mail/jakarta.mail/2.0.1/jakarta.mail/com/sun/mail/smtp/package-summary.html).

### mail-body-template

Template for the text of the incident e-mails. 
The placeholder `@INCIDENTS` marks the place where information about individual incidents will be output to. 
Each incident will be output as specified in the property `incident-template`.

For example:

```text
An error occurred in at least one of your processes!
Please check the following processes in your Camunda Cockpit:

@INCIDENTS

******************** AUTOMATED MESSAGE ************************
```

### incident-template

Template for each individual incident. 
Each of the incidents for a particular e-mail will be rendered using this template.
Then, all these texts are concatenated and put instead of the `@INCIDENTS` placeholder in `mail-body-template`.

For example:

```text
Process Activity: @ACTIVITY
Process Instance ID: @PROCESS_INSTANCE_ID
Incident Message: @MESSAGE
Incident Type: @INCIDENT_TYPE
Time of Occurrence: @TIME
Link to Process Instance: @URL
```

## Version Compatibility
The version based on Camunda Platform 7.24.0 and Spring Boot 3.5.5.
Therefore, the mail library should implement Jakarta Mail 2.0 rather then Java Mail 1.x.