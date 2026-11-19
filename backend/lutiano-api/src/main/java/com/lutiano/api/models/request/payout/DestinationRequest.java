package com.lutiano.api.models.request.payout;

public record DestinationRequest(String sellerId, String provider, String phone, String accountName, String note) {}
