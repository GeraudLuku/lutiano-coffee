package com.lutiano.api.security;

import java.util.Arrays;

/** The signed-in staff member, loaded fresh from the database for each request. */
public record Actor(String id, String email, String fullName, String role) {
  public boolean is(String... roles) { return Arrays.asList(roles).contains(role); }
  public boolean isSeller() { return Roles.SELLER.equals(role); }
}
