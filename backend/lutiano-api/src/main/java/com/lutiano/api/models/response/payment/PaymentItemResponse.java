package com.lutiano.api.models.response.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record PaymentItemResponse(
    @JsonProperty("amount_xaf") Long amountXaf,
    @JsonProperty("cash_received_xaf") Long cashReceivedXaf,
    @JsonProperty("change_xaf") Long changeXaf,
    @JsonProperty("confirmed_at") String confirmedAt,
    @JsonProperty("confirmed_by") String confirmedBy,
    @JsonProperty("confirmed_by_name") String confirmedByName,
    @JsonProperty("created_at") String createdAt,
    String currency,
    String id,
    @JsonProperty("merchant_account") Object merchantAccount,
    String method,
    @JsonProperty("method_label") String methodLabel,
    @JsonProperty("operation_code") String operationCode,
    @JsonProperty("operation_id") String operationId,
    @JsonProperty("operation_name") String operationName,
    @JsonProperty("payer_account") Object payerAccount,
    @JsonProperty("provider_reference") Object providerReference,
    String reference,
    @JsonProperty("sale_id") String saleId,
    @JsonProperty("sale_reference") String saleReference,
    @JsonProperty("sale_total_xaf") Long saleTotalXaf,
    @JsonProperty("seller_id") String sellerId,
    @JsonProperty("seller_name") String sellerName,
    @JsonProperty("shift_id") String shiftId,
    String status,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
