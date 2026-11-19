package com.lutiano.api.proxy;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import com.lutiano.api.utils.Validate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.config.AppProperties;

/**
 * The one place supporting documents (signed slips, receipts, ID scans…) are validated (PDF, JPEG, PNG or WebP, up to
 * 10 MB) and kept in the private documents bucket. A service stores the returned key and file details on its own record:
 * <pre>Saved s = evidence.save("payouts/" + payoutId, file, "evidence");</pre>
 */
@Service
public class EvidenceStorage {
  /** What was stored: {@code key} locates the object, the rest is saved on the owner's record. */
  public record Saved(String id, String key, String fileName, String contentType, long size) {}

  private static final Set<String> FILE_TYPES = Set.of("application/pdf", "image/jpeg", "image/png", "image/webp");
  private static final long MAX_BYTES = 10L * 1024 * 1024;

  private final ObjectStorage storage;
  private final String bucket;

  public EvidenceStorage(ObjectStorage storage, AppProperties.Storage config) {
    this.storage = storage; this.bucket = config.documentsBucket();
  }

  /** Stores the file as {@code keyPrefix/<new id>}. {@code defaultName} is used when the upload has no file name. */
  public Saved save(String keyPrefix, MultipartFile file, String defaultName) throws Exception {
    String type = file.getContentType();
    if (type == null || !FILE_TYPES.contains(type) || file.isEmpty() || file.getSize() > MAX_BYTES)
      throw Validate.bad("Upload a PDF, JPEG, PNG or WebP file up to 10 MB.");
    String name = Validate.text(file.getOriginalFilename() == null ? defaultName : file.getOriginalFilename(), "File name", 255);
    String id = UUID.randomUUID().toString(), key = keyPrefix + "/" + id;
    try (InputStream in = file.getInputStream()) { storage.put(bucket, key, in, file.getSize(), type); }
    return new Saved(id, key, name, type, file.getSize());
  }

  public ObjectStorage.StoredObject load(String key, String what) throws Exception {
    return storage.get(bucket, key).orElseThrow(() -> Validate.notFound("The " + what + " file is missing from storage."));
  }
}
