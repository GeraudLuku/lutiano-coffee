package com.lutiano.api.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.PageResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Builds the {@code ResponseEntity<GenericApiResponse<T>>} every controller returns. While services still produce row maps, the
 * map is converted to the endpoint's typed response record here (same JSON keys, see the record's {@code @JsonProperty} names).
 * The mapper is the application's own, set once at start-up by {@code config.JacksonConfig}.
 */
public final class Responses {
  private static ObjectMapper mapper;

  private Responses() {}

  public static void use(ObjectMapper m) { mapper = m; }

  public static <T> ResponseEntity<GenericApiResponse<T>> ok(T body) { return ResponseEntity.ok(GenericApiResponse.ok(body)); }

  public static <T> ResponseEntity<GenericApiResponse<T>> created(T body) { return ResponseEntity.status(HttpStatus.CREATED).body(GenericApiResponse.ok(body)); }

  public static <T> ResponseEntity<GenericApiResponse<T>> accepted(T body) { return ResponseEntity.status(HttpStatus.ACCEPTED).body(GenericApiResponse.ok(body)); }

  public static <T> ResponseEntity<GenericApiResponse<T>> accepted(Object row, Class<T> type) { return accepted(as(row, type)); }

  /** A row map as the endpoint's typed response. */
  public static <T> ResponseEntity<GenericApiResponse<T>> ok(Object row, Class<T> type) { return ok(as(row, type)); }

  public static <T> ResponseEntity<GenericApiResponse<T>> created(Object row, Class<T> type) { return created(as(row, type)); }

  public static <T> ResponseEntity<GenericApiResponse<List<T>>> list(List<?> rows, Class<T> type) {
    return ok(rows.stream().map(r -> as(r, type)).toList());
  }

  public static <T> ResponseEntity<GenericApiResponse<PageResponse<T>>> page(PageResponse<?> page, Class<T> itemType) {
    return ok(page.map(r -> as(r, itemType)));
  }

  /** A response with no body, for endpoints that answer 204. */
  public static ResponseEntity<Void> noContent() { return ResponseEntity.noContent().build(); }

  public static <T> T as(Object row, Class<T> type) {
    if (row == null || type.isInstance(row)) return type.cast(row);
    return mapper.convertValue(row, type);
  }
}
