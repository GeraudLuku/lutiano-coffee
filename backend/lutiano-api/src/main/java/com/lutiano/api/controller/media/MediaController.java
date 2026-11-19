package com.lutiano.api.controller.media;

import java.time.Duration;
import java.util.regex.Pattern;
import com.lutiano.api.utils.Validate;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import com.lutiano.api.proxy.ImageStorage;

/** Public read access to catalogue and profile images; file names are random, so they cannot be guessed. */
@RestController
public class MediaController {
  private static final Pattern ID_RE = Pattern.compile("^[0-9a-fA-F-]{36}$");
  private static final Pattern FILE_RE = Pattern.compile("^[0-9a-f-]{36}\\.(jpg|png|webp)$");
  private final ImageStorage storage;

  public MediaController(ImageStorage storage) { this.storage = storage; }

  @GetMapping("/media/{kind}/{ownerId}/{file}")
  public ResponseEntity<byte[]> image(@PathVariable String kind, @PathVariable String ownerId, @PathVariable String file) throws Exception {
    if (!ImageStorage.KINDS.contains(kind) || !ID_RE.matcher(ownerId).matches() || !FILE_RE.matcher(file).matches()) throw Validate.notFound("Image not found.");
    ImageStorage.StoredImage img = storage.load(kind, ownerId, file).orElseThrow(() -> Validate.notFound("Image not found."));
    return ResponseEntity.ok()
        .contentType(img.contentType() != null ? MediaType.parseMediaType(img.contentType()) : MediaType.APPLICATION_OCTET_STREAM)
        .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
        .body(img.bytes());
  }
}
