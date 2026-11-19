package com.lutiano.api.service.campaign;

import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.repository.earnings.EarningsRepository;
import com.lutiano.api.service.notification.NotificationCatalog;
import com.lutiano.api.service.notification.NotificationService;
import com.lutiano.api.repository.operation.OperationRepository;
import com.lutiano.api.security.Actor;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.repository.campaign.CampaignRepository;
import com.lutiano.api.models.request.campaign.CampaignRequest;
import com.lutiano.api.models.request.campaign.CampaignTierRequest;
import com.lutiano.api.utils.Enums;
import com.lutiano.api.definitions.enums.PeriodState;

/**
 * Sales campaigns. Over a period (and optionally one sales location) each seller's net sales (paid, less refunds) are measured against
 * tiers; the highest tier reached pays its bonus. Progress is live. After the campaign ends an administrator settles it once: each bonus
 * is posted to the seller's earnings as a CAMPAIGN adjustment and the seller is told. Refunds after settling do not take a bonus back.
 */
@Service
public class CampaignService {
  public static final String CAMPAIGN_ADJUSTMENT = "CAMPAIGN";
  private static final Set<String> STATES = Enums.names(PeriodState.class);
  private static final int MAX_TIERS = 10;


  private final CampaignRepository repo;
  private final EarningsRepository earnings;
  private final OperationRepository operations;
  private final NotificationService notifications;
  private final AuditService audit;

  public CampaignService(CampaignRepository repo, EarningsRepository earnings, OperationRepository operations, NotificationService notifications, AuditService audit) {
    this.repo = repo; this.earnings = earnings; this.operations = operations; this.notifications = notifications; this.audit = audit;
  }

  // ----- administrators -----

  @Transactional
  public Map<String, Object> create(Actor admin, CampaignRequest in) {
    String name = Validate.text(in.name(), "Name", 100);
    String description = Validate.optional(in.description(), "Description", 500);
    if (in.startsOn() == null || in.endsOn() == null) throw Validate.bad("Choose the first and last day of the campaign.");
    if (in.endsOn().isBefore(in.startsOn())) throw Validate.bad("The campaign cannot end before it starts.");
    if (in.endsOn().isBefore(Periods.today())) throw Validate.bad("The campaign has already ended.");
    String operationId = in.operationId() == null || in.operationId().isBlank() ? null : Validate.id(in.operationId(), "Location");
    if (operationId != null && operations.findById(operationId).isEmpty()) throw Validate.notFound("Location not found.");
    List<CampaignTierRequest> tiers = in.tiers() == null ? List.of() : in.tiers();
    if (tiers.isEmpty() || tiers.size() > MAX_TIERS) throw Validate.bad("Add between 1 and " + MAX_TIERS + " tiers.");
    Set<Long> seen = new HashSet<>();
    for (CampaignTierRequest t : tiers) {
      Validate.range(t.thresholdXaf(), "Sales threshold", 1, 1_000_000_000L);
      Validate.range(t.bonusXaf(), "Bonus", 1, 100_000_000L);
      if (!seen.add(t.thresholdXaf())) throw Validate.bad("Two tiers cannot have the same sales threshold.");
    }
    String id = UUID.randomUUID().toString();
    repo.insert(id, name, description, in.startsOn(), in.endsOn(), operationId, admin.id());
    for (CampaignTierRequest t : tiers) repo.insertTier(id, t.thresholdXaf(), t.bonusXaf());
    audit.log(admin, "CAMPAIGN_CREATED", "campaign", id, name);
    return detail(id);
  }

  public PageResponse<Map<String, Object>> list(String state, PageRequest req) {
    PageResponse<Map<String, Object>> page = repo.page(optionalState(state), null, Periods.today(), req);
    page.items().forEach(this::decorate);
    return page;
  }

  public Map<String, Object> detail(String id) {
    Map<String, Object> c = campaign(id);
    decorate(c);
    c.put("tiers", repo.tiers(id));
    if (c.get("settled_at") != null) c.put("paid", repo.paidTotals(id));
    return c;
  }

  public PageResponse<Map<String, Object>> standings(String id, PageRequest req) {
    Map<String, Object> c = campaign(id);
    List<Map<String, Object>> tiers = repo.tiers(id);
    PageResponse<Map<String, Object>> page = repo.standings(c, req);
    page.items().forEach(r -> r.put("bonus_xaf", bonusFor(tiers, Rows.asLong(r.get("sales_xaf")))));
    return page;
  }

  @Transactional
  public Map<String, Object> cancel(Actor admin, String id) {
    campaign(id);
    if (!repo.cancel(id)) throw Validate.conflict("This campaign is already settled or cancelled.");
    audit.log(admin, "CAMPAIGN_CANCELLED", "campaign", id);
    return detail(id);
  }

  /** Pays the bonuses of a finished campaign, once: one earnings adjustment and one message per seller who reached a tier. */
  @Transactional
  public Map<String, Object> settle(Actor admin, String id) {
    Map<String, Object> c = campaign(id);
    if (!"ACTIVE".equals(c.get("status"))) throw Validate.conflict("A cancelled campaign cannot be settled.");
    if (!CampaignRepository.day(c.get("ends_on")).isBefore(Periods.today())) throw Validate.conflict("The campaign can be settled after its last day.");
    if (!repo.markSettled(id)) throw Validate.conflict("This campaign is already settled.");
    List<Map<String, Object>> tiers = repo.tiers(id);
    String name = (String) c.get("name");
    for (Map<String, Object> s : repo.everyone(c)) {
      long sales = Rows.asLong(s.get("sales_xaf")), bonus = bonusFor(tiers, sales);
      if (bonus == 0) continue;
      String sellerId = (String) s.get("seller_id");
      String adjustment = earnings.insertAdjustment(sellerId, CAMPAIGN_ADJUSTMENT, bonus, "campaign", id, "Campaign: " + name, admin.id(), null);
      repo.insertBonus(id, sellerId, sales, bonus, adjustment);
      notifications.send(NotificationCatalog.CAMPAIGN_BONUS, sellerId, Map.of("campaign", name, "bonus", NotificationService.xaf(bonus)), "campaign", id);
    }
    audit.log(admin, "CAMPAIGN_SETTLED", "campaign", id, name);
    return detail(id);
  }

  // ----- sellers -----

  /** The campaigns that reach the seller, each with their own progress. */
  public PageResponse<Map<String, Object>> mine(Actor seller, String state, PageRequest req) {
    PageResponse<Map<String, Object>> page = repo.page(optionalState(state), seller.id(), Periods.today(), req);
    page.items().forEach(c -> { decorate(c); progress(seller.id(), c, repo.tiers((String) c.get("id"))); });
    return page;
  }

  public Map<String, Object> mine(Actor seller, String id) {
    Map<String, Object> c = campaign(id);
    if (!repo.reaches(seller.id(), c)) throw Validate.notFound("Campaign not found.");
    Map<String, Object> out = detail(id);
    out.remove("paid");
    progress(seller.id(), out, repo.tiers(id));
    return out;
  }

  // ----- shared -----

  /** The seller's net sales so far, the bonus they would get now, and what the next tier asks. */
  private void progress(String sellerId, Map<String, Object> c, List<Map<String, Object>> tiers) {
    long sales = repo.salesOf(sellerId, c);
    c.remove("created_by");
    c.put("sales_xaf", sales);
    c.put("bonus_xaf", bonusFor(tiers, sales));
    Map<String, Object> next = tiers.stream().filter(t -> Rows.asLong(t.get("threshold_xaf")) > sales).findFirst().orElse(null);
    c.put("next_threshold_xaf", next == null ? null : next.get("threshold_xaf"));
    c.put("next_bonus_xaf", next == null ? null : next.get("bonus_xaf"));
    c.put("paid_bonus", repo.bonusOf((String) c.get("id"), sellerId).map(b -> b.get("bonus_xaf")).orElse(null));
  }

  /** The bonus of the highest tier reached, 0 when none. Tiers are in ascending order of threshold. */
  public static long bonusFor(List<Map<String, Object>> tiers, long sales) {
    long bonus = 0;
    for (Map<String, Object> t : tiers) if (sales >= Rows.asLong(t.get("threshold_xaf"))) bonus = Rows.asLong(t.get("bonus_xaf"));
    return bonus;
  }

  /** Adds CURRENT, UPCOMING or ENDED to the row. */
  private void decorate(Map<String, Object> c) {
    LocalDate today = Periods.today(), from = CampaignRepository.day(c.get("starts_on")), to = CampaignRepository.day(c.get("ends_on"));
    String state = !"ACTIVE".equals(c.get("status")) || to.isBefore(today) ? "ENDED" : from.isAfter(today) ? "UPCOMING" : "CURRENT";
    c.put("state", state);
    c.put("settled", c.get("settled_at") != null);
  }

  private Map<String, Object> campaign(String id) {
    return new LinkedHashMap<>(repo.find(Validate.id(id, "Campaign")).orElseThrow(() -> Validate.notFound("Campaign not found.")));
  }

  private static String optionalState(String state) {
    return state == null || state.isBlank() ? null : Validate.oneOf(state, "State", STATES);
  }
}
