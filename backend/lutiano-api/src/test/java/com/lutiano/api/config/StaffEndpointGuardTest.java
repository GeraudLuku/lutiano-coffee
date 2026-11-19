package com.lutiano.api.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

/** A customer's token never reaches a staff endpoint, whatever that endpoint checks. */
class StaffEndpointGuardTest {
  @Test void staffAndSellersPassCustomersAndAnonymousDoNot() {
    assertTrue(SecurityConfig.isStaffToken(new TestingAuthenticationToken("u", "x", "ROLE_SELLER")));
    assertTrue(SecurityConfig.isStaffToken(new TestingAuthenticationToken("u", "x", "ROLE_ADMIN")));
    assertFalse(SecurityConfig.isStaffToken(new TestingAuthenticationToken("c", "x", "ROLE_CUSTOMER")));
    assertFalse(SecurityConfig.isStaffToken(new AnonymousAuthenticationToken("k", "anon", List.of(AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS").get(0)))));
    assertFalse(SecurityConfig.isStaffToken(null));
  }
}
