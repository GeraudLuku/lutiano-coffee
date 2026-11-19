package com.lutiano.api.service.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * The one shape every searchable record is flattened into. Only allow-listed fields are ever copied in (see
 * docs/search/); phone numbers, emails, amounts and one-time codes never appear. {@code meta} keys must exist in
 * the strict mapping (resources/search/index-v1.json); {@code perm} narrows candidates, MariaDB re-verifies them.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchDocument(
    String type, String sourceId, String module,
    String title, String subtitle, String content, String reference, List<String> tags,
    Map<String, Object> meta, Perm perm, String indexedAt) {

  public record Perm(List<String> roles, String sellerId, String customerId, List<String> operationIds, @JsonProperty("public") boolean isPublic, String status) {}

  /** Elasticsearch document id: unique per type and source row. */
  public String id() { return type + ":" + sourceId; }
}
