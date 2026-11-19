package com.lutiano.api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.lutiano.api.models.response.GenericApiResponse;

/** Every error is a {@link GenericApiResponse} with {status, code, message}, so clients can always show a readable message. */
@RestControllerAdvice
public class ApiErrors {
  private static final Logger log = LoggerFactory.getLogger(ApiErrors.class);

  static ResponseEntity<GenericApiResponse<Void>> body(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(GenericApiResponse.error(status, message));
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<GenericApiResponse<Void>> status(ResponseStatusException e) {
    HttpStatus s = HttpStatus.valueOf(e.getStatusCode().value());
    return body(s, e.getReason() != null ? e.getReason() : s.getReasonPhrase());
  }

  @ExceptionHandler(EmptyResultDataAccessException.class)
  ResponseEntity<GenericApiResponse<Void>> notFound(EmptyResultDataAccessException e) {
    return body(HttpStatus.NOT_FOUND, "The requested record was not found.");
  }

  @ExceptionHandler(DuplicateKeyException.class)
  ResponseEntity<GenericApiResponse<Void>> duplicate(DuplicateKeyException e) {
    return body(HttpStatus.CONFLICT, "A record with the same unique value (code, SKU, email or reference) already exists.");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<GenericApiResponse<Void>> integrity(DataIntegrityViolationException e) {
    log.warn("Data integrity violation", e);
    return body(HttpStatus.BAD_REQUEST, "The request contains invalid or missing values.");
  }

  @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
      MissingServletRequestParameterException.class})
  ResponseEntity<GenericApiResponse<Void>> badRequest(Exception e) {
    return body(HttpStatus.BAD_REQUEST, "The request is malformed or has a missing or invalid value.");
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  ResponseEntity<GenericApiResponse<Void>> missingHeader(MissingRequestHeaderException e) {
    return body(HttpStatus.BAD_REQUEST, "Missing required header: " + e.getHeaderName());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<GenericApiResponse<Void>> tooLarge(MaxUploadSizeExceededException e) {
    return body(HttpStatus.PAYLOAD_TOO_LARGE, "Files must be 5 MB or smaller.");
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<GenericApiResponse<Void>> mediaType(HttpMediaTypeNotSupportedException e) {
    return body(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Send the request body as JSON (Content-Type: application/json).");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<GenericApiResponse<Void>> method(HttpRequestMethodNotSupportedException e) {
    return body(HttpStatus.METHOD_NOT_ALLOWED, "This method is not supported for this endpoint.");
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<GenericApiResponse<Void>> noResource(NoResourceFoundException e) {
    return body(HttpStatus.NOT_FOUND, "Not found.");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<GenericApiResponse<Void>> unexpected(Exception e) {
    log.error("Unhandled error", e);
    return body(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Try again or contact support.");
  }
}
