package com.lutiano.api.service.sales;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lutiano.api.security.Actor;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.models.request.sales.SaleRequest;

/**
 * Recording a sale touches stock, cash, commission and loyalty ledgers that all require an open transaction
 * (propagation MANDATORY). If the entry points lose @Transactional, every sale fails with a 500, so guard them.
 */
class SaleTransactionTest {
  @Test void everyWayOfRecordingASaleRunsInOneTransaction() throws Exception {
    assertTrue(SaleService.class.getMethod("complete", Actor.class, String.class, SaleRequest.class).isAnnotationPresent(Transactional.class));
    assertTrue(SaleService.class.getMethod("completeOnBehalf", Actor.class, Actor.class, String.class, SaleRequest.class).isAnnotationPresent(Transactional.class));
  }
}
