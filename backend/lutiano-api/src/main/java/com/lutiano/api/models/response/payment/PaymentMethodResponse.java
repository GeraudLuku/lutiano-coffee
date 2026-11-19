package com.lutiano.api.models.response.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record PaymentMethodResponse(
    Boolean available,
    String code,
    Boolean enabled,
    String label,
    @JsonProperty("merchant_account") Object merchantAccount,
    String note,
    String provider,
    @JsonProperty("requires_reference") Boolean requiresReference,
    @JsonProperty("sort_order") Long sortOrder,
    @JsonProperty("updated_at") Object updatedAt,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
