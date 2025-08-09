package com.stac.document.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class StorageService {
  @Autowired
  private MinioClient minioClient;

  public String uploadFile(
    String bucketName,
    String objectName,
    InputStream inputStream,
    String contentType) {
    try {
      // check if bucket exists
      boolean found = minioClient
        .bucketExists(
          BucketExistsArgs.builder()
            .bucket(bucketName)
            .build()
        );
      // if no create the bucket
      if (!found) {
        minioClient.makeBucket(
          MakeBucketArgs.builder()
            .bucket(bucketName)
            .build()
        );
      }
      // upload the desired file to the bucket
      minioClient.putObject(
        PutObjectArgs.builder()
          .bucket(bucketName)
          .object(objectName)
          .stream(inputStream, inputStream.available(), -1)
          .contentType(contentType)
          .build()
      );
      return objectName;
    } catch (Exception e) {
      throw new RuntimeException("Failed to upload file: "+e.getMessage());
    }
  }
}
