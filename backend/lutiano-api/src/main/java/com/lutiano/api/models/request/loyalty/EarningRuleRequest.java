package com.lutiano.api.models.request.loyalty;

import java.time.LocalDate;

public record EarningRuleRequest(String name, String method, String scope, String productId, String category, Integer points, Long amountXaf, Integer multiplierPct, LocalDate startsOn, LocalDate endsOn, Boolean active) {}
