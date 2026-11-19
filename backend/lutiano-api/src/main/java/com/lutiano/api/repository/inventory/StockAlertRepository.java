package com.lutiano.api.repository.inventory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;

/** The open stock alerts, and the few product and location facts needed to decide whether stock is low. */
@Repository
public class StockAlertRepository {
  public static final String CENTRAL = "CENTRAL";
  private final JdbcTemplate db;

  public StockAlertRepository(JdbcTemplate db) { this.db = db; }

  public Optional<String> level(String scopeId, String productId) {
    return db.queryForList("select level from stock_alert where scope_id=? and product_id=?", String.class, scopeId, productId).stream().findFirst();
  }

  public void open(String scopeId, String productId, String level, int quantity) {
    db.update("insert into stock_alert(scope_id,product_id,level,quantity,opened_at) values(?,?,?,?,UTC_TIMESTAMP()) on duplicate key update level=values(level),quantity=values(quantity)", scopeId, productId, level, quantity);
  }

  public void setLevel(String scopeId, String productId, String level, int quantity) {
    db.update("update stock_alert set level=?,quantity=? where scope_id=? and product_id=?", level, quantity, scopeId, productId);
  }

  public void close(String scopeId, String productId) { db.update("delete from stock_alert where scope_id=? and product_id=?", scopeId, productId); }

  /** What decides an alert for a product: name, threshold, and whether it is a stock-tracked, active, sellable item. */
  public Optional<Map<String, Object>> product(String productId) {
    return db.queryForList("select name,low_stock_threshold,(active=true and track_stock=true and kind<>'PARENT') watched from product where id=?", productId).stream().findFirst();
  }

  /** The name of an active sales location, or empty for an inactive or unknown one. */
  public Optional<String> activeLocationName(String operationId) {
    return db.queryForList("select name from operation where id=? and active=true", String.class, operationId).stream().findFirst();
  }

  /** The variants of a parent product, or just the product itself. */
  public List<String> watchedIds(String productId) {
    List<String> variants = db.queryForList("select id from product where parent_id=?", String.class, productId);
    return variants.isEmpty() ? List.of(productId) : variants;
  }

  /** Every place that holds this product: central stock and each location with a balance. */
  public List<String> scopesHolding(String productId) {
    List<String> scopes = new ArrayList<>(List.of(CENTRAL));
    scopes.addAll(db.queryForList("select operation_id from inventory_balance where product_id=?", String.class, productId));
    return scopes;
  }
}
