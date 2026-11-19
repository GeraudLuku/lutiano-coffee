package com.lutiano.api.models.request.inventory;

/** operationId null = central stock; quantityDelta is signed. */
public record StockAdjustmentRequest(String operationId, String productId, Integer quantityDelta, String reason, String note) {}
