package com.lutiano.api.config;

import static org.junit.jupiter.api.Assertions.*;

import com.lutiano.api.api.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** The settings in application.yml (and the defaults for keys it does not list) reach the typed {@link AppProperties}. */
class AppPropertiesTest extends ApiIntegrationTest {
  @Autowired AppProperties props;

  @Test void valuesFromApplicationYml() {
    assertEquals(8, props.jwt().ttlHours());
    assertTrue(props.jwt().secret().length() >= 32);
    assertTrue(props.mfa().enabled());
    assertEquals(6, props.otp().length());
    assertEquals(300, props.otp().ttlSeconds());
    assertEquals("123456", props.otp().devCode());
    assertEquals("dev", props.sms().mode());
    assertEquals("NATIONAL_ID,CONTRACT", props.sellers().requiredDocuments());
    assertEquals("products", props.storage().bucket());
    assertEquals("seller-documents", props.storage().documentsBucket());
    assertEquals(5000, props.search().candidateCap());
    assertEquals(500, props.search().verificationCap());
    assertEquals(100, props.search().maxQueryLength());
    assertFalse(props.search().indexingEnabled());   // switched off for these tests in ApiIntegrationTest
    assertEquals("http://localhost:5173", props.cors().allowedOrigins());
  }

  @Test void defaultsForKeysTheFileDoesNotList() {
    assertEquals(30, props.orders().acceptTimeoutMinutes());
    assertEquals(30, props.rewards().walletValidityDays());
    assertEquals(14, props.dashboard().inactivityDays());
    assertTrue(props.inventory().secondApprover());
    assertTrue(props.refunds().secondApprover());
    assertFalse(props.sellers().enforceEligibility());
    assertEquals("", props.social().google().clientIds());
  }
}
