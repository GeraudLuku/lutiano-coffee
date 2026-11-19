package com.lutiano.api.models.request.product;

public record VariantRequest(String variantName, String sku, String unit, Long priceXaf, Integer lowStockThreshold, Boolean trackStock, Boolean active) {}
