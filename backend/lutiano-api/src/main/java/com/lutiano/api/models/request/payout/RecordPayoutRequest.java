package com.lutiano.api.models.request.payout;

public record RecordPayoutRequest(String sellerId, String periodId, Long amountXaf, String method, String note) {}
