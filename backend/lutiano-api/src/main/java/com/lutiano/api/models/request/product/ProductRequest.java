package com.lutiano.api.models.request.product;

/** requireVariants=true makes a PARENT: no price or stock of its own; variants are sold instead. */
public record ProductRequest(String sku, String name, String category, String description, String unit, Long priceXaf, Integer lowStockThreshold, Boolean active, Boolean trackStock, Boolean loyaltyEligible, Boolean promotionEligible, Boolean requireVariants) {}
