package com.lutiano.api.repository.payment;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.SqlFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.lutiano.api.repository.search.SearchOutbox;
import com.lutiano.api.utils.Candidates;
import com.lutiano.api.definitions.enums.SearchType;

@Repository
public class PaymentRepository {
  private static final String SELECT =
      "select p.*, m.label method_label, s.reference sale_reference, s.total_xaf sale_total_xaf, o.name operation_name, o.code operation_code," +
      " u.full_name seller_name, cb.full_name confirmed_by_name" +
      " from payment p join payment_method m on m.code=p.method join sale s on s.id=p.sale_id join operation o on o.id=p.operation_id" +
      " join staff_user u on u.id=p.seller_id left join staff_user cb on cb.id=p.confirmed_by";
  private final JdbcTemplate db;
  private final SearchOutbox search;

  public PaymentRepository(JdbcTemplate db, SearchOutbox search) { this.db = db; this.search = search; }

  // ----- methods -----

  public List<Map<String, Object>> methods() { return db.queryForList("select * from payment_method order by sort_order"); }

  public Optional<Map<String, Object>> method(String code) {
    return db.queryForList("select * from payment_method where code=?", code).stream().findFirst();
  }

  public void updateMethod(String code, String label, String merchantAccount, boolean enabled) {
    db.update("update payment_method set label=?,merchant_account=?,enabled=?,updated_at=UTC_TIMESTAMP() where code=?", label, merchantAccount, enabled, code);
  }

  // ----- payments -----

  /** Payments are only ever inserted today. When Mobile Money adds status changes, call {@code search.changed(SearchType.PAYMENT, id)} after each one. */
  public void insert(String id, String reference, String saleId, String operationId, String sellerId, String shiftId, String method, long amount,
                     String status, String providerReference, String merchantAccount, Long cashReceived, Long change, String confirmedBy) {
    db.update("insert into payment(id,reference,sale_id,operation_id,seller_id,shift_id,method,amount_xaf,status,provider_reference,merchant_account," +
        "cash_received_xaf,change_xaf,confirmed_by,confirmed_at,created_at) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,case when ?='CONFIRMED' then UTC_TIMESTAMP() end,UTC_TIMESTAMP())",
        id, reference, saleId, operationId, sellerId, shiftId, method, amount, status, providerReference, merchantAccount, cashReceived, change, confirmedBy, status);
    search.changed(SearchType.PAYMENT, id);
  }

  public void insertEvent(String paymentId, String type, String from, String to, String source, String actorId, String details) {
    db.update("insert into payment_event(id,payment_id,event_type,from_status,to_status,source,actor_id,details,created_at) values(?,?,?,?,?,?,?,?,UTC_TIMESTAMP())",
        UUID.randomUUID().toString(), paymentId, type, from, to, source, actorId, details);
  }

  public Optional<Map<String, Object>> find(String id) { return db.queryForList(SELECT + " where p.id=?", id).stream().findFirst(); }

  public List<Map<String, Object>> forSale(String saleId) { return db.queryForList(SELECT + " where p.sale_id=? order by p.created_at", saleId); }

  public List<Map<String, Object>> events(String paymentId) {
    return db.queryForList("select e.*,u.full_name actor_name from payment_event e left join staff_user u on u.id=e.actor_id where e.payment_id=? order by e.created_at", paymentId);
  }

  /** One page of payments with totals (confirmed, cash, Mobile Money, pending) over every match. */
  public PageResponse<Map<String, Object>> search(Timestamp from, Timestamp to, String method, String status, String operationId, String sellerId, Candidates text, PageRequest req) {
    SqlFilter f = new SqlFilter(SELECT).and("p.created_at>=?", from).and("p.created_at<?", to).and("p.method=?", method)
        .and("p.status=?", status).and("p.operation_id=?", operationId).and("p.seller_id=?", sellerId)
        .searchIds(text, "p.id", "p.provider_reference");
    return f.page(db, "order by p.created_at desc, p.reference desc", req).withSummary(f.aggregate(db,
        "sum(status='CONFIRMED') confirmed_count, coalesce(sum(case when status='CONFIRMED' then amount_xaf end),0) confirmed_xaf," +
        " coalesce(sum(case when status='CONFIRMED' and method='CASH' then amount_xaf end),0) cash_xaf," +
        " coalesce(sum(case when status='CONFIRMED' and method<>'CASH' then amount_xaf end),0) mobile_money_xaf, sum(status='PENDING') pending_count"));
  }

  // ----- reconciliation -----

  /** Per Douala day: completed sales vs confirmed payments, by count and amount. */
  public List<Map<String, Object>> dailyTotals(Timestamp from, Timestamp to) {
    String saleDay = Periods.businessDate("s.created_at"), payDay = Periods.businessDate("p.created_at");
    return db.queryForList(
        "select d.day, coalesce(sa.sales,0) sales, coalesce(sa.sales_xaf,0) sales_xaf, coalesce(pa.payments,0) payments, coalesce(pa.payments_xaf,0) payments_xaf," +
        " coalesce(pa.cash_xaf,0) cash_xaf, coalesce(pa.momo_xaf,0) mobile_money_xaf from (" +
        "  select " + saleDay + " day from sale s where s.created_at>=? and s.created_at<? union select " + payDay + " from payment p where p.created_at>=? and p.created_at<?" +
        " ) d left join (select " + saleDay + " day, count(*) sales, sum(s.total_xaf) sales_xaf from sale s where s.created_at>=? and s.created_at<? group by " + saleDay + ") sa on sa.day=d.day" +
        " left join (select " + payDay + " day, count(*) payments, sum(p.amount_xaf) payments_xaf, sum(case when p.method='CASH' then p.amount_xaf else 0 end) cash_xaf," +
        "  sum(case when p.method<>'CASH' then p.amount_xaf else 0 end) momo_xaf from payment p where p.status='CONFIRMED' and p.created_at>=? and p.created_at<? group by " + payDay + ") pa on pa.day=d.day" +
        " order by d.day desc", from, to, from, to, from, to, from, to);
  }

  /** Sales whose confirmed payment is missing, duplicated, or does not match the sale amount or method. */
  public List<Map<String, Object>> exceptions(Timestamp from, Timestamp to) {
    return db.queryForList(
        "select s.id sale_id, s.reference sale_reference, s.created_at, s.total_xaf, s.payment_method sale_method, o.name operation_name, u.full_name seller_name," +
        " count(p.id) confirmed_payments, coalesce(sum(p.amount_xaf),0) paid_xaf, group_concat(distinct p.method) payment_methods" +
        " from sale s join operation o on o.id=s.operation_id join staff_user u on u.id=s.seller_id" +
        " left join payment p on p.sale_id=s.id and p.status='CONFIRMED'" +
        " where s.created_at>=? and s.created_at<? group by s.id, s.reference, s.created_at, s.total_xaf, s.payment_method, o.name, u.full_name" +
        " having confirmed_payments<>1 or paid_xaf<>s.total_xaf or payment_methods<>s.payment_method order by s.created_at desc", from, to);
  }
}
