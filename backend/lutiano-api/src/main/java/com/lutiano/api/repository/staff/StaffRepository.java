package com.lutiano.api.repository.staff;

import com.lutiano.api.utils.SqlFilter;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.lutiano.api.repository.search.SearchOutbox;
import java.time.LocalDate;
import com.lutiano.api.definitions.enums.SearchType;

@Repository
public class StaffRepository {
  static final String COLUMNS = "id,email,full_name,phone,role,seller_role,address,area,image_url,language,approval_alerts,mfa_enrolled_at,active,created_at,last_login_at,deactivated_at,deactivation_reason," +
      "(select full_name from staff_user d where d.id=staff_user.deactivated_by) deactivated_by_name";
  private final JdbcTemplate db;
  private final SearchOutbox search;

  public StaffRepository(JdbcTemplate db, SearchOutbox search) { this.db = db; this.search = search; }

  public List<Map<String, Object>> findAll() {
    return db.queryForList("select " + COLUMNS + ",(select count(*) from seller_operation x where x.seller_id=staff_user.id) operation_count from staff_user order by full_name");
  }

  public Optional<Map<String, Object>> findById(String id) {
    return db.queryForList("select " + COLUMNS + " from staff_user where id=?", id).stream().findFirst();
  }

  public Optional<Map<String, Object>> findByEmail(String email) {
    return db.queryForList("select " + COLUMNS + " from staff_user where email=?", email).stream().findFirst();
  }

  public String passwordHash(String id) {
    return db.queryForObject("select password_hash from staff_user where id=?", String.class, id);
  }

  public void insert(String id, String email, String passwordHash, String fullName, String phone, String role, String sellerRole, String address, String area, String language) {
    db.update("insert into staff_user(id,email,password_hash,full_name,phone,role,seller_role,address,area,language,created_at) values(?,?,?,?,?,?,?,?,?,?,UTC_TIMESTAMP())",
        id, email, passwordHash, fullName, phone, role, sellerRole, address, area, language);
    search.changed(SearchType.STAFF, id);
  }

  public int update(String id, String email, String fullName, String phone, String role, String sellerRole, String address, String area, String language) {
    int n = db.update("update staff_user set email=?,full_name=?,phone=?,role=?,seller_role=?,address=?,area=?,language=? where id=?",
        email, fullName, phone, role, sellerRole, address, area, language, id);
    search.changed(SearchType.STAFF, id);
    search.changedAll(SearchType.SALE, db.queryForList("select id from sale where seller_id=?", String.class, id));   // sales embed the seller's name
    return n;
  }

  /** Deactivating also signs the account out everywhere. */
  public int deactivate(String id, String actorId, String reason) {
    int n = db.update("update staff_user set active=false,deactivated_at=UTC_TIMESTAMP(),deactivated_by=?,deactivation_reason=?,tokens_valid_after=UTC_TIMESTAMP() where id=?",
        actorId, reason, id);
    search.changed(SearchType.STAFF, id);
    return n;
  }

  public int reactivate(String id) {
    int n = db.update("update staff_user set active=true,deactivated_at=null,deactivated_by=null,deactivation_reason=null where id=?", id);
    search.changed(SearchType.STAFF, id);
    return n;
  }

  /** Rejects every sign-in token issued before now. */
  public void setImageUrl(String id, String url) { db.update("update staff_user set image_url=? where id=?", url, id); }

  public void markMfaEnrolled(String id) { db.update("update staff_user set mfa_enrolled_at=UTC_TIMESTAMP() where id=?", id); }

  /** Forgets that the person confirmed their phone: they confirm it again at their next sign-in. */
  public void clearMfa(String id) { db.update("update staff_user set mfa_enrolled_at=null where id=?", id); }

  public int revokeSessions(String id) { return db.update("update staff_user set tokens_valid_after=UTC_TIMESTAMP() where id=?", id); }

  public int setPasswordHash(String id, String hash) { return db.update("update staff_user set password_hash=? where id=?", hash, id); }
  public void touchLastLogin(String id) { db.update("update staff_user set last_login_at=UTC_TIMESTAMP() where id=?", id); }

  public boolean hasOpenShift(String sellerId) {
    Integer n = db.queryForObject("select count(*) from cash_shift where seller_id=? and status='OPEN'", Integer.class, sellerId);
    return n != null && n > 0;
  }

  // ----- seller ↔ operation assignments -----

  public List<Map<String, Object>> operationsOf(String sellerId) {
    return db.queryForList("select o.id,o.name,o.code,o.type,o.active from operation o join seller_operation s on s.operation_id=o.id where s.seller_id=? order by o.name", sellerId);
  }

  /** @return true if the assignment is new. */
  public boolean assign(String sellerId, String operationId) {
    return db.update("insert ignore into seller_operation(seller_id,operation_id) values(?,?)", sellerId, operationId) == 1;
  }

  /** @return true if an assignment was removed. */
  public boolean unassign(String sellerId, String operationId) {
    return db.update("delete from seller_operation where seller_id=? and operation_id=?", sellerId, operationId) == 1;
  }

  public void logAssignment(String sellerId, String operationId, String action, String actorId, String note) {
    db.update("insert into seller_assignment_log(id,seller_id,operation_id,action,actor_id,note,created_at) values(?,?,?,?,?,?,UTC_TIMESTAMP())",
        UUID.randomUUID().toString(), sellerId, operationId, action, actorId, note);
  }

  public PageResponse<Map<String, Object>> assignmentHistory(String sellerId, PageRequest req) {
    return new SqlFilter("select l.*,o.name operation_name,o.code operation_code,o.type operation_type,u.full_name actor_name" +
        " from seller_assignment_log l join operation o on o.id=l.operation_id left join staff_user u on u.id=l.actor_id")
        .and("l.seller_id=?", sellerId).page(db, "order by l.created_at desc, l.id desc", req);
  }

  public List<Map<String, Object>> assignments() {
    return db.queryForList("select s.id seller_id,s.full_name seller_name,s.seller_role,o.id operation_id,o.name operation_name,o.code operation_code,o.type operation_type from seller_operation x join staff_user s on s.id=x.seller_id join operation o on o.id=x.operation_id order by s.full_name,o.name");
  }

  // ----- seller overview -----

  /** Sellers with location names, 30-day performance, last sale and open-shift flag. */
  public List<Map<String, Object>> sellers(Timestamp since) {
    return db.queryForList("select " + COLUMNS + "," +
        " (select group_concat(o.name order by o.name separator ', ') from seller_operation x join operation o on o.id=x.operation_id where x.seller_id=staff_user.id) operation_names," +
        " (select count(*) from seller_operation x where x.seller_id=staff_user.id) operation_count," +
        " (select count(*) from seller_operation x join operation o on o.id=x.operation_id where x.seller_id=staff_user.id and o.type<>case staff_user.seller_role" +
        "   when 'FIXED_POS_BARISTA' then 'FIXED_POS' when 'MOBILE_BARISTA' then 'MOBILE' when 'MARKET_AGENT' then 'MARKET_AGENT' end) mismatched_operations," +
        " (select count(*) from sale s where s.seller_id=staff_user.id and s.created_at>=?) sales_30d," +
        " (select coalesce(sum(s.total_xaf),0) from sale s where s.seller_id=staff_user.id and s.created_at>=?) revenue_30d," +
        " (select max(s.created_at) from sale s where s.seller_id=staff_user.id) last_sale_at," +
        " (select count(*) from cash_shift c where c.seller_id=staff_user.id and c.status='OPEN') open_shift" +
        " from staff_user where role='SELLER' order by full_name", since, since);
  }

  /** Detailed performance for one seller since a moment. */
  public Map<String, Object> performance(String sellerId, Timestamp since) {
    return db.queryForMap("select count(*) sales, coalesce(sum(s.total_xaf),0) revenue_xaf," +
        " coalesce((select sum(l.quantity) from sale_line l join sale x on x.id=l.sale_id where x.seller_id=? and x.created_at>=?),0) items," +
        " (select max(created_at) from sale where seller_id=?) last_sale_at" +
        " from sale s where s.seller_id=? and s.created_at>=?", sellerId, since, sellerId, sellerId, since);
  }

  public List<Map<String, Object>> performanceByOperation(String sellerId, Timestamp since) {
    return db.queryForList("select o.id operation_id,o.name operation_name,o.type operation_type,count(*) sales,coalesce(sum(s.total_xaf),0) revenue_xaf" +
        " from sale s join operation o on o.id=s.operation_id where s.seller_id=? and s.created_at>=? group by o.id,o.name,o.type order by revenue_xaf desc", sellerId, since);
  }

  public Optional<Map<String, Object>> openShift(String sellerId) {
    return db.queryForList("select id,opening_float,expected_cash,opened_at from cash_shift where seller_id=? and status='OPEN'", sellerId).stream().findFirst();
  }

  /** Stock held at the seller's assigned locations (tracked items with stock). */
  public List<Map<String, Object>> assignedStock(String sellerId) {
    return db.queryForList("select o.id operation_id,o.name operation_name,p.id product_id,p.name product_name,p.sku,p.low_stock_threshold,b.quantity" +
        " from seller_operation x join operation o on o.id=x.operation_id join inventory_balance b on b.operation_id=o.id join product p on p.id=b.product_id" +
        " where x.seller_id=? and b.quantity>0 order by o.name,p.name", sellerId);
  }

  /** Transfers on their way to the seller's locations (approved or dispatched). */
  public List<Map<String, Object>> incomingStock(String sellerId) {
    return db.queryForList("select a.id,a.status,a.sent_quantity,a.created_at,a.dispatched_at,o.name operation_name,p.name product_name" +
        " from stock_allocation a join seller_operation x on x.operation_id=a.operation_id join operation o on o.id=a.operation_id join product p on p.id=a.product_id" +
        " where x.seller_id=? and a.status in ('PENDING_APPROVAL','APPROVED','IN_TRANSIT') order by a.created_at desc", sellerId);
  }

  // ----- eligibility documents -----

  private static final String DOCUMENT_SELECT = "select d.id,d.seller_id,d.doc_type,d.file_name,d.content_type,d.size_bytes,d.expires_on,d.note,d.status," +
      "d.uploaded_at,d.reviewed_at,d.review_note,ub.full_name uploaded_by_name,rb.full_name reviewed_by_name" +
      " from seller_document d left join staff_user ub on ub.id=d.uploaded_by left join staff_user rb on rb.id=d.reviewed_by";

  public List<Map<String, Object>> documents(String sellerId) {
    return db.queryForList(DOCUMENT_SELECT + " where d.seller_id=? order by d.uploaded_at desc", sellerId);
  }

  public Optional<Map<String, Object>> document(String id) { return db.queryForList(DOCUMENT_SELECT + " where d.id=?", id).stream().findFirst(); }

  /** The stored file's key and metadata, only if it belongs to that seller. */
  public Optional<Map<String, Object>> documentFile(String sellerId, String documentId) {
    return db.queryForList("select * from seller_document where id=? and seller_id=?", documentId, sellerId).stream().findFirst();
  }

  public void insertDocument(String id, String sellerId, String type, String key, String name, String contentType, long size,
                             LocalDate expiresOn, String note, String actorId) {
    db.update("insert into seller_document(id,seller_id,doc_type,object_key,file_name,content_type,size_bytes,expires_on,note,status,uploaded_by,uploaded_at)" +
        " values(?,?,?,?,?,?,?,?,?,'PENDING',?,UTC_TIMESTAMP())", id, sellerId, type, key, name, contentType, size, expiresOn, note, actorId);
  }

  public void reviewDocument(String id, String status, String actorId, String note) {
    db.update("update seller_document set status=?,reviewed_by=?,reviewed_at=UTC_TIMESTAMP(),review_note=? where id=?", status, actorId, note, id);
  }
}
