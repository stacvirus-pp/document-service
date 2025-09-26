package com.stac.document.service;

import com.stac.document.config.MinioProperties;
import com.stac.document.exception.DocumentUploadException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class StorageServiceTests {
  @Mock
  private MinioClient minioClient;

  @Mock
  private MinioProperties minioProperties;

  @Mock
  private MultipartFile multipartFile;

  @InjectMocks
  private StorageService storageService;

  private MinioProperties.Bucket bucket;

  @BeforeEach
  void setUp() {
    bucket = new MinioProperties.Bucket();
    bucket.setName("test-bucket");
  }

  @Test
  void uploadFile_SuccessfulUpload_ReturnsFormattedLink() throws Exception {
    // Arrange
    String fileName = "test file.txt";
    String expectedLink = "http://minio-server/test-bucket/test_file.txt";
    InputStream inputStream = new ByteArrayInputStream("test content".getBytes());

    when(minioProperties.getBucket()).thenReturn(bucket);
    when(minioProperties.getUrl()).thenReturn("http://minio-server");
    when(multipartFile.isEmpty()).thenReturn(false);
    when(multipartFile.getOriginalFilename()).thenReturn(fileName);
    when(multipartFile.getInputStream()).thenReturn(inputStream);
    when(multipartFile.getContentType()).thenReturn("text/plain");
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

    // Act
    String result = storageService.uploadFile(multipartFile);

    // Assert
    verify(minioClient).putObject(any(PutObjectArgs.class));
    assertEquals(expectedLink, result);
  }

  @Test
  void uploadFile_NoFileName_ThrowsDocumentUploadException() {
    // Arrange
    when(multipartFile.getOriginalFilename()).thenReturn("");
    when(multipartFile.isEmpty()).thenReturn(false);
    when(minioProperties.getBucket()).thenReturn(bucket);

    // Act & Assert
    DocumentUploadException exception = assertThrows(DocumentUploadException.class,
      () -> storageService.uploadFile(multipartFile));
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertTrue(exception.getMessage().contains("File name is null or empty"));
  }

  @Test
  void uploadFile_EmptyFile_ThrowsDocumentUploadException() {
    // Arrange
    when(multipartFile.isEmpty()).thenReturn(true);
    when(minioProperties.getBucket()).thenReturn(bucket);

    // Act & Assert
    DocumentUploadException exception = assertThrows(DocumentUploadException.class,
      () -> storageService.uploadFile(multipartFile));
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertTrue(exception.getMessage().contains("File is empty"));
  }

  @Test
  void uploadFile_MinioError_ThrowsDocumentUploadException() throws Exception {
    // Arrange
    when(minioProperties.getBucket()).thenReturn(bucket);
    when(multipartFile.isEmpty()).thenReturn(false);
    when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
    when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
//    doThrow(new RuntimeException("Minio error")).when(minioClient).putObject(any(PutObjectArgs.class));

    // Act & Assert
    DocumentUploadException exception = assertThrows(DocumentUploadException.class,
      () -> storageService.uploadFile(multipartFile));
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    assertTrue(exception.getMessage().contains("Failed to upload file"));
  }

  @Test
  void uploadFiles_SuccessfulUpload_ReturnsListOfLinks() throws Exception {
    // Arrange
    String fileName1 = "test1.txt";
    String fileName2 = "test2.txt";
    List<MultipartFile> files = Arrays.asList(multipartFile, multipartFile);

    when(minioProperties.getBucket()).thenReturn(bucket);
    when(multipartFile.getOriginalFilename()).thenReturn(fileName1).thenReturn(fileName2);
    when(multipartFile.getInputStream())
      .thenReturn(new ByteArrayInputStream("test1".getBytes()))
      .thenReturn(new ByteArrayInputStream("test2".getBytes()));
    when(multipartFile.getContentType()).thenReturn("text/plain");
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

    // Act
    CompletableFuture<List<String>> resultFuture = storageService.uploadFiles(files);
    List<String> result = resultFuture.join();

    // Assert
    verify(minioClient, times(2)).putObject(any());
    assertEquals(2, result.size());
  }

  @Test
  void uploadFiles_failedUpload_ReturnsErrorMessage() throws Exception {
    // Arrange
    String fileName1 = "test1.txt";
    String fileName2 = "test2.txt";
    List<MultipartFile> files = Arrays.asList(multipartFile, multipartFile);

    when(minioProperties.getBucket()).thenReturn(bucket);
    when(multipartFile.getOriginalFilename()).thenReturn(fileName1).thenReturn(fileName2);
    when(multipartFile.getInputStream())
      .thenReturn(new ByteArrayInputStream("test1".getBytes()))
      .thenReturn(new ByteArrayInputStream("test2".getBytes()));
    when(multipartFile.getContentType()).thenReturn("text/plain");
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    doThrow(new RuntimeException("Minio put object failed")).when(minioClient).putObject(any(PutObjectArgs.class));

    // Act
    CompletableFuture<List<String>> resultFuture = storageService.uploadFiles(files);
    CompletionException completionException = assertThrows(CompletionException.class, resultFuture::join);
    Throwable cause = completionException.getCause();

    // Assert
    assertTrue(cause instanceof DocumentUploadException, "Cause should be DocumentUploadException");
    DocumentUploadException exception = (DocumentUploadException) cause;
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    assertTrue(exception.getMessage().contains("Failed to upload file"));
  }

  @Test
  void uploadFiles_EmptyList_ThrowsDocumentUploadException() {
    // Arrange
    List<MultipartFile> files = Arrays.asList();

    // Act & Assert
    DocumentUploadException exception = assertThrows(DocumentUploadException.class,
      () -> storageService.uploadFiles(files));
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertTrue(exception.getMessage().contains("Files are empty"));
  }

  @Test
  void checkOrCreateBucket_BucketDoesNotExist_CreatesNewBucket() throws Exception {
    // Arrange
    String fileName = "test file.txt";
    String expectedLink = "http://minio-server/test-bucket/test_file.txt";
    InputStream inputStream = new ByteArrayInputStream("test content".getBytes());

    when(minioProperties.getBucket()).thenReturn(bucket);
    when(minioProperties.getUrl()).thenReturn("http://minio-server");
    when(multipartFile.isEmpty()).thenReturn(false);
    when(multipartFile.getOriginalFilename()).thenReturn(fileName);
    when(multipartFile.getInputStream()).thenReturn(inputStream);
    when(multipartFile.getContentType()).thenReturn("text/plain");
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

    // Act
    storageService.uploadFile(multipartFile);

    // Assert
    verify(minioClient).makeBucket(any(MakeBucketArgs.class));
  }

  @Test
  void checkOrCreateBucket_BucketExists_DoesNotCreateBucket() throws Exception {
    // Arrange
    when(minioProperties.getBucket()).thenReturn(bucket);
    when(minioProperties.getUrl()).thenReturn("http://minio-server");
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    when(multipartFile.isEmpty()).thenReturn(false);
    when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
    when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
    when(multipartFile.getContentType()).thenReturn("text/plain");

    // Act
    storageService.uploadFile(multipartFile);

    // Assert
    verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
  }

  @Test
  void checkOrCreateBucket_BucketExists_DoesNotCreateBucket_fail_with_minioException() throws Exception {
    // Arrange
    when(minioProperties.getBucket()).thenReturn(bucket);
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
    doThrow(new RuntimeException("Minio bucket creation failed")).when(minioClient).makeBucket(any(MakeBucketArgs.class));

    // Act
    DocumentUploadException exception = assertThrows(DocumentUploadException.class,
      () -> storageService.uploadFile(multipartFile));

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    assertTrue(exception.getMessage().contains("Failed to check or create bucket"));
  }
}