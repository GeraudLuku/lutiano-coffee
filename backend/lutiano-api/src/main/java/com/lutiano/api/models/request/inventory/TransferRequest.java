package com.lutiano.api.models.request.inventory;

public record TransferRequest(String operationId, String productId, Integer quantity, String note) {}
