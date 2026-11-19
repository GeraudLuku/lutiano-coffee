package com.lutiano.api.models.response.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ReconciliationDayResponse(
    @JsonProperty("cash_xaf") Long cashXaf,
    String day,
    @JsonProperty("difference_xaf") Long differenceXaf,
    @JsonProperty("mobile_money_xaf") Long mobileMoneyXaf,
    Long payments,
    @JsonProperty("payments_xaf") Long paymentsXaf,
    Long sales,
    @JsonProperty("sales_xaf") Long salesXaf,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
