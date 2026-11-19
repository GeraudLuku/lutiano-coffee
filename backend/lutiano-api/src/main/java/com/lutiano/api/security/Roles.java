package com.lutiano.api.security;

import com.lutiano.api.definitions.enums.Role;
import java.util.Set;

/** The {@link Role} names as constants for {@code require(...)} and token claims; the enum is the single list of roles. */
public final class Roles {
  public static final String ADMIN = "ADMIN";
  public static final String INVENTORY_MANAGER = "INVENTORY_MANAGER";
  public static final String SELLER = "SELLER";
  /** The Customer app's accounts. Not a staff role: it is never in {@link #ALL} and customer tokens cannot reach staff endpoints. */
  public static final String CUSTOMER = "CUSTOMER";
  public static final Set<String> ALL = Set.of(Role.ADMIN.name(), Role.INVENTORY_MANAGER.name(), Role.SELLER.name());
  /** Back-office staff who use the Admin Portal. */
  public static final String[] STAFF = {ADMIN, INVENTORY_MANAGER};

  private Roles() {}
}
