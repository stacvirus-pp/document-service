package com.stac.document.service;

import com.stac.document.config.MinioProperties;
import com.stac.document.exception.DocumentUploadException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {
  private final MinioClient minioClient;
  private final MinioProperties properties;

  public String uploadFile(MultipartFile file) {
    String bucketName = properties.getBucket().getName();
    checkOrCreateBucket(bucketName);
    return processFile(file, bucketName);
  }

  @Async
  public CompletableFuture<List<String>> uploadFiles(List<MultipartFile> files) {
    if (files == null || files.isEmpty()) {
      throw new DocumentUploadException(HttpStatus.BAD_REQUEST, "Files are empty");
    }
    String bucketName = properties.getBucket().getName();
    checkOrCreateBucket(bucketName);

    return CompletableFuture.supplyAsync(() -> files.parallelStream()
      .map(file -> processFile(file, bucketName))
      .collect(Collectors.toCollection(ArrayList::new))
    );
  }

  private void checkOrCreateBucket(String bucketName) {
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
        log.warn("bucket doesn't exists, creating a new bucket: {}", bucketName);
        minioClient.makeBucket(
          MakeBucketArgs.builder()
            .bucket(bucketName)
            .build()
        );
        log.info("creating a new bucket successful: {}", bucketName);

        String policy = """
          {
              "Version": "2012-10-17",
              "Statement": [
                  {
                      "Effect": "Allow",
                      "Principal": "*",
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                  }
              ]
          }
          """.formatted(bucketName);
        minioClient.setBucketPolicy(
          SetBucketPolicyArgs.builder()
            .bucket(bucketName)
            .config(policy)
            .build()
        );
        log.info("Public read policy set for bucket: {}", bucketName);
      } else {
        log.info("bucket {} already exists", bucketName);
      }
    } catch (Exception e) {
      log.error("error occurred when creating bucket or setting policy: {}", e.getMessage());
      throw new DocumentUploadException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to check or create bucket: " + e.getMessage());
    }
  }

  private String processFile(MultipartFile file, String bucketName) {
    validateFile(file);

    try {
      String originalFilename = file.getOriginalFilename();
      String objectName = formatObjectName(originalFilename);
      log.info("Uploading file: {}", originalFilename);

      try (InputStream inputStream = file.getInputStream()) {
        String contentType = file.getContentType();
        minioClient.putObject(
          PutObjectArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            .stream(inputStream, file.getSize(), -1)
            .contentType(contentType)
            .build()
        );
      }
      log.info("Upload successful: {}", objectName);
      return formatLink(objectName);
    } catch (Exception e) {
      log.error("Error uploading file '{}': {}", file.getOriginalFilename(), e.getMessage());
      throw new DocumentUploadException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file: " + e.getMessage());
    }
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      log.warn("Invalid file detected: file is null or empty");
      throw new DocumentUploadException(HttpStatus.BAD_REQUEST, "File is empty or null");
    }
    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null || originalFilename.trim().isEmpty()) {
      log.warn("Invalid file detected: filename is null or empty");
      throw new DocumentUploadException(HttpStatus.BAD_REQUEST, "File name is null or empty");
    }
  }

  private String formatLink(String savedObjectName) {
    return String.format("%s/%s/%s", properties.getUrl(), properties.getBucket().getName(), savedObjectName);
  }

  private String formatObjectName(String objectName) {
    return objectName.replace(" ", "_");
  }
}
