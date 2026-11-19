package com.lutiano.api.models.request.notification;

public record AnnouncementRequest(String title, String body, String audience, String sellerRole, String operationId, Boolean important) {}
