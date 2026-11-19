package com.lutiano.api.models.response.payout;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record PayoutOverviewResponse(
    @JsonProperty("approval_count") Long approvalCount,
    @JsonProperty("approval_threshold_xaf") Long approvalThresholdXaf,
    @JsonProperty("approval_xaf") Long approvalXaf,
    @JsonProperty("destinations_pending") Long destinationsPending,
    @JsonProperty("outstanding_xaf") Long outstandingXaf,
    @JsonProperty("owed_xaf") Long owedXaf,
    @JsonProperty("paid_month_xaf") Long paidMonthXaf,
    @JsonProperty("paid_xaf") Long paidXaf,
    @JsonProperty("pending_xaf") Long pendingXaf,
    @JsonProperty("reversal_count") Long reversalCount,
    @JsonProperty("sellers_with_balance") Long sellersWithBalance,
    @JsonProperty("to_pay_count") Long toPayCount,
    @JsonProperty("to_pay_xaf") Long toPayXaf,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
