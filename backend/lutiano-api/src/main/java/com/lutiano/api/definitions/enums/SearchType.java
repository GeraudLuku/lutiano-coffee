package com.lutiano.api.definitions.enums;

/** The kinds of record that are searchable. The name is the "type" stored in the search index and the outbox. */
public enum SearchType {
  PRODUCT,
  CATEGORY,
  ADDON,
  OPERATION,
  SALE,
  PAYMENT,
  REFUND,
  STAFF,
  CUSTOMER,
  PAYOUT,
  PAYOUT_DESTINATION,
  TARGET,
  COMMISSION_RULE,
  CASH_OUTFLOW,
  CASH_REMITTANCE,
  STOCKTAKE,
  ADJUSTMENT,
  TRANSFER,
  DISCREPANCY,
  REWARD,
  EARNING_RULE,
  REDEMPTION,
  POINTS_ADJUSTMENT,
  DELIVERY
}
