package com.lutiano.api.service.staff;

import java.util.Map;

/** The three seller roles share one app; each works one kind of sales operation (channel). */
public final class SellerRoles {
  /** Seller role → the operation type (channel) it works. */
  public static final Map<String, String> CHANNEL = Map.of(
      "FIXED_POS_BARISTA", "FIXED_POS",
      "MOBILE_BARISTA", "MOBILE",
      "MARKET_AGENT", "MARKET_AGENT");

  private SellerRoles() {}

  public static boolean matches(String sellerRole, String operationType) {
    return sellerRole != null && operationType != null && operationType.equals(CHANNEL.get(sellerRole));
  }
}
