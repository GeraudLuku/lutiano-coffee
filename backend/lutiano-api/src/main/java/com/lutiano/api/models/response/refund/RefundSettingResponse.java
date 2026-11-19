package com.lutiano.api.models.response.refund;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record RefundSettingResponse(List<String> reasons, Boolean secondApprover, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
