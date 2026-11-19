package com.lutiano.api.models.response.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record StatusDocumentsResponse(
    @JsonProperty("ADDON") Long aDDON,
    @JsonProperty("ADJUSTMENT") Long aDJUSTMENT,
    @JsonProperty("CASH_OUTFLOW") Long cASHOUTFLOW,
    @JsonProperty("CASH_REMITTANCE") Long cASHREMITTANCE,
    @JsonProperty("CATEGORY") Long cATEGORY,
    @JsonProperty("COMMISSION_RULE") Long cOMMISSIONRULE,
    @JsonProperty("CUSTOMER") Long cUSTOMER,
    @JsonProperty("DELIVERY") Long dELIVERY,
    @JsonProperty("DISCREPANCY") Long dISCREPANCY,
    @JsonProperty("EARNING_RULE") Long eARNINGRULE,
    @JsonProperty("OPERATION") Long oPERATION,
    @JsonProperty("PAYMENT") Long pAYMENT,
    @JsonProperty("PAYOUT") Long pAYOUT,
    @JsonProperty("PAYOUT_DESTINATION") Long pAYOUTDESTINATION,
    @JsonProperty("POINTS_ADJUSTMENT") Long pOINTSADJUSTMENT,
    @JsonProperty("PRODUCT") Long pRODUCT,
    @JsonProperty("REDEMPTION") Long rEDEMPTION,
    @JsonProperty("REFUND") Long rEFUND,
    @JsonProperty("REWARD") Long rEWARD,
    @JsonProperty("SALE") Long sALE,
    @JsonProperty("STAFF") Long sTAFF,
    @JsonProperty("STOCKTAKE") Long sTOCKTAKE,
    @JsonProperty("TARGET") Long tARGET,
    @JsonProperty("TRANSFER") Long tRANSFER,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
