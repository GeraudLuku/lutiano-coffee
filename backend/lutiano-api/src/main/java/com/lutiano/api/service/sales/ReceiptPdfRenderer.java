package com.lutiano.api.service.sales;

import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Rows;
import org.springframework.stereotype.Component;

/** Renders a sale (as returned by SaleService) as an 80 mm thermal-roll PDF receipt. */
@Component
public class ReceiptPdfRenderer {
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);
  private static final Font TITLE = new Font(Font.HELVETICA, 14, Font.BOLD);
  private static final Font SMALL = new Font(Font.HELVETICA, 8);
  private static final Font BODY = new Font(Font.HELVETICA, 9);
  private static final Font BOLD = new Font(Font.HELVETICA, 9, Font.BOLD);

  public byte[] render(Map<String, Object> sale) {
    try {
      return write(sale);
    } catch (Exception e) {
      throw new IllegalStateException("Could not render receipt", e);
    }
  }

  @SuppressWarnings("unchecked")
  private byte[] write(Map<String, Object> sale) throws Exception {
    List<Map<String, Object>> lines = (List<Map<String, Object>>) sale.get("lines");
    Timestamp created = (Timestamp) sale.get("created_at");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document doc = new Document(new Rectangle(226, 600), 14, 14, 18, 18);
    PdfWriter.getInstance(doc, out);
    doc.open();
    doc.add(centered("LUTIANO COFFEE", TITLE));
    doc.add(centered("Sales receipt", SMALL));
    doc.add(blank());
    doc.add(new Paragraph("Reference: " + sale.get("reference"), BODY));
    doc.add(new Paragraph("Date: " + created.toInstant().atZone(Periods.DOUALA).format(TIME) + " (Douala)", BODY));
    doc.add(new Paragraph("Location: " + sale.get("operation_name") + " (" + sale.get("operation_code") + ")", BODY));
    doc.add(new Paragraph("Seller: " + sale.get("seller_name"), BODY));
    doc.add(blank());
    PdfPTable t = new PdfPTable(new float[] {5, 2, 3});
    t.setWidthPercentage(100);
    for (Map<String, Object> l : lines) {
      t.addCell(cell(String.valueOf(l.get("product_name")), BODY, Element.ALIGN_LEFT));
      t.addCell(cell("x" + l.get("quantity"), BODY, Element.ALIGN_CENTER));
      t.addCell(cell(xaf(l.get("line_total_xaf")), BODY, Element.ALIGN_RIGHT));
      for (Map<String, Object> a : (List<Map<String, Object>>) l.getOrDefault("addons", List.of())) {
        t.addCell(cell("  + " + a.get("addon_name") + " (" + xaf(a.get("unit_price_xaf")) + ")", SMALL, Element.ALIGN_LEFT));
        t.addCell(cell("", SMALL, Element.ALIGN_LEFT));
        t.addCell(cell("", SMALL, Element.ALIGN_LEFT));
      }
    }
    t.addCell(cell("TOTAL", BOLD, Element.ALIGN_LEFT));
    t.addCell(cell("", BOLD, Element.ALIGN_LEFT));
    t.addCell(cell(xaf(sale.get("total_xaf")), BOLD, Element.ALIGN_RIGHT));
    doc.add(t);
    doc.add(blank());
    doc.add(new Paragraph("Payment: Cash", BODY));
    if (sale.get("cash_received_xaf") != null) {
      doc.add(new Paragraph("Cash received: " + xaf(sale.get("cash_received_xaf")), BODY));
      doc.add(new Paragraph("Change: " + xaf(sale.get("change_xaf")), BODY));
    }
    doc.add(blank());
    doc.add(centered("Thank you!", SMALL));
    doc.close();
    return out.toByteArray();
  }

  private static Paragraph centered(String text, Font font) {
    Paragraph p = new Paragraph(text, font);
    p.setAlignment(Element.ALIGN_CENTER);
    return p;
  }

  private static Paragraph blank() { return new Paragraph(" ", SMALL); }

  private static PdfPCell cell(String text, Font font, int align) {
    PdfPCell c = new PdfPCell(new Paragraph(text, font));
    c.setBorder(Rectangle.NO_BORDER);
    c.setHorizontalAlignment(align);
    c.setPaddingBottom(3);
    return c;
  }

  static String xaf(Object v) {
    return String.format(Locale.US, "%,d XAF", Rows.asLong(v));
  }
}
