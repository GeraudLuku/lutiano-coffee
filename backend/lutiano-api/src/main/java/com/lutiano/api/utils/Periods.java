package com.lutiano.api.utils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;

/** Business periods use Africa/Douala (UTC+1, no daylight saving); stored timestamps are UTC. */
public final class Periods {
  public static final ZoneId DOUALA = ZoneId.of("Africa/Douala");

  private Periods() {}

  /** SQL expression for the Douala business date of a UTC timestamp column. */
  public static String businessDate(String column) { return "DATE(DATE_ADD(" + column + ", INTERVAL 1 HOUR))"; }

  public static LocalDate today() { return LocalDate.now(DOUALA); }
  public static Timestamp startOf(LocalDate day) { return Timestamp.from(day.atStartOfDay(DOUALA).toInstant()); }
  public static Timestamp endOf(LocalDate day) { return startOf(day.plusDays(1)); }

  /** Inclusive day filters for list endpoints: null when the day is not given. */
  public static Timestamp startOrNull(LocalDate day) { return day == null ? null : startOf(day); }
  public static Timestamp endOrNull(LocalDate day) { return day == null ? null : endOf(day); }
}
