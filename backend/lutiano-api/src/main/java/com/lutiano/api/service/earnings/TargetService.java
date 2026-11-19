package com.lutiano.api.service.earnings;

import com.lutiano.api.service.notification.NotificationService;
import com.lutiano.api.service.notification.NotificationCatalog;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.repository.product.ProductRepository;
import com.lutiano.api.security.Actor;
import com.lutiano.api.security.Roles;
import com.lutiano.api.repository.staff.StaffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Date;
import com.lutiano.api.repository.earnings.EarningsRepository;
import com.lutiano.api.models.request.earnings.TargetRequest;
import com.lutiano.api.definitions.enums.BonusType;
import com.lutiano.api.utils.Enums;
import com.lutiano.api.definitions.enums.TargetMetric;
import com.lutiano.api.definitions.enums.TargetPeriod;

/**
 * Individual seller targets by value (net paid XAF) or quantity (units, of one product or all) over a daily, weekly
 * (Monday–Sunday), monthly or custom period. Achievement counts completed paid sales, reduced by refunds. The bonus is
 * earned only when the target is reached: FIXED pays bonus_xaf once, PER_UNIT pays bonus_xaf per unit sold in the period.
 */
@Service
public class TargetService {
  public static final Set<String> METRICS = Enums.names(TargetMetric.class);
  public static final Set<String> PERIODS = Enums.names(TargetPeriod.class);
  public static final Set<String> BONUS_TYPES = Enums.names(BonusType.class);


  private final EarningsRepository repo;
  private final StaffRepository staff;
  private final ProductRepository products;
  private final AuditService audit;
  private final NotificationService notifications;

  public TargetService(EarningsRepository repo, StaffRepository staff, ProductRepository products, AuditService audit,
                       NotificationService notifications) {
    this.repo = repo; this.staff = staff; this.products = products; this.audit = audit; this.notifications = notifications;
  }

  /**
   * After a sale: tells the seller once when a running target passes 80% and once when it is reached. Jumping straight past both
   * sends only the last. Called inside the sale transaction; the messages go out after it commits.
   */
  public void checkMilestones(String sellerId) {
    LocalDate today = Periods.today();
    for (Map<String, Object> t : repo.targetsEndingBetween(sellerId, today, today.plusYears(1))) {
      if (toDate(t.get("starts_on")).isAfter(today)) continue;
      Map<String, Object> p = withProgress(t);
      double pct = ((Number) p.get("progress_pct")).doubleValue();
      Integer top = null;
      for (int milestone : new int[] {80, 100}) {
        if (pct >= milestone && repo.recordMilestone((String) t.get("id"), milestone)) top = milestone;
      }
      if (top == null) continue;
      String name = (String) t.get("name");
      if (top == 100) notifications.send(NotificationCatalog.TARGET_GOAL_REACHED, sellerId, Map.of("target", name), "seller_target", (String) t.get("id"));
      else notifications.send(NotificationCatalog.TARGET_MILESTONE, sellerId, Map.of("target", name, "percent", String.valueOf(Math.round(pct))), "seller_target", (String) t.get("id"));
    }
  }

  /** state: CURRENT, UPCOMING or ENDED. Each row carries its live progress. */
  public PageResponse<Map<String, Object>> list(String sellerId, String status, String state, PageRequest req) {
    return repo.targets(sellerId, status, state, Periods.today(), req).map(this::withProgress);
  }

  public Map<String, Object> get(String id) {
    return withProgress(repo.target(Validate.id(id, "Target")).orElseThrow(() -> Validate.notFound("Target not found.")));
  }

  @Transactional
  public Map<String, Object> create(Actor actor, TargetRequest in) {
    String sellerId = Validate.id(in.sellerId(), "Seller");
    Map<String, Object> seller = staff.findById(sellerId).orElseThrow(() -> Validate.bad("Seller not found."));
    if (!Roles.SELLER.equals(seller.get("role")) || !Rows.truthy(seller.get("active"))) throw Validate.bad("Targets are set for active sellers.");
    String metric = Validate.oneOf(in.metric(), "Measure", METRICS);
    String productId = in.productId() == null || in.productId().isBlank() ? null : Validate.id(in.productId(), "Product");
    if (productId != null && products.findById(productId).isEmpty()) throw Validate.bad("Product not found.");
    String periodType = Validate.oneOf(in.periodType(), "Period", PERIODS);
    if (in.startsOn() == null) throw Validate.bad("Choose when the target starts.");
    LocalDate start = switch (periodType) {
      case "WEEKLY" -> in.startsOn().with(DayOfWeek.MONDAY);
      case "MONTHLY" -> in.startsOn().withDayOfMonth(1);
      default -> in.startsOn();
    };
    LocalDate end = switch (periodType) {
      case "DAILY" -> start;
      case "WEEKLY" -> start.plusDays(6);
      case "MONTHLY" -> start.with(TemporalAdjusters.lastDayOfMonth());
      default -> {
        if (in.endsOn() == null || in.endsOn().isBefore(start)) throw Validate.bad("The end date must be on or after the start date.");
        if (ChronoUnit.DAYS.between(start, in.endsOn()) > 366) throw Validate.bad("A custom period can be at most one year.");
        yield in.endsOn();
      }
    };
    long target = Validate.range(in.targetValue(), "VALUE".equals(metric) ? "Target amount" : "Target quantity", 1L, 10_000_000_000L);
    String bonusType = Validate.oneOf(in.bonusType(), "Bonus type", BONUS_TYPES);
    long bonus = Validate.range(in.bonusXaf(), "Bonus", 0L, 100_000_000L);
    String id = UUID.randomUUID().toString();
    repo.insertTarget(id, sellerId, Validate.text(in.name(), "Target name", 100), metric, productId, periodType, start, end, target, bonusType, bonus, actor.id());
    audit.log(actor, "TARGET_CREATED", "staff_user", sellerId, in.name() + ": " + target + ("VALUE".equals(metric) ? " XAF" : " units") + ", " + start + " to " + end);
    return get(id);
  }

  @Transactional
  public Map<String, Object> cancel(Actor actor, String id, String reason) {
    Map<String, Object> t = Validate.status(repo.lockTarget(Validate.id(id, "Target")).orElseThrow(() -> Validate.notFound("Target not found.")),
        "target", "cancelled", "ACTIVE");
    if (t.get("bonus_paid_xaf") != null) throw Validate.conflict("Its bonus was already included in a finalised period; adjust earnings instead.");
    String text = Validate.text(reason, "Reason", 500);
    repo.cancelTarget(id);
    audit.log(actor, "TARGET_CANCELLED", "staff_user", (String) t.get("seller_id"), t.get("name") + ": " + text);
    return get(id);
  }

  /** {achieved, progress_pct, reached, bonus_earned} for a target, from its sales so far. */
  public Map<String, Object> withProgress(Map<String, Object> t) {
    LocalDate start = toDate(t.get("starts_on")), end = toDate(t.get("ends_on"));
    Map<String, Object> a = repo.achievement((String) t.get("seller_id"), (String) t.get("product_id"), Periods.startOf(start), Periods.endOf(end));
    long achieved = "VALUE".equals(t.get("metric")) ? Rows.asLong(a.get("value_xaf")) : Rows.asLong(a.get("units"));
    long target = Rows.asLong(t.get("target_value"));
    boolean reached = achieved >= target;
    t.put("achieved", achieved);
    t.put("progress_pct", target == 0 ? 0 : Math.round(achieved * 1000.0 / target) / 10.0);
    t.put("reached", reached);
    t.put("bonus_earned", reached ? ("FIXED".equals(t.get("bonus_type")) ? Rows.asLong(t.get("bonus_xaf")) : Rows.asLong(t.get("bonus_xaf")) * Rows.asLong(a.get("units"))) : 0);
    LocalDate today = Periods.today();
    t.put("state", today.isBefore(start) ? "UPCOMING" : today.isAfter(end) ? "ENDED" : "CURRENT");
    return t;
  }

  /** After a refund: a bonus already frozen in a finalised period that is no longer reached is taken back once. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void recheckPaidBonuses(String sellerId, LocalDate saleDay, String refundId, String refundReference, Actor actor) {
    for (Map<String, Object> t : repo.paidTargetsCovering(sellerId, saleDay)) {
      if (Rows.truthy(withProgress(t).get("reached"))) continue;
      repo.markBonusReversed((String) t.get("id"));
      repo.insertAdjustment(sellerId, EarningsService.BONUS_REVERSAL, -Rows.asLong(t.get("bonus_paid_xaf")), "REFUND", refundId,
          "Bonus \"" + t.get("name") + "\" no longer reached after refund " + refundReference, actor.id(), null);
    }
  }

  static LocalDate toDate(Object v) { return v instanceof Date d ? d.toLocalDate() : LocalDate.parse(String.valueOf(v)); }
}
