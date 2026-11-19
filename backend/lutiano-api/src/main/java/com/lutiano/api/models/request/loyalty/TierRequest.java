package com.lutiano.api.models.request.loyalty;

public record TierRequest(String name, String color, Long minAnnualSpendXaf, Integer multiplierPct, String benefits, Boolean active) {}
