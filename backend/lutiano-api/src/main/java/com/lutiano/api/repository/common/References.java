package com.lutiano.api.repository.common;

import java.time.format.DateTimeFormatter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.lutiano.api.utils.Periods;

/**
 * Human-readable references from database sequences (NOCACHE, so numbers never jump):
 * dated ones such as SL-20261116-00012 and plain ones such as CU-000031.
 */
@Component
public class References {
  private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

  private final JdbcTemplate db;

  public References(JdbcTemplate db) { this.db = db; }

  /** PREFIX-yyyyMMdd-NNNNN using today's Douala date. */
  public String dated(String prefix, String sequence) {
    return prefix + "-" + Periods.today().format(DAY) + "-" + String.format("%05d", next(sequence));
  }

  /** PREFIX-yyyy-NNNNN using today's Douala year, e.g. SR-2026-00457. */
  public String yearly(String prefix, String sequence) {
    return prefix + "-" + Periods.today().getYear() + "-" + String.format("%05d", next(sequence));
  }

  /** PREFIX-NNNNNN. */
  public String plain(String prefix, String sequence) { return prefix + "-" + String.format("%06d", next(sequence)); }

  private long next(String sequence) {
    if (!sequence.matches("[a-z_]+")) throw new IllegalArgumentException("Bad sequence name");
    return db.queryForObject("select nextval(" + sequence + ")", Long.class);
  }
}
