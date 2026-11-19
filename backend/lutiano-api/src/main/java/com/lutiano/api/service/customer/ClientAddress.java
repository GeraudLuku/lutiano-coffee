package com.lutiano.api.service.customer;

import jakarta.servlet.http.HttpServletRequest;

/** The address a request came from: the first X-Forwarded-For hop when behind a proxy, otherwise the socket address. */
public final class ClientAddress {
  private ClientAddress() {}

  public static String of(HttpServletRequest req) {
    String forwarded = req.getHeader("X-Forwarded-For");
    return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : req.getRemoteAddr();
  }
}
