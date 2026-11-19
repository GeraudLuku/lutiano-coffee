package com.lutiano.api.repository.operation;

import java.sql.Time;
import java.time.LocalTime;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.lutiano.api.repository.search.SearchOutbox;
import java.util.LinkedHashMap;
import com.lutiano.api.definitions.enums.SearchType;

@Repository
public class OperationRepository {
  private static final String COLUMNS = "o.id,o.name,o.code,o.type,o.address,o.description,o.image_url,o.city,o.region,o.latitude,o.longitude,o.active,o.created_at";
  private static final String SELLER_NAMES = "(select group_concat(u.full_name order by u.full_name separator ', ') from seller_operation x join staff_user u on u.id=x.seller_id where x.operation_id=o.id) seller_names";
  private final JdbcTemplate db;
  private final SearchOutbox search;

  public OperationRepository(JdbcTemplate db, SearchOutbox search) { this.db = db; this.search = search; }

  /** Every location with its assigned sellers and what it sold since {@code dayStart}. */
  public List<Map<String, Object>> findAll(Timestamp dayStart) {
    return db.queryForList("select " + COLUMNS + "," + SELLER_NAMES + ",(select coalesce(sum(s.total_xaf),0) from sale s where s.operation_id=o.id and s.created_at>=?) today_xaf from operation o order by o.name", dayStart);
  }

  public List<Map<String, Object>> findActiveAssignedTo(String sellerId) {
    return db.queryForList("select " + COLUMNS + " from operation o join seller_operation s on s.operation_id=o.id where s.seller_id=? and o.active=true order by o.name", sellerId);
  }

  /** Opening hours, one row per weekday (1 = Monday to 7 = Sunday). */
  public List<Map<String, Object>> hours(String operationId) {
    return db.queryForList("select weekday,opens,closes from operation_hours where operation_id=? order by weekday", operationId);
  }

  public void replaceHours(String operationId, List<Object[]> rows) {
    db.update("delete from operation_hours where operation_id=?", operationId);
    for (Object[] r : rows) db.update("insert into operation_hours(operation_id,weekday,opens,closes) values(?,?,?,?)", operationId, r[0], Time.valueOf((LocalTime) r[1]), Time.valueOf((LocalTime) r[2]));
  }

  public Optional<Map<String, Object>> findById(String id) {
    return db.queryForList("select " + COLUMNS + " from operation o where o.id=?", id).stream().findFirst();
  }

  public Optional<Map<String, Object>> findQr(String id) {
    return db.queryForList("select id,code,name,type,active,qr_token from operation where id=?", id).stream().findFirst();
  }

  public boolean isActive(String id) {
    List<Boolean> rows = db.queryForList("select active from operation where id=?", Boolean.class, id);
    return !rows.isEmpty() && Boolean.TRUE.equals(rows.get(0));
  }

  public void insert(String id, String qrToken, String name, String code, String type, String address, String description, String city, String region,
                     Double latitude, Double longitude, boolean active) {
    db.update("insert into operation(id,qr_token,name,code,type,address,description,city,region,latitude,longitude,active,created_at) values(?,?,?,?,?,?,?,?,?,?,?,?,UTC_TIMESTAMP())",
        id, qrToken, name, code, type, address, description, city, region, latitude, longitude, active);
    search.changed(SearchType.OPERATION, id);
  }

  /** The code is not updatable: it is printed with the permanent QR code. */
  public int update(String id, String name, String type, String address, String description, String city, String region, Double latitude, Double longitude) {
    int n = db.update("update operation set name=?,type=?,address=?,description=?,city=?,region=?,latitude=?,longitude=? where id=?",
        name, type, address, description, city, region, latitude, longitude, id);
    search.changed(SearchType.OPERATION, id);
    search.changedAll(SearchType.SALE, db.queryForList("select id from sale where operation_id=?", String.class, id));   // sales embed the location's name
    return n;
  }

  /** Highest numeric suffix among codes like "FP-012" for a prefix, or 0. */
  public int maxCodeNumber(String prefix) {
    Integer n = db.queryForObject(
        "select coalesce(max(cast(substring(code, ?) as unsigned)),0) from operation where code regexp ?",
        Integer.class, prefix.length() + 2, "^" + prefix + "-[0-9]+$");
    return n == null ? 0 : n;
  }

  public void setImageUrl(String id, String url) { db.update("update operation set image_url=? where id=?", url, id); }

  // ----- accepted payment methods -----

  /** Every platform payment method and whether it can be used today (an integration exists and it is enabled). */
  public Map<String, Boolean> paymentMethodUsability() {
    Map<String, Boolean> out = new LinkedHashMap<>();
    db.query("select code, (available=true and enabled=true) usable from payment_method order by sort_order", rs -> { out.put(rs.getString(1), rs.getBoolean(2)); });
    return out;
  }

  public List<String> acceptedMethods(String operationId) {
    return db.queryForList("select method from operation_payment_method where operation_id=? order by method", String.class, operationId);
  }

  public void setAcceptedMethods(String operationId, List<String> methods) {
    db.update("delete from operation_payment_method where operation_id=?", operationId);
    for (String m : methods) db.update("insert into operation_payment_method(operation_id,method) values(?,?)", operationId, m);
  }

  public boolean accepts(String operationId, String method) {
    Integer n = db.queryForObject("select count(*) from operation_payment_method where operation_id=? and method=?", Integer.class, operationId, method);
    return n != null && n > 0;
  }

  public int setActive(String id, boolean active) {
    int n = db.update("update operation set active=? where id=?", active, id);
    search.changed(SearchType.OPERATION, id);
    return n;
  }

  public List<Map<String, Object>> sellers(String id) {
    return db.queryForList("select u.id,u.full_name,u.email,u.phone,u.active from seller_operation x join staff_user u on u.id=x.seller_id where x.operation_id=? order by u.full_name", id);
  }

  public Map<String, Object> salesStats(String id, Timestamp dayStart, Timestamp weekStart, Timestamp monthStart) {
    return db.queryForMap(
        "select coalesce(sum(case when created_at>=? then total_xaf end),0) today_xaf," +
        " coalesce(sum(case when created_at>=? then total_xaf end),0) week_xaf," +
        " coalesce(sum(case when created_at>=? then total_xaf end),0) month_xaf," +
        " count(*) transactions from sale where operation_id=?", dayStart, weekStart, monthStart, id);
  }
}
