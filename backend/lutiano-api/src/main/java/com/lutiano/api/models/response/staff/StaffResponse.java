package com.lutiano.api.models.response.staff;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record StaffResponse(
    Boolean active,
    String address,
    @JsonProperty("approval_alerts") Boolean approvalAlerts,
    String area,
    List<StaffAssignedStockResponse> assignedStock,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("deactivated_at") Object deactivatedAt,
    @JsonProperty("deactivated_by_name") Object deactivatedByName,
    @JsonProperty("deactivation_reason") Object deactivationReason,
    List<StaffDocumentResponse> documents,
    StaffSellerItemEligibilityResponse eligibility,
    Boolean eligibilityEnforced,
    String email,
    @JsonProperty("full_name") String fullName,
    String id,
    @JsonProperty("image_url") Object imageUrl,
    List<StaffIncomingStockResponse> incomingStock,
    String language,
    @JsonProperty("last_login_at") String lastLoginAt,
    @JsonProperty("mfa_enrolled_at") Object mfaEnrolledAt,
    StaffOpenShiftResponse openShift,
    List<StaffOperationResponse> operations,
    StaffPerformanceResponse performance,
    List<StaffPerformanceByOperationResponse> performanceByOperation,
    String phone,
    String role,
    @JsonProperty("seller_role") String sellerRole,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
