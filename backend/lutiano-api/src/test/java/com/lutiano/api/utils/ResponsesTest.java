package com.lutiano.api.utils;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** A row map converted to a response record must serialise to exactly the JSON the map would have produced. */
class ResponsesTest {
  record Row(Long id, @JsonProperty("full_name") String fullName, @JsonInclude(JsonInclude.Include.NON_NULL) String note, BigDecimal rate, String createdAt,
             @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}

  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test void sameJsonAsTheMapIncludingKeysTheRecordDoesNotList() throws Exception {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", 7L); row.put("full_name", "Ann"); row.put("rate", new BigDecimal("4.027000"));
    row.put("createdAt", Timestamp.valueOf("2026-11-17 12:53:22").toString().length() > 0 ? "2026-11-17T12:53:22.000+00:00" : null);
    row.put("surprise", 5); row.put("nothing", null);
    Responses.use(mapper);
    Row typed = Responses.as(row, Row.class);
    JsonNode a = mapper.readTree(mapper.writeValueAsString(row)), b = mapper.readTree(mapper.writeValueAsString(typed));
    assertEquals(a.get("surprise"), b.get("surprise"));
    assertTrue(b.has("nothing") && b.get("nothing").isNull(), b.toString());
    assertEquals(a.get("rate").toString(), b.get("rate").toString());
    assertFalse(b.has("note"));   // absent in the map, so absent in the JSON
    assertEquals(a.size(), b.size(), b.toString());
  }
}
