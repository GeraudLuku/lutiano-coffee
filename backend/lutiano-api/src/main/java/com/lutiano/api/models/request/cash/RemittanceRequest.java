package com.lutiano.api.models.request.cash;

public record RemittanceRequest(Long amountXaf, String method, String recipientId, String bankReference, String note) {}
