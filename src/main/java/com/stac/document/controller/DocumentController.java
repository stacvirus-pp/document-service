package com.stac.document.controller;

import com.stac.document.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/documents")
public class DocumentController {
  private final StorageService service;

  @PostMapping("/upload")
  public ResponseEntity<String> uploadDocument(
    @RequestParam("file")MultipartFile file
    ) {
    return ResponseEntity.ok(service.uploadFile(file));
  }

  @PostMapping("/batch/upload")
  public CompletableFuture<ResponseEntity<List<String>>> uploadDocuments(
    @RequestParam("files") List<MultipartFile> files
    ) {
    return service.uploadFiles(files)
      .thenApply(ResponseEntity::ok);
  }
}
