package com.lutiano.api.models.response.payout;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record PayoutSettingResponse(Long approvalThresholdXaf, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
