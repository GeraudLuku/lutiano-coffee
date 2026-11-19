package com.lutiano.api.proxy;

import java.io.InputStream;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.lutiano.api.utils.Validate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.config.AppProperties;

/**
 * The one place images are validated (JPEG, PNG or WebP, up to 5 MB), stored and read back. Any service can inject it:
 * <pre>String url = images.save(ImageStorage.STAFF, staffId, multipartFile);  // then store {@code url} on the owner</pre>
 * Images live in a private bucket and clients load them through the API at /media/{kind}/{ownerId}/{file}, so devices
 * never need direct access to the store. To give a new kind of record images, add its name to {@link #KINDS}.
 */
@Service
public class ImageStorage {
  public record StoredImage(byte[] bytes, String contentType) {}

  /** Image owners. Products keep their original object keys ({id}/{file}); the others are stored under {kind}/{id}/{file}. */
  public static final String PRODUCTS = "products", CATEGORIES = "categories", ADDONS = "addons", STAFF = "staff", OPERATIONS = "operations", CUSTOMERS = "customers";
  public static final Set<String> KINDS = Set.of(PRODUCTS, CATEGORIES, ADDONS, STAFF, OPERATIONS, CUSTOMERS);
  private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
  private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

  private final ObjectStorage storage;
  private final String bucket;

  public ImageStorage(ObjectStorage storage, AppProperties.Storage config) {
    this.storage = storage; this.bucket = config.bucket();
  }

  /** Checks and stores an uploaded image; returns the API-relative URL to save on its owner. */
  public String save(String kind, String ownerId, MultipartFile file) throws Exception {
    String type = file.getContentType();
    if (type == null || !IMAGE_TYPES.contains(type) || file.isEmpty() || file.getSize() > MAX_IMAGE_BYTES)
      throw Validate.bad("Use a JPEG, PNG or WebP image up to 5 MB.");
    String name = UUID.randomUUID() + "." + (type.equals("image/jpeg") ? "jpg" : type.substring(6));
    try (InputStream in = file.getInputStream()) {
      storage.put(bucket, key(kind, ownerId, name), in, file.getSize(), type);
    }
    return "/media/" + kind + "/" + ownerId + "/" + name;
  }

  public Optional<StoredImage> load(String kind, String ownerId, String file) throws Exception {
    return storage.get(bucket, key(kind, ownerId, file)).map(o -> new StoredImage(o.bytes(), o.contentType()));
  }

  private static String key(String kind, String ownerId, String file) {
    return (PRODUCTS.equals(kind) ? "" : kind + "/") + ownerId + "/" + file;
  }
}
