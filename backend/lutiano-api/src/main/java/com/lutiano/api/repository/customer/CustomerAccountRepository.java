package com.lutiano.api.repository.customer;

import java.sql.Date;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.repository.search.SearchOutbox;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.lutiano.api.definitions.enums.SearchType;

/** The sign-in side of a customer: credentials, profile fields, linked Google/Apple accounts and favourites. */
@Repository
public class CustomerAccountRepository {
  private static final String SELECT = "select id,code,full_name,phone,email,password_hash,language,birth_date,image_url,two_factor_enabled," +
      "marketing_consent,status,registered_at,loyalty_token,tier_code,created_at from customer where deleted_at is null";
  private final JdbcTemplate db;
  private final SearchOutbox search;

  public CustomerAccountRepository(JdbcTemplate db, SearchOutbox search) { this.db = db; this.search = search; }

  public Optional<Map<String, Object>> byId(String id) { return db.queryForList(SELECT + " and id=?", id).stream().findFirst(); }
  public Optional<Map<String, Object>> byPhone(String phone) { return db.queryForList(SELECT + " and phone=?", phone).stream().findFirst(); }
  public boolean emailTaken(String email, String exceptId) {
    Integer n = db.queryForObject("select count(*) from customer where email=? and id<>?", Integer.class, email, exceptId == null ? "" : exceptId);
    return n != null && n > 0;
  }

  /** A new customer who signed up in the app. */
  public void insertRegistered(String id, String code, String name, String phone, String email, String passwordHash, String language) {
    db.update("insert into customer(id,code,full_name,phone,phone_verified,status,loyalty_token,tier_code,email,password_hash,language,terms_accepted_at,registered_at,created_at)" +
        " values(?,?,?,?,true,'ACTIVE',?,'BRONZE',?,?,?,UTC_TIMESTAMP(),UTC_TIMESTAMP(),UTC_TIMESTAMP())",
        id, code, name, phone, UUID.randomUUID().toString(), email, passwordHash, language);
    search.changed(SearchType.CUSTOMER, id);
  }

  /** A customer record an administrator created earlier (with its points) is claimed by the person it belongs to. */
  public void claim(String id, String name, String email, String passwordHash, String language) {
    db.update("update customer set full_name=?,email=?,password_hash=?,language=?,phone_verified=true,terms_accepted_at=UTC_TIMESTAMP(),registered_at=UTC_TIMESTAMP() where id=?",
        name, email, passwordHash, language, id);
    search.changed(SearchType.CUSTOMER, id);
  }

  /** Remembers when the customer last signed in. */
  public void touchLogin(String id) { db.update("update customer set last_login_at=UTC_TIMESTAMP() where id=?", id); }

  public void setPassword(String id, String hash) { db.update("update customer set password_hash=? where id=?", hash, id); }
  public void revokeSessions(String id, Timestamp before) { db.update("update customer set tokens_valid_after=? where id=?", before, id); }
  public void setTwoFactor(String id, boolean on) { db.update("update customer set two_factor_enabled=? where id=?", on, id); }
  public void setImage(String id, String url) { db.update("update customer set image_url=? where id=?", url, id); }
  public void setPhone(String id, String phone) { db.update("update customer set phone=?,phone_verified=true where id=?", phone, id); search.changed(SearchType.CUSTOMER, id); }

  public void updateProfile(String id, String name, String email, String language, Date birthDate, boolean marketing) {
    db.update("update customer set full_name=?,email=?,language=?,birth_date=?,marketing_consent=? where id=?", name, email, language, birthDate, marketing, id);
    search.changed(SearchType.CUSTOMER, id);
  }

  /** Removes what identifies the person and closes the account; sales and loyalty history stay for the accounts. */
  public void anonymise(String id) {
    db.update("update customer set full_name='Deleted customer',phone=CONCAT('deleted-',LEFT(REPLACE(id,'-',''),22)),email=NULL,password_hash=NULL,image_url=NULL,birth_date=NULL," +
        "status='SUSPENDED',two_factor_enabled=false,marketing_consent=false,deleted_at=UTC_TIMESTAMP(),tokens_valid_after=UTC_TIMESTAMP(),loyalty_token=UUID() where id=?", id);
    db.update("delete from customer_identity where customer_id=?", id);
    db.update("delete from customer_favourite where customer_id=?", id);
    db.update("delete from device_token where user_id=?", id);
    search.changed(SearchType.CUSTOMER, id);
  }

  // ----- Google and Apple -----

  public Optional<String> customerOfIdentity(String provider, String subject) {
    return db.queryForList("select customer_id from customer_identity where provider=? and subject=?", String.class, provider, subject).stream().findFirst();
  }

  public void addIdentity(String provider, String subject, String customerId, String email) {
    db.update("insert into customer_identity(provider,subject,customer_id,email) values(?,?,?,?)", provider, subject, customerId, email);
  }

  public int removeIdentity(String customerId, String provider) { return db.update("delete from customer_identity where customer_id=? and provider=?", customerId, provider); }

  public List<Map<String, Object>> identities(String customerId) {
    return db.queryForList("select provider,email,created_at from customer_identity where customer_id=? order by provider", customerId);
  }

  // ----- favourites -----

  /** A product a customer can heart: active, and not a size variant (the variants hang under their parent). */
  public boolean favouritable(String productId) {
    Integer n = db.queryForObject("select count(*) from product where id=? and active=true and kind<>'VARIANT'", Integer.class, productId);
    return n != null && n > 0;
  }

  public void addFavourite(String customerId, String productId) {
    db.update("insert ignore into customer_favourite(customer_id,product_id) values(?,?)", customerId, productId);
  }

  public void removeFavourite(String customerId, String productId) { db.update("delete from customer_favourite where customer_id=? and product_id=?", customerId, productId); }

  /** The customer's favourite products that can still be sold, latest first. */
  public PageResponse<Map<String, Object>> favourites(String customerId, PageRequest req) {
    Long total = db.queryForObject("select count(*) from customer_favourite f join product p on p.id=f.product_id where f.customer_id=? and p.active and p.kind<>'VARIANT'", Long.class, customerId);
    List<Map<String, Object>> rows = db.queryForList("select p.id,p.name,p.category,p.price_xaf,p.image_url,p.description from customer_favourite f join product p on p.id=f.product_id" +
        " where f.customer_id=? and p.active and p.kind<>'VARIANT' order by f.created_at desc, p.id limit ? offset ?", customerId, req.size(), req.offset());
    return PageResponse.of(rows, req, total == null ? 0 : total);
  }
}
