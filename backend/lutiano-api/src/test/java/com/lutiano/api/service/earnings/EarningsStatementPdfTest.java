package com.lutiano.api.service.earnings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The statement says what the seller asked for, in their language, and marks months that are not final. */
class EarningsStatementPdfTest {
  static String text(byte[] pdf) throws Exception {
    PdfReader r = new PdfReader(pdf);
    StringBuilder sb = new StringBuilder();
    for (int p = 1; p <= r.getNumberOfPages(); p++) sb.append(new PdfTextExtractor(r).getTextFromPage(p)).append('\n');
    return sb.toString();
  }

  static EarningsStatementPdf.Model model(String lang, Set<String> sections) {
    var months = List.<Map<String, Object>>of(
        Map.of("month", "2026-10", "status", "FINAL", "commission_xaf", 32416L, "bonus_xaf", 15000L, "adjustments_xaf", -32L, "total_xaf", 47384L),
        Map.of("month", "2026-11", "status", "PROVISIONAL", "commission_xaf", 18876L, "bonus_xaf", 0L, "adjustments_xaf", -136L, "total_xaf", 18740L));
    var breakdown = Map.<String, Object>of("product_commission_xaf", 50000L, "addon_commission_xaf", 1292L,
        "top_products", List.of(Map.of("product_name", "Americano", "units", 29L, "commission_xaf", 2188L)));
    return new EarningsStatementPdf.Model("Martin Nguemaleu", YearMonth.of(2026, 10), YearMonth.of(2026, 11), lang, sections, months, breakdown,
        List.of(Map.of("name", "October target", "ends_on", "2026-10-31", "bonus_xaf", 15000L)),
        List.of(Map.of("reference", "EA-000001", "adjustment_type", "MANUAL", "amount_xaf", -32L, "note", "Correction")),
        List.of(Map.of("operation_name", "Akwa Mobile", "sales_count", 9L, "sales_xaf", 16200L, "commission_xaf", 648L)));
  }

  @Test void frenchStatementWithEverySection() throws Exception {
    String t = text(new EarningsStatementPdf().render(model("fr", Set.of("SUMMARY", "BREAKDOWN", "BONUSES", "ADJUSTMENTS", "SALES"))));
    assertTrue(t.contains("Relevé de gains"));
    assertTrue(t.contains("Martin Nguemaleu"));
    assertTrue(t.contains("provisoire"), "an open month is marked");
    assertTrue(t.contains("47 384 XAF"));
    assertTrue(t.contains("Détail des commissions") && t.contains("Primes") && t.contains("Ajustements") && t.contains("Ventes"));
  }

  @Test void onlyTheChosenSectionsAppear() throws Exception {
    String t = text(new EarningsStatementPdf().render(model("en", Set.of("SUMMARY"))));
    assertTrue(t.contains("Earnings statement") && t.contains("Summary"));
    assertFalse(t.contains("Commission breakdown"));
    assertFalse(t.contains("EA-000001"));
  }
}
