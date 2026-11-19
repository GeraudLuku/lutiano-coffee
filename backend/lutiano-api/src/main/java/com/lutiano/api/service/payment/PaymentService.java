package com.lutiano.api.service.payment;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.utils.Candidates;
import com.lutiano.api.repository.payment.PaymentRepository;
import com.lutiano.api.models.request.payment.PaymentMethodRequest;

/**
 * Payments are recorded separately from sales (sale state ≠ payment state), each with an event history.
 * Cash is confirmed by the seller at the moment of sale. Mobile Money methods exist in configuration but stay
 * unavailable until a provider integration is built; they cannot be enabled or used.
 */
@Service
public class PaymentService {
  public static final String CASH = "CASH";

  private final PaymentRepository payments;
  private final AuditService audit;

  public PaymentService(PaymentRepository payments, AuditService audit) { this.payments = payments; this.audit = audit; }

  public List<Map<String, Object>> methods() { return payments.methods(); }

  /** Admins can rename a method or set its merchant account; only methods with an integration can be enabled. Cash stays enabled. */
  public Map<String, Object> updateMethod(Actor actor, String code, PaymentMethodRequest in) {
    Map<String, Object> m = payments.method(code).orElseThrow(() -> Validate.notFound("Payment method not found."));
    boolean enabled = in.enabled() == null ? Boolean.TRUE.equals(m.get("enabled")) : in.enabled();
    if (enabled && !Boolean.TRUE.equals(m.get("available")))
      throw Validate.conflict(m.get("label") + " is not available yet: its provider integration has not been set up.");
    if (CASH.equals(code) && !enabled) throw Validate.conflict("Cash cannot be switched off: it is the only available payment method.");
    payments.updateMethod(code, Validate.text(in.label(), "Label", 60), Validate.optional(in.merchantAccount(), "Merchant account", 60), enabled);
    audit.log(actor, "PAYMENT_METHOD_UPDATED", "payment_method", null, code + (enabled ? " enabled" : " disabled"));
    return payments.method(code).orElseThrow();
  }

  /** Fails unless the method exists, has an integration and is enabled. Called before a sale is recorded. */
  public void requireUsable(String code) {
    Map<String, Object> m = payments.method(code).orElseThrow(() -> Validate.bad("Unknown payment method: " + code + "."));
    if (!Boolean.TRUE.equals(m.get("available")) || !Boolean.TRUE.equals(m.get("enabled")))
      throw Validate.conflict(m.get("label") + " is not available yet. Take this payment in cash.");
  }

  /** Records the seller-confirmed cash payment for a sale, inside the sale's transaction. */
  @Transactional(propagation = Propagation.MANDATORY)
  public String recordCash(String saleId, String saleReference, String operationId, String sellerId, String shiftId, long amount, Long cashReceived) {
    String id = UUID.randomUUID().toString();
    Long change = cashReceived == null ? null : cashReceived - amount;
    payments.insert(id, "PAY-" + saleReference.substring(3), saleId, operationId, sellerId, shiftId, CASH, amount, "CONFIRMED",
        null, null, cashReceived, change, sellerId);
    payments.insertEvent(id, "CASH_CONFIRMED", null, "CONFIRMED", "SELLER", sellerId,
        cashReceived == null ? "Cash received" : "Cash received " + cashReceived + " XAF, change " + change + " XAF");
    return id;
  }

  public PageResponse<Map<String, Object>> search(LocalDate from, LocalDate to, String method, String status, String operationId, String sellerId, Candidates text, PageRequest req) {
    return payments.search(Periods.startOrNull(from), Periods.endOrNull(to),
        method, status, operationId, sellerId, text, req);
  }

  public Map<String, Object> detail(String id) {
    Map<String, Object> p = payments.find(id).orElseThrow(() -> Validate.notFound("Payment not found."));
    p.put("events", payments.events(id));
    return p;
  }

  /** The sale's payments with their events, for the sale page. */
  public List<Map<String, Object>> forSale(String saleId) {
    List<Map<String, Object>> rows = payments.forSale(saleId);
    rows.forEach(p -> p.put("events", payments.events((String) p.get("id"))));
    return rows;
  }

  /**
   * Daily totals of completed sales vs confirmed payments, and every sale whose payment is missing, duplicated
   * or does not match. A clean period has no exceptions and equal totals each day.
   */
  public Map<String, Object> reconciliation(LocalDate from, LocalDate to) {
    LocalDate last = to == null ? Periods.today() : to, first = from == null ? last.minusDays(29) : from;
    if (first.isAfter(last)) throw Validate.bad("The start date must be on or before the end date.");
    List<Map<String, Object>> days = payments.dailyTotals(Periods.startOf(first), Periods.endOf(last));
    days.forEach(d -> d.put("difference_xaf", Rows.asLong(d.get("payments_xaf")) - Rows.asLong(d.get("sales_xaf"))));
    List<Map<String, Object>> exceptions = payments.exceptions(Periods.startOf(first), Periods.endOf(last));
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("from", first.toString());
    out.put("to", last.toString());
    out.put("days", days);
    out.put("exceptions", exceptions);
    out.put("salesXaf", days.stream().mapToLong(d -> Rows.asLong(d.get("sales_xaf"))).sum());
    out.put("paymentsXaf", days.stream().mapToLong(d -> Rows.asLong(d.get("payments_xaf"))).sum());
    out.put("cashXaf", days.stream().mapToLong(d -> Rows.asLong(d.get("cash_xaf"))).sum());
    out.put("mobileMoneyXaf", days.stream().mapToLong(d -> Rows.asLong(d.get("mobile_money_xaf"))).sum());
    out.put("balanced", exceptions.isEmpty());
    return out;
  }

}
