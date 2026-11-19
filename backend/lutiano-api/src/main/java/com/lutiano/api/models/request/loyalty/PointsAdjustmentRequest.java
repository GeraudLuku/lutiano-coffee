package com.lutiano.api.models.request.loyalty;

public record PointsAdjustmentRequest(String customerId, Integer points, String reason, String note) {}
