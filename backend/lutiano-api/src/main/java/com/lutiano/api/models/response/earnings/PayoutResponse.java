package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record PayoutResponse(
    @JsonProperty("amount_xaf") Long amountXaf,
    @JsonProperty("destination_name") String destinationName,
    String id,
    String method,
    @JsonProperty("paid_at") String paidAt,
    @JsonProperty("period_ends_on") String periodEndsOn,
    @JsonProperty("period_id") String periodId,
    @JsonProperty("period_starts_on") String periodStartsOn,
    @JsonProperty("provider_reference") String providerReference,
    String reference,
    @JsonProperty("requested_at") String requestedAt,
    PayoutStatementResponse statement,
    String status,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("destination_last4") String destinationLast4,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
