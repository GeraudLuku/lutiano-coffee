package com.lutiano.api.repository.catalogue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Time;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.lutiano.api.service.catalogue.PublicCatalogueService;

/** "Open now" is worked out in Douala time from one row per open weekday; no rows means always open. */
class OpeningHoursTest {
  private static final ZoneId DOUALA = ZoneId.of("Africa/Douala");
  private static final List<Map<String, Object>> WEEKDAYS = List.of(
      Map.of("weekday", 1, "opens", Time.valueOf("07:00:00"), "closes", Time.valueOf("19:00:00")),
      Map.of("weekday", 2, "opens", Time.valueOf("07:00:00"), "closes", Time.valueOf("19:00:00")));

  @Test void openInsideTheHoursOnAListedDay() {
    assertTrue(PublicCatalogueService.openAt(WEEKDAYS, ZonedDateTime.of(2026, 11, 16, 12, 0, 0, 0, DOUALA)));   // a Monday
  }

  @Test void closedBeforeOpeningAtClosingAndOnUnlistedDays() {
    assertFalse(PublicCatalogueService.openAt(WEEKDAYS, ZonedDateTime.of(2026, 11, 16, 6, 59, 0, 0, DOUALA)));
    assertFalse(PublicCatalogueService.openAt(WEEKDAYS, ZonedDateTime.of(2026, 11, 16, 19, 0, 0, 0, DOUALA)));
    assertFalse(PublicCatalogueService.openAt(WEEKDAYS, ZonedDateTime.of(2026, 11, 18, 12, 0, 0, 0, DOUALA)));   // a Wednesday
  }

  @Test void noHoursMeansAlwaysOpen() {
    assertTrue(PublicCatalogueService.openAt(List.of(), ZonedDateTime.of(2026, 11, 18, 3, 0, 0, 0, DOUALA)));
  }
}
