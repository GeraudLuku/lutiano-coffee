package com.lutiano.api.models.response.staff;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record StaffSellerItemResponse(
    Boolean active,
    String address,
    @JsonProperty("approval_alerts") Boolean approvalAlerts,
    String area,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("deactivated_at") Object deactivatedAt,
    @JsonProperty("deactivated_by_name") Object deactivatedByName,
    @JsonProperty("deactivation_reason") Object deactivationReason,
    StaffSellerItemEligibilityResponse eligibility,
    String email,
    @JsonProperty("full_name") String fullName,
    String id,
    @JsonProperty("image_url") Object imageUrl,
    String language,
    @JsonProperty("last_login_at") String lastLoginAt,
    @JsonProperty("last_sale_at") String lastSaleAt,
    @JsonProperty("mfa_enrolled_at") Object mfaEnrolledAt,
    @JsonProperty("mismatched_operations") Long mismatchedOperations,
    @JsonProperty("open_shift") Long openShift,
    @JsonProperty("operation_count") Long operationCount,
    @JsonProperty("operation_names") String operationNames,
    String phone,
    @JsonProperty("revenue_30d") Long revenue30d,
    String role,
    @JsonProperty("sales_30d") Long sales30d,
    @JsonProperty("seller_role") String sellerRole,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
