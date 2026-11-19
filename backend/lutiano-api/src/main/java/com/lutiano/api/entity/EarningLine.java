package com.lutiano.api.entity;

/** One sale line as the loyalty rules see it: total paid for it (unit + add-ons × quantity). */
public record EarningLine(String productId, String parentId, String name, String category, int quantity, long totalXaf, boolean loyaltyEligible, boolean promotionEligible) {}
