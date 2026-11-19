package com.lutiano.api.utils;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Input checks and the standard HTTP errors used by services. */
public final class Validate {
  private static final Pattern UUID_RE = Pattern.compile("^[0-9a-fA-F-]{36}$");
  private static final Pattern EMAIL_RE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  private Validate() {}

  public static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
  public static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
  public static ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
  public static ResponseStatusException forbidden(String message) { return new ResponseStatusException(HttpStatus.FORBIDDEN, message); }
  public static ResponseStatusException tooMany(String message) { return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, message); }
  public static ResponseStatusException unauthorized(String message) { return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message); }

  public static String text(String value, String field, int max) {
    if (value == null || value.isBlank()) throw bad(field + " is required.");
    String v = value.trim();
    if (v.length() > max) throw bad(field + " must be " + max + " characters or fewer.");
    return v;
  }

  public static String optional(String value, String field, int max) {
    if (value == null || value.isBlank()) return null;
    String v = value.trim();
    if (v.length() > max) throw bad(field + " must be " + max + " characters or fewer.");
    return v;
  }

  public static String oneOf(String value, String field, Set<String> allowed) {
    if (value == null || !allowed.contains(value)) throw bad(field + " must be one of " + allowed + ".");
    return value;
  }

  /** An app language, "fr" or "en" (any case); blank gives {@code whenBlank}. */
  public static String language(String value, String whenBlank) {
    return value == null || value.isBlank() ? whenBlank : oneOf(value.trim().toLowerCase(), "Language", LANGUAGES);
  }

  private static final Set<String> LANGUAGES = Set.of("fr", "en");

  public static String id(String value, String field) {
    if (value == null || !UUID_RE.matcher(value).matches()) throw bad(field + " is missing or invalid.");
    return value;
  }

  public static String email(String value) {
    String v = text(value, "Email", 190).toLowerCase();
    if (!EMAIL_RE.matcher(v).matches()) throw bad("Enter a valid email address.");
    return v;
  }

  public static void password(String value) {
    if (value == null || value.length() < 10) throw bad("Password must be at least 10 characters.");
    if (value.length() > 72) throw bad("Password must be 72 characters or fewer.");
  }

  public static int positive(Integer value, String field) {
    if (value == null || value < 1) throw bad(field + " must be at least 1.");
    return value;
  }

  /** Amounts in XAF and other counts that must be above zero. */
  public static long positive(Long value, String field) {
    if (value == null || value <= 0) throw bad(field + " must be greater than zero.");
    return value;
  }

  public static int range(Integer value, String field, int min, int max) {
    if (value == null || value < min || value > max) throw bad(field + " must be between " + min + " and " + max + ".");
    return value;
  }

  public static long range(Long value, String field, long min, long max) {
    if (value == null || value < min || value > max) throw bad(field + " must be between " + min + " and " + max + ".");
    return value;
  }

  /**
   * Workflow guard: a record must be in one of {@code allowed} statuses for the action ({@code verb}), e.g.
   * "This refund is paid and cannot be approved." Returns the row for chaining.
   */
  public static Map<String, Object> status(Map<String, Object> row, String what, String verb, String... allowed) {
    Object status = row.get("status");
    for (String s : allowed) if (s.equals(status)) return row;
    throw conflict("This " + what + " is " + String.valueOf(status).toLowerCase().replace('_', ' ') + " and cannot be " + verb + ".");
  }
}
