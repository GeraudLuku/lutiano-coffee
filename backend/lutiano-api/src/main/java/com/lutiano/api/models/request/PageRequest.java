package com.lutiano.api.models.request;

/**
 * A page of a list, bound straight from the query string ({@code ?page=&size=}) when a controller method takes it as a
 * parameter. {@code page} is 1-based; {@code size} defaults to 5 and is capped at 200.
 */
public record PageRequest(Integer page, Integer size) {
  public static final int DEFAULT_SIZE = 5, MAX_SIZE = 200;

  public PageRequest {
    page = page == null || page < 1 ? 1 : page;
    size = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
  }

  public static PageRequest of(Integer page, Integer size) { return new PageRequest(page, size); }

  public int offset() { return (page - 1) * size; }
}
