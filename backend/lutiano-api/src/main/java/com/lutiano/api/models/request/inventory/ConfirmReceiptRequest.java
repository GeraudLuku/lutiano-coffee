package com.lutiano.api.models.request.inventory;

public record ConfirmReceiptRequest(String allocationId, Integer quantity, String note) {}
