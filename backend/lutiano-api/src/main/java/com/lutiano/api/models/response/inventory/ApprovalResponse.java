package com.lutiano.api.models.response.inventory;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ApprovalResponse(
    List<ApprovalAdjustmentsToApproveResponse> adjustmentsToApprove,
    List<ApprovalRequestsToApproveResponse> requestsToApprove,
    List<Object> requestsToDispatch,
    Boolean secondApproverRequired,
    List<Object> stocktakesToApprove,
    List<ApprovalTransfersToApproveResponse> transfersToApprove,
    List<ApprovalTransfersToDispatchResponse> transfersToDispatch,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
