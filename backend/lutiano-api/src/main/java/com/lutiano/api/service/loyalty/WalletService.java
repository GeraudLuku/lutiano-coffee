package com.lutiano.api.service.loyalty;

import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.repository.common.References;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.service.inventory.StockLedger;
import com.lutiano.api.service.notification.NotificationCatalog;
import com.lutiano.api.service.notification.NotificationService;
import com.lutiano.api.security.Actor;
import com.lutiano.api.security.CurrentUser;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.repository.loyalty.LoyaltyRepository;
import com.lutiano.api.repository.loyalty.PointsRepository;
import com.lutiano.api.repository.loyalty.WalletRepository;
import com.lutiano.api.config.AppProperties;
import com.lutiano.api.utils.Enums;
import com.lutiano.api.definitions.enums.MovementType;
import com.lutiano.api.definitions.enums.WalletState;

/**
 * The rewards wallet. A customer redeems a reward in the app: its points are spent at once and it waits in the wallet, valid for a while.
 * At a sales point the seller scans its QR code and gives the reward (a free product comes out of that sales point's stock). A reward
 * not used in time expires; its points are not given back.
 */
@Service
public class WalletService {
  public static final String QR_PREFIX = "LUTIANO:RW:";
  private static final Logger log = LoggerFactory.getLogger(WalletService.class);
  private static final Set<String> STATES = Enums.names(WalletState.class);

  private final WalletRepository wallet;
  private final PointsRepository points;
  private final PointsService pointsService;
  private final PointsLedger ledger;
  private final LoyaltyRepository config;
  private final StockLedger stock;
  private final CurrentUser current;
  private final NotificationService notifications;
  private final AuditService audit;
  private final References references;
  private final int validityDays;

  public WalletService(WalletRepository wallet, PointsRepository points, PointsService pointsService, PointsLedger ledger, LoyaltyRepository config, StockLedger stock,
                       CurrentUser current, NotificationService notifications, AuditService audit, References references,
                       AppProperties.Rewards rewards) {
    this.wallet = wallet; this.points = points; this.pointsService = pointsService; this.ledger = ledger; this.config = config; this.stock = stock;
    this.current = current; this.notifications = notifications; this.audit = audit; this.references = references; this.validityDays = rewards.walletValidityDays();
  }

  // ----- the customer -----

  /** Spends the points of a reward and puts it in the wallet. */
  @Transactional
  public Map<String, Object> redeem(Actor me, String rewardId) {
    Map<String, Object> customer = ledger.lock(me.id());
    if (!"ACTIVE".equals(customer.get("status"))) throw Validate.conflict("This account is not active.");
    Map<String, Object> reward = config.reward(Validate.id(rewardId, "Reward")).orElseThrow(() -> Validate.notFound("Reward not found."));
    int cost = pointsService.requireEligible(me.id(), reward);
    String id = UUID.randomUUID().toString(), reference = references.dated("RWD", "redemption_seq"), token = UUID.randomUUID().toString();
    points.insertRedemption(id, reference, me.id(), (String) reward.get("id"), (String) reward.get("name"), cost, null, me.id());
    for (var t : ledger.debit(me.id(), cost, new PointsLedger.Entry(PointsLedger.REDEEMED, "REDEMPTION", id, reference, "Redeemed " + reward.get("name"), me.id()), false, null))
      points.insertRedemptionBatch(id, t.batchId(), t.points());
    Timestamp until = Timestamp.from(Instant.now().plus(validityDays, ChronoUnit.DAYS));
    wallet.makeWalletItem(id, token, until);
    audit.log(me, "REWARD_REDEEMED", "customer", me.id(), reference + " " + reward.get("name") + " (" + cost + " pts)");
    notifications.send(NotificationCatalog.REWARD_REDEEMED, me.id(),
        Map.of("reward", (String) reward.get("name"), "until", DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(Periods.DOUALA).format(until.toInstant())), "reward_redemption", id);
    return item(me, id);
  }

  public PageResponse<Map<String, Object>> mine(Actor me, String state, PageRequest req) {
    String s = state == null || state.isBlank() ? null : Validate.oneOf(state, "State", STATES);
    return wallet.ofCustomer(me.id(), s, req).map(WalletService::view);
  }

  /** One wallet item; while it can still be used it carries the QR text to show at the sales point. */
  public Map<String, Object> item(Actor me, String id) {
    Map<String, Object> x = wallet.ofCustomer(me.id(), Validate.id(id, "Reward")).orElseThrow(() -> Validate.notFound("Reward not found."));
    Map<String, Object> out = view(x);
    if ("AVAILABLE".equals(out.get("state"))) out.put("qr", QR_PREFIX + wallet.tokenOf(id).orElseThrow());
    return out;
  }

  // ----- the sales point -----

  /** What a scanned reward QR is, before the seller confirms. */
  public Map<String, Object> lookup(Actor seller, String code) {
    Map<String, Object> x = wallet.lockByToken(tokenOf(code)).orElseThrow(() -> Validate.notFound("This reward code is not valid."));
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("reward_name", x.get("reward_name"));
    out.put("customer_name", x.get("customer_name"));
    out.put("state", state(x));
    out.put("valid_until", x.get("valid_until"));
    out.put("requires_stock", x.get("requires_stock"));
    out.put("reward_type", x.get("reward_type"));
    out.put("discount_pct", x.get("discount_pct"));   // a discount voucher is given by taking this off the customer's bill
    return out;
  }

  /** Gives the reward: a free product leaves this sales point's stock. Works once. */
  @Transactional
  public Map<String, Object> use(Actor seller, String code, String operationId) {
    if (!current.isAssigned(seller, Validate.id(operationId, "Sales point"))) throw Validate.forbidden("This sales point is not assigned to you.");
    Map<String, Object> x = wallet.lockByToken(tokenOf(code)).orElseThrow(() -> Validate.notFound("This reward code is not valid."));
    String state = state(x);
    if ("USED".equals(state)) throw Validate.conflict("This reward was already used.");
    if (!"AVAILABLE".equals(state)) throw Validate.conflict("This reward has expired.");
    String id = (String) x.get("id");
    if (Rows.truthy(x.get("requires_stock")) && !stock.take(operationId, (String) x.get("product_id"), 1, MovementType.REWARD_OUT,
        new StockLedger.Ref("reward", id, (String) x.get("reference")), seller.id(), (String) x.get("reward_name")))
      throw Validate.conflict("There is none left of this item at this sales point, so the reward cannot be given here.");
    wallet.markUsed(id, seller.id(), operationId);
    audit.log(seller, "REWARD_USED", "customer", (String) x.get("customer_id"), x.get("reference") + " " + x.get("reward_name"));
    notifications.send(NotificationCatalog.REWARD_USED, (String) x.get("customer_id"), Map.of("reward", (String) x.get("reward_name")), "reward_redemption", id);
    return Map.of("used", true, "reward_name", x.get("reward_name"), "customer_name", x.get("customer_name"));
  }

  // ----- time -----

  /** Every hour: rewards past their date become expired and the customer is told. */
  @Scheduled(fixedDelay = 3_600_000, initialDelay = 120_000)
  public void expireDue() {
    try {
      for (Map<String, Object> x : wallet.due())
        if (wallet.expire((String) x.get("id")))
          notifications.send(NotificationCatalog.REWARD_EXPIRED, (String) x.get("customer_id"), Map.of("reward", (String) x.get("reward_name")), "reward_redemption", (String) x.get("id"));
    } catch (RuntimeException e) { log.error("Could not expire rewards: {}", e.toString()); }
  }

  // ----- helpers -----

  private static String tokenOf(String code) {
    String c = code == null ? "" : code.trim();
    if (!c.startsWith(QR_PREFIX)) throw Validate.bad("This is not a LUTIANO reward code.");
    return c.substring(QR_PREFIX.length());
  }

  /** AVAILABLE, USED or EXPIRED from the redemption row (a reversed or out-of-date one is expired). */
  public static String state(Map<String, Object> x) {
    if (!"COMPLETED".equals(x.get("status"))) return "EXPIRED";
    if ("USED".equals(x.get("use_state"))) return "USED";
    Object until = x.get("valid_until");
    return "AVAILABLE".equals(x.get("use_state")) && until instanceof Timestamp t && t.toInstant().isAfter(Instant.now()) ? "AVAILABLE" : "EXPIRED";
  }

  private static Map<String, Object> view(Map<String, Object> x) {
    Map<String, Object> out = new LinkedHashMap<>();
    for (String k : new String[] {"id", "reference", "reward_name", "reward_type", "description", "image_url", "points", "valid_until", "used_at", "used_operation_name", "created_at"}) out.put(k, x.get(k));
    out.put("state", state(x));
    return out;
  }
}
