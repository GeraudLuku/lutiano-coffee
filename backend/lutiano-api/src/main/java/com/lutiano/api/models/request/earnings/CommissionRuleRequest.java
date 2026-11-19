package com.lutiano.api.models.request.earnings;

public record CommissionRuleRequest(String productId, String sellerRole, String basis, Integer rate, String note) {}
