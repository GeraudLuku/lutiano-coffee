package com.lutiano.api.models.request.earnings;

public record EarningsAdjustmentRequest(String sellerId, Long amountXaf, String note) {}
