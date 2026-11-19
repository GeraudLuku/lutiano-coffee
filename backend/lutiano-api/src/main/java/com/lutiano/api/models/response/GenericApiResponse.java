package com.lutiano.api.models.response;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

/**
 * The single envelope for every JSON response:
 * <pre>
 *   { "success": true,  "data": …, "timestamp": "…" }
 *   { "success": false, "error": { "status": 409, "code": "CONFLICT", "message": "…" }, "timestamp": "…" }
 * </pre>
 * Lists are a {@link Page} in {@code data}: {items, page, size, total, pages, summary}.
 * Controllers return {@code ResponseEntity<GenericApiResponse<T>>} (see utils.Responses), and {@link com.lutiano.api.exception.ApiErrors} builds error envelopes.
 * File downloads (PDF, PNG, documents) are not wrapped.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GenericApiResponse<T>(boolean success, T data, ErrorBody error, Instant timestamp) {

  public record ErrorBody(int status, String code, String message) {}

  public static <T> GenericApiResponse<T> ok(T data) { return new GenericApiResponse<>(true, data, null, Instant.now()); }

  public static GenericApiResponse<Void> error(HttpStatus status, String message) {
    return new GenericApiResponse<>(false, null, new ErrorBody(status.value(), status.name(), message), Instant.now());
  }
}
