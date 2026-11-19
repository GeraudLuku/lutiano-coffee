package com.lutiano.api.models.request.earnings;

import java.time.LocalDate;

public record TargetRequest(String sellerId, String name, String metric, String productId, String periodType, LocalDate startsOn, LocalDate endsOn, Long targetValue, String bonusType, Long bonusXaf) {}
