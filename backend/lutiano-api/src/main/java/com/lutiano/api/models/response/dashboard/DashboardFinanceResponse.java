package com.lutiano.api.models.response.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record DashboardFinanceResponse(
    @JsonProperty("commissions_xaf") Long commissionsXaf,
    Long customers,
    @JsonProperty("new_customers") Long newCustomers,
    @JsonProperty("payouts_outstanding_xaf") Long payoutsOutstandingXaf,
    @JsonProperty("payouts_paid_xaf") Long payoutsPaidXaf,
    @JsonProperty("points_earned") Long pointsEarned,
    @JsonProperty("points_redeemed") Long pointsRedeemed,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
