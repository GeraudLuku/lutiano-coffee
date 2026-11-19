package com.lutiano.api.service.staff;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.proxy.EvidenceStorage;
import com.lutiano.api.proxy.ObjectStorage;
import com.lutiano.api.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.sql.Date;
import com.lutiano.api.repository.staff.StaffRepository;
import com.lutiano.api.config.AppProperties;
import com.lutiano.api.utils.Enums;
import com.lutiano.api.definitions.enums.SellerDocumentType;

/**
 * Seller eligibility documents (ID, contract…) kept in a private bucket. A seller is eligible when every
 * required document type has a VERIFIED, unexpired document (lutiano.sellers.required-documents).
 */
@Service
public class SellerDocumentService {
  public static final Set<String> TYPES = Enums.names(SellerDocumentType.class);

  public record DocumentFile(String fileName, String contentType, byte[] bytes) {}

  private final StaffRepository staff;
  private final EvidenceStorage files;
  private final AuditService audit;
  private final List<String> required;

  public SellerDocumentService(StaffRepository staff, EvidenceStorage files, AuditService audit,
                               AppProperties.Sellers sellers) {
    this.staff = staff; this.files = files; this.audit = audit;
    this.required = Arrays.stream(sellers.requiredDocuments().split(",")).map(String::trim).filter(TYPES::contains).toList();
  }

  public List<String> requiredTypes() { return required; }

  public List<Map<String, Object>> list(String sellerId) {
    return staff.documents(sellerId);
  }

  /** {eligible, required, missing, expired, pending}. Each required type needs a verified document that has not expired. */
  public Map<String, Object> eligibility(String sellerId) {
    LocalDate today = Periods.today();
    List<Map<String, Object>> docs = list(sellerId);
    List<String> missing = new ArrayList<>(), expired = new ArrayList<>(), pending = new ArrayList<>();
    for (String type : required) {
      List<Map<String, Object>> ofType = docs.stream().filter(d -> type.equals(d.get("doc_type"))).toList();
      boolean valid = ofType.stream().anyMatch(d -> "VERIFIED".equals(d.get("status")) && !isExpired(d, today));
      if (valid) continue;
      if (ofType.stream().anyMatch(d -> "VERIFIED".equals(d.get("status")))) expired.add(type);
      else if (ofType.stream().anyMatch(d -> "PENDING".equals(d.get("status")))) pending.add(type);
      else missing.add(type);
    }
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("eligible", missing.isEmpty() && expired.isEmpty() && pending.isEmpty());
    out.put("required", required);
    out.put("missing", missing);
    out.put("expired", expired);
    out.put("pending", pending);
    return out;
  }

  public Map<String, Object> upload(Actor actor, String sellerId, String docType, LocalDate expiresOn, String note, MultipartFile file) throws Exception {
    String type = Validate.oneOf(docType, "Document type", TYPES);
    EvidenceStorage.Saved saved = files.save(sellerId, file, "document");
    staff.insertDocument(saved.id(), sellerId, type, saved.key(), saved.fileName(), saved.contentType(), saved.size(), expiresOn, Validate.optional(note, "Note", 500), actor.id());
    audit.log(actor, "SELLER_DOCUMENT_UPLOADED", "staff_user", sellerId, type + ": " + saved.fileName());
    return get(saved.id());
  }

  /** Marks a document VERIFIED or REJECTED (a rejection needs a reason). */
  public Map<String, Object> review(Actor actor, String sellerId, String documentId, boolean verified, String note) {
    Map<String, Object> d = get(documentId);
    if (!sellerId.equals(d.get("seller_id"))) throw Validate.notFound("Document not found.");
    String reason = verified ? Validate.optional(note, "Note", 500) : Validate.text(note, "Reason", 500);
    staff.reviewDocument(documentId, verified ? "VERIFIED" : "REJECTED", actor.id(), reason);
    audit.log(actor, verified ? "SELLER_DOCUMENT_VERIFIED" : "SELLER_DOCUMENT_REJECTED", "staff_user", sellerId, d.get("doc_type") + (reason == null ? "" : ": " + reason));
    return get(documentId);
  }

  public DocumentFile download(Actor actor, String sellerId, String documentId) throws Exception {
    Map<String, Object> row = staff.documentFile(sellerId, documentId).orElseThrow(() -> Validate.notFound("Document not found."));
    ObjectStorage.StoredObject obj = files.load((String) row.get("object_key"), "document");
    audit.log(actor, "SELLER_DOCUMENT_VIEWED", "staff_user", sellerId, (String) row.get("doc_type"));
    return new DocumentFile((String) row.get("file_name"), (String) row.get("content_type"), obj.bytes());
  }

  private Map<String, Object> get(String id) {
    return staff.document(id).orElseThrow(() -> Validate.notFound("Document not found."));
  }

  private static boolean isExpired(Map<String, Object> d, LocalDate today) {
    Object e = d.get("expires_on");
    if (e == null) return false;
    LocalDate expires = e instanceof Date sql ? sql.toLocalDate() : LocalDate.parse(e.toString());
    return expires.isBefore(today);
  }
}
