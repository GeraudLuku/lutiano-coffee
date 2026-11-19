package com.lutiano.api.models.response.payment;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ReconciliationResponse(
    Boolean balanced,
    Long cashXaf,
    List<ReconciliationDayResponse> days,
    List<Object> exceptions,
    String from,
    Long mobileMoneyXaf,
    Long paymentsXaf,
    Long salesXaf,
    String to,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
