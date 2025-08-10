package com.stac.document.service;

import com.stac.document.config.MinioProperties;
import com.stac.document.exception.DocumentUploadException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {
  private final MinioClient minioClient;
  private final MinioProperties properties;

  public String uploadFile(
    MultipartFile file
  ) {
    if (file.isEmpty())
      throw new DocumentUploadException(HttpStatus.BAD_REQUEST, "File is empty");
    try {
      log.info("uploading a new file: {}", file.getOriginalFilename());
      String objectName = formatObjectName(Objects.requireNonNull(file.getOriginalFilename()));
      String bucketName = properties.getBucket().getName();

      checkOrCreateBucket(bucketName);
      InputStream inputStream = file.getInputStream();
      String contentType = file.getContentType();
      // upload the desired file to the bucket
      minioClient.putObject(
        PutObjectArgs.builder()
          .bucket(bucketName)
          .object(objectName)
          .stream(inputStream, inputStream.available(), -1)
          .contentType(contentType)
          .build()
      );
      log.info("upload successful: {}", objectName);
      return formatLink(objectName);
    } catch (Exception e) {
      log.error("Error occurred when uploading file: {}", e.getMessage());
      throw new DocumentUploadException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file: " + e.getMessage());
    }
  }

  public List<String> uploadFiles(
    List<MultipartFile> files
  ) {
    if (files == null || files.isEmpty())
      throw new DocumentUploadException(HttpStatus.BAD_REQUEST, "Files are empty");
    try {
      String bucketName = properties.getBucket().getName();
      checkOrCreateBucket(bucketName);
      return files.stream()
        .map(file -> {
            try {
              log.info("Uploading file: {}", file.getOriginalFilename());
              String objectName = formatObjectName(Objects.requireNonNull(file.getOriginalFilename()));
              InputStream inputStream = file.getInputStream();
              String contentType = file.getContentType();
              minioClient.putObject(
                PutObjectArgs.builder()
                  .bucket(bucketName)
                  .object(objectName)
                  .stream(inputStream, inputStream.available(), -1)
                  .contentType(contentType)
                  .build()
              );
              log.info("Upload successful: {}", objectName);
              return formatLink(objectName);
            } catch (Exception e) {
              log.error("error occurred when uploading file: {}", e.getMessage());
              throw new DocumentUploadException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file: " + e.getMessage());
            }
          }
        )
        .collect(Collectors.toCollection(ArrayList::new));
    } catch (Exception e) {
      log.error("error occurred when uploading files: {}", e.getMessage());
      throw new DocumentUploadException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload files: " + e.getMessage());
    }
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
        log.warn("creating a new bucket: {}", bucketName);
        minioClient.makeBucket(
          MakeBucketArgs.builder()
            .bucket(bucketName)
            .build()
        );
      } else {
        log.info("bucket {} already exists", bucketName);
      }
    } catch (Exception e) {
      log.error("error occurred when creating bucket: {}", e.getMessage());
      throw new DocumentUploadException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to check or create bucket: " + e.getMessage());
    }
  }

  private String formatLink(String savedObjectName) {
    return String.format("%s/%s/%s", properties.getUrl(), properties.getBucket().getName(), savedObjectName);
  }

  private String formatObjectName(String objectName) {
    return objectName.replace(" ", "_");
  }
}
