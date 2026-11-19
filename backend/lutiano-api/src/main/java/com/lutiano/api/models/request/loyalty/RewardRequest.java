package com.lutiano.api.models.request.loyalty;

public record RewardRequest(String name, String rewardType, String description, Integer pointCost, String productId, Boolean requiresStock, Integer discountPct, Integer minPurchases, Long minSpendXaf, Boolean active) {}
