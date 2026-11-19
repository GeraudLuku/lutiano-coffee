package com.lutiano.api.models.request.refund;

public record RefundPayRequest(String fundSource, String shiftId, String advanceReference, Long advanceAmountXaf, Long advanceReturnedXaf, String recipientName, Boolean recipientAcknowledged, String note) {}
