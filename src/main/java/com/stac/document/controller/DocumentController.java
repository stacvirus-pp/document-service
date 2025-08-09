package com.stac.document.controller;

import com.stac.document.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/documents")
public class DocumentController {
  private final StorageService service;

  @Value("${minio.bucket.name}")
  private String bucketName;
  @Value("${minio.url}")
  private String minioUrl;

  @PostMapping("/upload")
  public ResponseEntity<String> uploadDocument(
    @RequestParam("file")MultipartFile file
    ) {
    try {
      if(file.isEmpty()){
        return ResponseEntity.badRequest().body("File is empty");
      }
      String objectName = file.getOriginalFilename();
      String savedObjectName = service.uploadFile(bucketName, objectName, file.getInputStream(), file.getContentType());
      String minioFileUrl = String.format("%s/%s/%s",minioUrl, bucketName, savedObjectName);
      return ResponseEntity.ok(minioFileUrl);
    } catch (IOException e) {
      return ResponseEntity.status(500).body("Failed to upload file: "+e.getMessage());
    }
  }
}
