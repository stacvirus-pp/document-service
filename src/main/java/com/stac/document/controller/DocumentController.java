package com.stac.document.controller;

import com.stac.document.config.MinioProperties;
import com.stac.document.service.StorageService;
import lombok.RequiredArgsConstructor;
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
  private final MinioProperties properties;

  @PostMapping("/upload")
  public ResponseEntity<String> uploadDocument(
    @RequestParam("file")MultipartFile file
    ) {
    try {
      if(file.isEmpty()){
        return ResponseEntity.badRequest().body("File is empty");
      }
      String objectName = file.getOriginalFilename();
      String savedObjectName = service.uploadFile(properties.getBucket().getName(), objectName, file.getInputStream(), file.getContentType());
      String minioFileUrl = String.format("%s/%s/%s",properties.getUrl(), properties.getBucket().getName(), savedObjectName);
      return ResponseEntity.ok(minioFileUrl);
    } catch (IOException e) {
      return ResponseEntity.status(500).body("Failed to upload file: "+e.getMessage());
    }
  }
}
