package com.lutiano.api.models.request.inventory;

public record ReceiptRequest(String productId, Integer quantity, String source, String supplier, String reference, String note) {}
