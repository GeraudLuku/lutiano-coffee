package com.lutiano.api.models.response.customer;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record CustomerMeResponse(
    @JsonProperty("birth_date") Object birthDate,
    String code,
    @JsonProperty("created_at") String createdAt,
    Object email,
    @JsonProperty("full_name") String fullName,
    @JsonProperty("has_password") Boolean hasPassword,
    String id,
    @JsonProperty("image_url") Object imageUrl,
    String language,
    @JsonProperty("linked_accounts") List<Object> linkedAccounts,
    @JsonProperty("loyalty_qr") String loyaltyQr,
    @JsonProperty("marketing_consent") Boolean marketingConsent,
    String phone,
    @JsonProperty("tier_code") String tierCode,
    @JsonProperty("two_factor_enabled") Boolean twoFactorEnabled,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
