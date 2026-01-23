package io.mopl.batch.s3;

import io.mopl.batch.common.BatchErrorCode;
import io.mopl.core.error.BusinessException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
@RequiredArgsConstructor
public class S3CleanupService {

  private static final int DELETE_BATCH_SIZE = 1000;

  private final S3Client s3Client;
  private final S3Properties s3Properties;

  public int deleteByPrefix(String prefix) {
    String bucket = requireBucket();
    String continuationToken = null;
    int deleted = 0;

    do {
      ListObjectsV2Request request =
          ListObjectsV2Request.builder()
              .bucket(bucket)
              .prefix(prefix)
              .continuationToken(continuationToken)
              .build();
      ListObjectsV2Response response = s3Client.listObjectsV2(request);
      List<ObjectIdentifier> keys = new ArrayList<>();
      for (S3Object object : response.contents()) {
        keys.add(ObjectIdentifier.builder().key(object.key()).build());
      }
      if (!keys.isEmpty()) {
        deleteObjects(bucket, keys);
        deleted += keys.size();
      }
      continuationToken = response.nextContinuationToken();
    } while (continuationToken != null);

    return deleted;
  }

  public int deleteByKeys(List<String> keys) {
    String bucket = requireBucket();
    if (keys == null || keys.isEmpty()) {
      return 0;
    }
    int deleted = 0;
    List<ObjectIdentifier> batch = new ArrayList<>(DELETE_BATCH_SIZE);
    for (String key : keys) {
      if (key == null || key.isBlank()) {
        continue;
      }
      batch.add(ObjectIdentifier.builder().key(key).build());
      if (batch.size() == DELETE_BATCH_SIZE) {
        deleteObjects(bucket, batch);
        deleted += batch.size();
        batch.clear();
      }
    }
    if (!batch.isEmpty()) {
      deleteObjects(bucket, batch);
      deleted += batch.size();
    }
    return deleted;
  }

  private void deleteObjects(String bucket, List<ObjectIdentifier> keys) {
    DeleteObjectsRequest request =
        DeleteObjectsRequest.builder()
            .bucket(bucket)
            .delete(Delete.builder().objects(keys).build())
            .build();
    s3Client.deleteObjects(request);
  }

  private String requireBucket() {
    if (s3Properties.bucket() == null || s3Properties.bucket().isBlank()) {
      throw new BusinessException(BatchErrorCode.S3_BUCKET_NOT_CONFIGURED);
    }
    return s3Properties.bucket();
  }
}
