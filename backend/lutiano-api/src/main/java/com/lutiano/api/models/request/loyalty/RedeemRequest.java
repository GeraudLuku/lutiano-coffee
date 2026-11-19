package com.lutiano.api.models.request.loyalty;

public record RedeemRequest(String customerId, String rewardId, String operationId, Boolean customerConfirmed) {}
