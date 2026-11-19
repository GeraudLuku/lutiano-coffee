package com.lutiano.api.models.response.payout;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record BalanceResponse(
    Boolean active,
    List<BalanceDestinationResponse> destinations,
    @JsonProperty("full_name") String fullName,
    @JsonProperty("image_url") Object imageUrl,
    @JsonProperty("last_paid_at") Object lastPaidAt,
    @JsonProperty("outstanding_xaf") Long outstandingXaf,
    @JsonProperty("owed_xaf") Long owedXaf,
    @JsonProperty("paid_xaf") Long paidXaf,
    @JsonProperty("pending_xaf") Long pendingXaf,
    String phone,
    @JsonProperty("seller_id") String sellerId,
    @JsonProperty("seller_role") String sellerRole,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
