package com.lutiano.api.models.request.refund;

public record RefundRequest(String saleId, Long amountXaf, String reason, String note, String customerName, String customerPhone) {}
