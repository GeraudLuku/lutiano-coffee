package com.lutiano.api.models.request.notification;

public record NotificationEventRequest(Boolean smsEnabled, String fr, String en) {}
