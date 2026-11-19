package com.lutiano.api.models.response.staff;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record StaffSellerItemEligibilityResponse(
    Boolean eligible,
    List<Object> expired,
    List<String> missing,
    List<Object> pending,
    List<String> required,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
