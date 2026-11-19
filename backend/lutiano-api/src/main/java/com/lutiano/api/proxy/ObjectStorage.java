package com.lutiano.api.proxy;

import java.io.InputStream;
import java.util.Optional;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.springframework.stereotype.Service;
import com.lutiano.api.config.AppProperties;

/** Thin client for any S3-compatible store (the MinIO SDK is a generic S3 client). Buckets are private. */
@Service
public class ObjectStorage {
  public record StoredObject(byte[] bytes, String contentType) {}

  private final MinioClient client;

  public ObjectStorage(AppProperties.Storage config) {
    // A fixed region avoids a GetBucketLocation call that some S3-compatible stores do not support.
    this.client = MinioClient.builder().endpoint(config.endpoint()).region(config.region()).credentials(config.accessKey(), config.secretKey()).build();
  }

  public void put(String bucket, String key, InputStream data, long size, String contentType) throws Exception {
    if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))
      client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
    client.putObject(PutObjectArgs.builder().bucket(bucket).object(key).stream(data, size, -1).contentType(contentType).build());
  }

  public Optional<StoredObject> get(String bucket, String key) throws Exception {
    try (GetObjectResponse obj = client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
      return Optional.of(new StoredObject(obj.readAllBytes(), obj.headers().get("Content-Type")));
    } catch (ErrorResponseException e) {
      return Optional.empty();
    }
  }
}
