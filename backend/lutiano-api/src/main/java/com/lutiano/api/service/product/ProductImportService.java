package com.lutiano.api.service.product;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.function.Function;
import org.springframework.dao.DuplicateKeyException;
import com.lutiano.api.models.request.product.ProductRequest;

/**
 * Creates products from a CSV file (header row, then one product per line). All or nothing: if any line is wrong,
 * nothing is created and the problems are listed with their line numbers. Columns: sku, name, category, price_xaf (required);
 * unit, description, low_stock_threshold, track_stock (yes/no), active (yes/no) optional. Products come in as simple products;
 * variants are added afterwards.
 */
@Service
public class ProductImportService {
  public static final String TEMPLATE = "sku,name,category,price_xaf,unit,description,low_stock_threshold,track_stock,active\nCOF-EXAMPLE,Example Coffee,Beverage,1500,cup,Short description,10,yes,yes\n";
  private static final int MAX_ROWS = 500, MAX_ERRORS = 10;
  private static final long MAX_BYTES = 1024 * 1024;

  private final ProductService products;

  public ProductImportService(ProductService products) { this.products = products; }

  @Transactional
  public Map<String, Object> importCsv(Actor actor, MultipartFile file) throws Exception {
    if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) throw Validate.bad("Upload a CSV file up to 1 MB.");
    List<List<String>> rows = parse(new String(file.getBytes(), StandardCharsets.UTF_8).replace("﻿", ""));
    if (rows.size() < 2) throw Validate.bad("The file needs a header row and at least one product.");
    List<String> header = rows.get(0).stream().map(h -> h.trim().toLowerCase()).toList();
    for (String required : List.of("sku", "name", "category", "price_xaf"))
      if (!header.contains(required)) throw Validate.bad("The header must contain the column \"" + required + "\".");
    if (rows.size() - 1 > MAX_ROWS) throw Validate.bad("Import at most " + MAX_ROWS + " products at a time.");
    List<String> errors = new ArrayList<>();
    int created = 0;
    for (int i = 1; i < rows.size(); i++) {
      List<String> r = rows.get(i);
      if (r.stream().allMatch(String::isBlank)) continue;
      try {
        products.create(actor, input(header, r));
        created++;
      } catch (ResponseStatusException e) {
        if (errors.size() < MAX_ERRORS) errors.add("Line " + (i + 1) + ": " + e.getReason());
      } catch (NumberFormatException e) {
        if (errors.size() < MAX_ERRORS) errors.add("Line " + (i + 1) + ": a number is not valid.");
      } catch (DuplicateKeyException e) {
        if (errors.size() < MAX_ERRORS) errors.add("Line " + (i + 1) + ": the SKU already exists.");
      }
    }
    // Anything wrong rolls the whole import back, so a half-imported catalogue is never left behind.
    if (!errors.isEmpty()) throw Validate.bad("Nothing was imported. " + String.join(" ", errors));
    return Map.of("created", created);
  }

  private static ProductRequest input(List<String> header, List<String> row) {
    Function<String, String> col = name -> { int i = header.indexOf(name); return i < 0 || i >= row.size() ? "" : row.get(i).trim(); };
    Function<String, Boolean> yes = name -> { String v = col.apply(name).toLowerCase(); return v.isEmpty() ? null : List.of("yes", "true", "1", "y").contains(v); };
    String threshold = col.apply("low_stock_threshold");
    return new ProductRequest(col.apply("sku"), col.apply("name"), col.apply("category"), col.apply("description"), col.apply("unit"),
        Long.parseLong(col.apply("price_xaf")), threshold.isEmpty() ? 10 : Integer.parseInt(threshold), yes.apply("active"), yes.apply("track_stock"), true, true, false);
  }

  /** A small CSV reader: commas, double-quoted fields with "" escapes, CRLF or LF line ends. */
  static List<List<String>> parse(String text) {
    List<List<String>> rows = new ArrayList<>();
    List<String> row = new ArrayList<>();
    StringBuilder cell = new StringBuilder();
    boolean quoted = false;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (quoted) {
        if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') { cell.append('"'); i++; }
        else if (c == '"') quoted = false;
        else cell.append(c);
      } else if (c == '"') quoted = true;
      else if (c == ',') { row.add(cell.toString()); cell.setLength(0); }
      else if (c == '\n' || c == '\r') {
        if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
        row.add(cell.toString()); cell.setLength(0); rows.add(row); row = new ArrayList<>();
      } else cell.append(c);
    }
    if (cell.length() > 0 || !row.isEmpty()) { row.add(cell.toString()); rows.add(row); }
    return rows;
  }
}
