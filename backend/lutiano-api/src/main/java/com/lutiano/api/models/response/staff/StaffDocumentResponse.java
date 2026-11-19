package com.lutiano.api.models.response.staff;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record StaffDocumentResponse(
    @JsonProperty("content_type") String contentType,
    @JsonProperty("doc_type") String docType,
    @JsonProperty("expires_on") String expiresOn,
    @JsonProperty("file_name") String fileName,
    String id,
    String note,
    @JsonProperty("review_note") String reviewNote,
    @JsonProperty("reviewed_at") String reviewedAt,
    @JsonProperty("reviewed_by_name") String reviewedByName,
    @JsonProperty("seller_id") String sellerId,
    @JsonProperty("size_bytes") Long sizeBytes,
    String status,
    @JsonProperty("uploaded_at") String uploadedAt,
    @JsonProperty("uploaded_by_name") String uploadedByName,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
