package com.lutiano.api.models.request.loyalty;

public record LoyaltySettingsRequest(Boolean programActive, Integer basePoints, Long baseAmountXaf, Integer pointValueXaf, Integer expiryMonths, Integer expiryNoticeDays, Integer minRedeemPoints) {}
