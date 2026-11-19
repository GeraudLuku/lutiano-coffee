package com.lutiano.api.service.earnings;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Rows;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Renders a seller's earnings statement (French or English) with the sections they chose. Amounts are whole XAF. */
@Component
public class EarningsStatementPdf {
  public record Model(String sellerName, YearMonth from, YearMonth to, String language, Set<String> sections,
                      List<Map<String, Object>> months, Map<String, Object> breakdown, List<Map<String, Object>> bonuses,
                      List<Map<String, Object>> adjustments, List<Map<String, Object>> operations) {}

  private static final Font TITLE = new Font(Font.HELVETICA, 18, Font.BOLD), H2 = new Font(Font.HELVETICA, 12, Font.BOLD),
      BODY = new Font(Font.HELVETICA, 9), BOLD = new Font(Font.HELVETICA, 9, Font.BOLD), SMALL = new Font(Font.HELVETICA, 8);

  public byte[] render(Model m) {
    try {
      return write(m);
    } catch (Exception e) {
      throw new IllegalStateException("Could not render the statement", e);
    }
  }

  private byte[] write(Model m) throws Exception {
    boolean fr = "fr".equalsIgnoreCase(m.language());
    Locale locale = fr ? Locale.FRANCE : Locale.ENGLISH;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document doc = new Document(PageSize.A4, 36, 36, 40, 40);
    PdfWriter.getInstance(doc, out);
    doc.open();
    doc.add(new Paragraph("LUTIANO COFFEE", TITLE));
    doc.add(new Paragraph(fr ? "Relevé de gains" : "Earnings statement", H2));
    doc.add(new Paragraph((fr ? "Vendeur : " : "Seller: ") + m.sellerName(), BODY));
    doc.add(new Paragraph((fr ? "Période : " : "Period: ") + m.from() + (m.from().equals(m.to()) ? "" : " → " + m.to()) + (fr ? " (heure de Douala)" : " (Douala time)"), BODY));
    doc.add(new Paragraph((fr ? "Établi le " : "Generated on ") + Periods.today(), BODY));
    boolean provisional = m.months().stream().anyMatch(x -> "PROVISIONAL".equals(x.get("status")));
    if (provisional) doc.add(new Paragraph(fr ? "Les mois marqués « provisoire » ne sont pas encore arrêtés : leurs montants peuvent changer." : "Months marked \"provisional\" are not finalised yet: their amounts may change.", SMALL));

    if (m.sections().contains("SUMMARY")) {
      heading(doc, fr ? "Résumé" : "Summary");
      PdfPTable t = table(new float[] {3, 2, 3, 3, 3, 3});
      for (String h : fr ? new String[] {"Mois", "Statut", "Commissions", "Primes", "Ajustements", "Total"} : new String[] {"Month", "Status", "Commissions", "Bonuses", "Adjustments", "Total"}) t.addCell(cell(h, BOLD, Element.ALIGN_LEFT));
      long c = 0, b = 0, a = 0;
      for (Map<String, Object> r : m.months()) {
        t.addCell(cell(String.valueOf(r.get("month")), BODY, Element.ALIGN_LEFT));
        t.addCell(cell("FINAL".equals(r.get("status")) ? (fr ? "arrêté" : "final") : (fr ? "provisoire" : "provisional"), BODY, Element.ALIGN_LEFT));
        t.addCell(cell(xaf(r.get("commission_xaf"), locale), BODY, Element.ALIGN_RIGHT));
        t.addCell(cell(xaf(r.get("bonus_xaf"), locale), BODY, Element.ALIGN_RIGHT));
        t.addCell(cell(xaf(r.get("adjustments_xaf"), locale), BODY, Element.ALIGN_RIGHT));
        t.addCell(cell(xaf(r.get("total_xaf"), locale), BOLD, Element.ALIGN_RIGHT));
        c += Rows.asLong(r.get("commission_xaf")); b += Rows.asLong(r.get("bonus_xaf")); a += Rows.asLong(r.get("adjustments_xaf"));
      }
      t.addCell(cell("Total", BOLD, Element.ALIGN_LEFT)); t.addCell(cell("", BOLD, Element.ALIGN_LEFT));
      t.addCell(cell(xaf(c, locale), BOLD, Element.ALIGN_RIGHT)); t.addCell(cell(xaf(b, locale), BOLD, Element.ALIGN_RIGHT));
      t.addCell(cell(xaf(a, locale), BOLD, Element.ALIGN_RIGHT)); t.addCell(cell(xaf(c + b + a, locale), BOLD, Element.ALIGN_RIGHT));
      doc.add(t);
    }
    if (m.sections().contains("BREAKDOWN")) {
      heading(doc, fr ? "Détail des commissions" : "Commission breakdown");
      Map<String, Object> bd = m.breakdown();
      PdfPTable t = table(new float[] {6, 3});
      row(t, fr ? "Commissions sur les produits" : "Product commissions", xaf(bd.get("product_commission_xaf"), locale));
      row(t, fr ? "Commissions sur les suppléments" : "Add-on commissions", xaf(bd.get("addon_commission_xaf"), locale));
      doc.add(t);
      @SuppressWarnings("unchecked") List<Map<String, Object>> top = (List<Map<String, Object>>) bd.get("top_products");
      if (!top.isEmpty()) {
        doc.add(new Paragraph(fr ? "Produits les plus rémunérateurs" : "Top products", BOLD));
        PdfPTable p = table(new float[] {6, 2, 3});
        for (Map<String, Object> r : top) { p.addCell(cell(String.valueOf(r.get("product_name")), BODY, Element.ALIGN_LEFT)); p.addCell(cell("x" + r.get("units"), BODY, Element.ALIGN_CENTER)); p.addCell(cell(xaf(r.get("commission_xaf"), locale), BODY, Element.ALIGN_RIGHT)); }
        doc.add(p);
      }
    }
    if (m.sections().contains("BONUSES")) {
      heading(doc, fr ? "Primes" : "Bonuses");
      if (m.bonuses().isEmpty()) doc.add(new Paragraph(fr ? "Aucune prime sur cette période." : "No bonuses in this period.", BODY));
      else {
        PdfPTable t = table(new float[] {6, 3, 3});
        for (Map<String, Object> r : m.bonuses()) { t.addCell(cell(String.valueOf(r.get("name")), BODY, Element.ALIGN_LEFT)); t.addCell(cell(String.valueOf(r.get("ends_on")), BODY, Element.ALIGN_LEFT)); t.addCell(cell(xaf(r.get("bonus_xaf"), locale), BODY, Element.ALIGN_RIGHT)); }
        doc.add(t);
      }
    }
    if (m.sections().contains("ADJUSTMENTS")) {
      heading(doc, fr ? "Ajustements" : "Adjustments");
      if (m.adjustments().isEmpty()) doc.add(new Paragraph(fr ? "Aucun ajustement sur cette période." : "No adjustments in this period.", BODY));
      else {
        PdfPTable t = table(new float[] {3, 3, 6, 3});
        for (Map<String, Object> r : m.adjustments()) {
          t.addCell(cell(String.valueOf(r.get("reference")), BODY, Element.ALIGN_LEFT)); t.addCell(cell(String.valueOf(r.get("adjustment_type")), BODY, Element.ALIGN_LEFT));
          t.addCell(cell(r.get("note") == null ? "" : String.valueOf(r.get("note")), SMALL, Element.ALIGN_LEFT)); t.addCell(cell(xaf(r.get("amount_xaf"), locale), BODY, Element.ALIGN_RIGHT));
        }
        doc.add(t);
      }
    }
    if (m.sections().contains("SALES")) {
      heading(doc, fr ? "Ventes" : "Sales");
      PdfPTable t = table(new float[] {5, 2, 3, 3});
      for (String h : fr ? new String[] {"Opération", "Ventes", "Montant", "Commission"} : new String[] {"Operation", "Sales", "Amount", "Commission"}) t.addCell(cell(h, BOLD, Element.ALIGN_LEFT));
      for (Map<String, Object> r : m.operations()) {
        t.addCell(cell(String.valueOf(r.get("operation_name")), BODY, Element.ALIGN_LEFT)); t.addCell(cell(String.valueOf(r.get("sales_count")), BODY, Element.ALIGN_CENTER));
        t.addCell(cell(xaf(r.get("sales_xaf"), locale), BODY, Element.ALIGN_RIGHT)); t.addCell(cell(xaf(r.get("commission_xaf"), locale), BODY, Element.ALIGN_RIGHT));
      }
      doc.add(t);
    }
    doc.close();
    return out.toByteArray();
  }

  private static void heading(Document doc, String text) throws Exception {
    Paragraph p = new Paragraph(text, H2);
    p.setSpacingBefore(14);
    p.setSpacingAfter(4);
    doc.add(p);
  }

  private static PdfPTable table(float[] widths) {
    PdfPTable t = new PdfPTable(widths);
    t.setWidthPercentage(100);
    return t;
  }

  private static void row(PdfPTable t, String label, String value) {
    t.addCell(cell(label, BODY, Element.ALIGN_LEFT));
    t.addCell(cell(value, BOLD, Element.ALIGN_RIGHT));
  }

  private static PdfPCell cell(String text, Font font, int align) {
    PdfPCell c = new PdfPCell(new Paragraph(text, font));
    c.setHorizontalAlignment(align);
    c.setBorderWidth(0.3f);
    c.setPadding(3);
    return c;
  }

  private static String xaf(Object v, Locale locale) {
    return NumberFormat.getIntegerInstance(locale).format(Rows.asLong(v)).replace(' ', ' ').replace(' ', ' ') + " XAF";
  }
}
