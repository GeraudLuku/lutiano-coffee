package com.lutiano.api.models.response.payout;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record BalanceDestinationResponse(
    @JsonProperty("account_name") String accountName,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("created_by") String createdBy,
    @JsonProperty("created_by_name") String createdByName,
    String id,
    Object note,
    String phone,
    String provider,
    @JsonProperty("seller_id") String sellerId,
    @JsonProperty("seller_name") String sellerName,
    String status,
    @JsonProperty("verified_at") String verifiedAt,
    @JsonProperty("verified_by") String verifiedBy,
    @JsonProperty("verified_by_name") String verifiedByName,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
