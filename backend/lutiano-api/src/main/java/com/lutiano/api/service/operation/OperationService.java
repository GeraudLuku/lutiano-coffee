package com.lutiano.api.service.operation;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import com.lutiano.api.utils.Lists;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.proxy.ImageStorage;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.service.reference.CameroonRegions;
import com.lutiano.api.security.Actor;
import com.lutiano.api.security.CurrentUser;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import com.lutiano.api.service.search.SearchCandidates;
import java.io.IOException;
import com.lutiano.api.repository.operation.OperationRepository;
import com.lutiano.api.models.request.operation.OperatingHoursRequest;
import com.lutiano.api.models.request.operation.OperationRequest;
import com.lutiano.api.utils.Enums;
import com.lutiano.api.definitions.enums.OperationType;

@Service
public class OperationService {
  public static final Set<String> TYPES = Enums.names(OperationType.class);
  /** Code prefix per location type, e.g. FP-001 (design: Sales Locations board). */
  private static final Map<String, String> CODE_PREFIX = Map.of("FIXED_POS", "FP", "MOBILE", "MB", "MARKET_AGENT", "MA");
  private static final int CODE_ATTEMPTS = 5;
  /** Cameroon's bounding box, to catch swapped or mistyped coordinates. */
  private static final double LAT_MIN = 1.6, LAT_MAX = 13.1, LNG_MIN = 8.4, LNG_MAX = 16.2;

  public record QrImage(String code, byte[] png) {}

  private final OperationRepository operations;
  private final QrCodeService qr;
  private final SearchCandidates candidates;
  private final CurrentUser current;
  private final AuditService audit;
  private final ImageStorage images;

  public OperationService(OperationRepository operations, QrCodeService qr, CurrentUser current, AuditService audit, ImageStorage images, SearchCandidates candidates) {
    this.candidates = candidates;
    this.operations = operations; this.qr = qr; this.current = current; this.audit = audit; this.images = images;
  }

  /** Sellers get their active assigned locations. Summary: totals by type over every location the actor can see. */
  public PageResponse<Map<String, Object>> list(Actor actor, String q, String type, String active, PageRequest req) {
    List<Map<String, Object>> all = actor.isSeller() ? operations.findActiveAssignedTo(actor.id()) : operations.findAll(Periods.startOf(Periods.today()));
    var text = candidates.find(actor, "OPERATION", q);
    List<Map<String, Object>> rows = all.stream().filter(o -> Lists.inCandidates(text, o, "code")
        && Lists.is(type, o.get("type")) && Lists.is(active, o.get("active"))).toList();
    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("total", all.size());
    for (String t : TYPES) summary.put(t, Lists.count(all, o -> t.equals(o.get("type"))));
    return PageResponse.slice(rows, req).withSummary(summary).withTruncated(text != null && text.truncated());
  }

  public Map<String, Object> get(String id) {
    return operations.findById(id).orElseThrow(() -> Validate.notFound("Sales location not found."));
  }

  public Map<String, Object> detail(Actor actor, String id) {
    current.requireOperationAccess(actor, id);
    Map<String, Object> op = get(id);
    op.put("sellers", operations.sellers(id));
    op.put("payment_methods", operations.acceptedMethods(id));
    op.put("hours", operations.hours(id));
    LocalDate today = Periods.today();
    op.put("stats", operations.salesStats(id, Periods.startOf(today), Periods.startOf(today.minusDays(6)), Periods.startOf(today.withDayOfMonth(1))));
    return op;
  }

  public Map<String, Object> create(Actor actor, OperationRequest in) {
    Clean c = clean(in);
    String id = UUID.randomUUID().toString();
    String code = insertWithGeneratedCode(id, c);
    operations.setAcceptedMethods(id, c.methods);
    audit.log(actor, "OPERATION_CREATED", "operation", id, c.name + " (" + code + ")");
    return get(id);
  }

  /** Next free code for the type's prefix; retries if another request took the same number first. */
  private String insertWithGeneratedCode(String id, Clean c) {
    String prefix = CODE_PREFIX.get(c.type);
    for (int attempt = 0; attempt < CODE_ATTEMPTS; attempt++) {
      String code = String.format("%s-%03d", prefix, operations.maxCodeNumber(prefix) + 1 + attempt);
      try {
        operations.insert(id, UUID.randomUUID().toString(), c.name, code, c.type, c.address, c.description, c.city, c.region, c.latitude, c.longitude, c.active);
        return code;
      } catch (DuplicateKeyException e) {
        // Another location was created with this code at the same moment; try the next number.
      }
    }
    throw Validate.conflict("Could not generate a unique location code. Try again.");
  }

  public Map<String, Object> update(Actor actor, String id, OperationRequest in) {
    Clean c = clean(in);
    if (operations.update(id, c.name, c.type, c.address, c.description, c.city, c.region, c.latitude, c.longitude) != 1) throw Validate.notFound("Sales location not found.");
    operations.setAcceptedMethods(id, c.methods);
    audit.log(actor, "OPERATION_UPDATED", "operation", id);
    return get(id);
  }


  /** Replaces the opening hours: one row per open weekday (1 = Monday to 7 = Sunday), in Douala time; no rows means always open. */
  public List<Map<String, Object>> setHours(Actor actor, String id, List<OperatingHoursRequest> in) {
    get(id);
    Set<Integer> seen = new HashSet<>();
    List<Object[]> rows = new ArrayList<>();
    for (OperatingHoursRequest h : in == null ? List.<OperatingHoursRequest>of() : in) {
      int day = Validate.range(h.weekday(), "Weekday", 1, 7);
      if (!seen.add(day)) throw Validate.bad("Each weekday can appear only once.");
      LocalTime opens, closes;
      try { opens = LocalTime.parse(h.opens()); closes = LocalTime.parse(h.closes()); }
      catch (RuntimeException e) { throw Validate.bad("Enter opening and closing times as HH:mm."); }
      if (!closes.isAfter(opens)) throw Validate.bad("A location must close after it opens (HH:mm, same day).");
      rows.add(new Object[] {day, opens, closes});
    }
    operations.replaceHours(id, rows);
    audit.log(actor, "OPERATION_HOURS_UPDATED", "operation", id);
    return operations.hours(id);
  }

  public void setActive(Actor actor, String id, boolean active) {
    if (operations.setActive(id, active) != 1) throw Validate.notFound("Sales location not found.");
    audit.log(actor, active ? "OPERATION_ACTIVATED" : "OPERATION_DEACTIVATED", "operation", id);
  }

  public Map<String, Object> qrInfo(Actor actor, String id) {
    current.requireOperationAccess(actor, id);
    Map<String, Object> row = operations.findQr(id).orElseThrow(() -> Validate.notFound("Sales location not found."));
    row.put("opaqueToken", row.remove("qr_token"));
    return row;
  }

  public QrImage qrImage(Actor actor, String id) {
    current.requireOperationAccess(actor, id);
    Map<String, Object> row = operations.findQr(id).orElseThrow(() -> Validate.notFound("Sales location not found."));
    return new QrImage((String) row.get("code"), qr.operationPng((String) row.get("qr_token")));
  }

  private record Clean(String name, String type, String address, String description, String city, String region, Double latitude, Double longitude, boolean active, List<String> methods) {}

  private Clean clean(OperationRequest in) {
    String region = Validate.optional(in.region(), "Region", 80);
    if (region != null && !CameroonRegions.isRegion(region)) throw Validate.bad("Choose one of Cameroon's regions.");
    if (in.latitude() == null || in.longitude() == null) throw Validate.bad("Set the location's position on the map.");
    if (in.latitude() < LAT_MIN || in.latitude() > LAT_MAX || in.longitude() < LNG_MIN || in.longitude() > LNG_MAX)
      throw Validate.bad("The map position must be in Cameroon.");
    return new Clean(
        Validate.text(in.name(), "Location name", 150),
        Validate.oneOf(in.type() == null ? "FIXED_POS" : in.type(), "Location type", TYPES),
        Validate.optional(in.address(), "Address", 255), Validate.optional(in.description(), "Description", 200), Validate.optional(in.city(), "City", 80), region,
        round(in.latitude()), round(in.longitude()), in.active() == null || in.active(), methods(in.paymentMethods()));
  }

  /** The payment methods a location accepts: known methods only, and at least one that can be used today. Defaults to all. */
  private List<String> methods(List<String> requested) {
    Map<String, Boolean> usable = operations.paymentMethodUsability();
    List<String> methods = requested == null || requested.isEmpty() ? List.copyOf(usable.keySet()) : requested.stream().distinct().toList();
    for (String m : methods) if (!usable.containsKey(m)) throw Validate.bad("Unknown payment method " + m + ".");
    if (methods.stream().noneMatch(usable::get)) throw Validate.bad("Accept at least one payment method that is available today (currently Cash).");
    return methods;
  }

  public String uploadImage(Actor actor, String id, MultipartFile file) throws Exception {
    get(id);
    String url = images.save(ImageStorage.OPERATIONS, id, file);
    operations.setImageUrl(id, url);
    audit.log(actor, "OPERATION_IMAGE_UPDATED", "operation", id);
    return url;
  }

  /** Every QR code matching the filters, as PNGs named after their location, in one zip. */
  public byte[] qrZip(String type, String active) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(out)) {
      for (Map<String, Object> o : operations.findAll(Periods.startOf(Periods.today()))) {
        if (!Lists.is(type, o.get("type")) || !Lists.is(active, o.get("active"))) continue;
        Map<String, Object> row = operations.findQr((String) o.get("id")).orElseThrow();
        zip.putNextEntry(new ZipEntry(row.get("code") + " " + String.valueOf(row.get("name")).replaceAll("[^\\w .-]", "") + ".png"));
        zip.write(qr.operationPng((String) row.get("qr_token")));
        zip.closeEntry();
      }
      zip.finish();
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Could not build the QR zip", e);
    }
  }

  /** Six decimals (about 10 cm), the column's precision. */
  private static Double round(Double v) {
    return Math.round(v * 1e6) / 1e6;
  }
}
