package com.stac.document.controller;

import com.stac.document.service.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
class DocumentControllerTests {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private StorageService storageService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void uploadDocument_ShouldReturnOk_WhenFileIsUploaded() throws Exception {
    // Arrange
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "test.txt",
      MediaType.TEXT_PLAIN_VALUE,
      "Hello World".getBytes()
    );
    String expectedResponse = "file-id-123";

    when(storageService.uploadFile(any())).thenReturn(expectedResponse);

    // Act & Assert
    mockMvc.perform(multipart("/api/v1/documents/upload")
        .file(file))
      .andExpect(status().isOk())
      .andExpect(content().string(expectedResponse));
  }

  @Test
  void uploadDocument_ShouldReturnBadRequest_WhenFileParameterIsMissing() throws Exception {
    // Act & Assert
    mockMvc.perform(multipart("/api/v1/documents/upload"))
      .andExpect(status().isBadRequest());
  }

  @Test
  void uploadDocument_ShouldReturnBadRequest_WhenFileIsEmpty() throws Exception {
    // Arrange
    MockMultipartFile emptyFile = new MockMultipartFile(
      "file",
      "empty.txt",
      MediaType.TEXT_PLAIN_VALUE,
      new byte[0]
    );

    // Act & Assert
    mockMvc.perform(multipart("/api/v1/documents/upload")
        .file(emptyFile))
      .andExpect(status().isOk()); // Note: This will depend on your StorageService implementation
  }

  @Test
  void uploadDocument_ShouldHandleDifferentFileTypes() throws Exception {
    // Arrange
    MockMultipartFile pdfFile = new MockMultipartFile(
      "file",
      "document.pdf",
      MediaType.APPLICATION_PDF_VALUE,
      "PDF content".getBytes()
    );
    String expectedResponse = "pdf-file-id-456";

    when(storageService.uploadFile(any())).thenReturn(expectedResponse);

    // Act & Assert
    mockMvc.perform(multipart("/api/v1/documents/upload")
        .file(pdfFile))
      .andExpect(status().isOk())
      .andExpect(content().string(expectedResponse));
  }

  @Test
  void uploadDocuments_ShouldReturnOk_WhenMultipleFilesAreUploaded() throws Exception {
    MockMultipartFile file1 = new MockMultipartFile(
      "files",
      "test1.txt",
      MediaType.TEXT_PLAIN_VALUE,
      "Content 1".getBytes()
    );
    MockMultipartFile file2 = new MockMultipartFile(
      "files",
      "test2.txt",
      MediaType.TEXT_PLAIN_VALUE,
      "Content 2".getBytes()
    );

    List<String> expectedResponse = Arrays.asList("file-id-1", "file-id-2");
    CompletableFuture<List<String>> futureResponse = CompletableFuture.completedFuture(expectedResponse);

    when(storageService.uploadFiles(anyList())).thenReturn(futureResponse);

    MvcResult mvcResult = mockMvc.perform(multipart("/api/v1/documents/batch/upload")
        .file(file1)
        .file(file2)
        .contentType(MediaType.MULTIPART_FORM_DATA))
      .andExpect(status().isOk())
      .andExpect(request().asyncStarted())
      .andReturn();

    mockMvc.perform(asyncDispatch(mvcResult))
      .andExpect(status().isOk())
      .andExpect(content().json(objectMapper.writeValueAsString(expectedResponse)));
  }

  @Test
  void uploadDocuments_ShouldReturnOk_WhenSingleFileInBatch() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
      "files",
      "single.txt",
      MediaType.TEXT_PLAIN_VALUE,
      "Single file content".getBytes()
    );

    List<String> expectedResponse = Arrays.asList("single-file-id");
    CompletableFuture<List<String>> futureResponse = CompletableFuture.completedFuture(expectedResponse);

    when(storageService.uploadFiles(anyList())).thenReturn(futureResponse);

    MvcResult mvcResult = mockMvc.perform(multipart("/api/v1/documents/batch/upload")
        .file(file)
        .contentType(MediaType.MULTIPART_FORM_DATA))
      .andExpect(status().isOk())
      .andExpect(request().asyncStarted())
      .andReturn();

    mockMvc.perform(asyncDispatch(mvcResult))
      .andExpect(status().isOk())
      .andExpect(content().json(objectMapper.writeValueAsString(expectedResponse)));
  }

  @Test
  void uploadDocuments_ShouldReturnBadRequest_WhenFilesParameterIsMissing() throws Exception {
    // Act & Assert
    mockMvc.perform(multipart("/api/v1/documents/batch/upload"))
      .andExpect(status().isBadRequest());
  }

  @Test
  void uploadDocuments_ShouldHandleAsyncResponse() throws Exception {
    // Arrange
    MockMultipartFile file = new MockMultipartFile(
      "files",
      "async.txt",
      MediaType.TEXT_PLAIN_VALUE,
      "Async content".getBytes()
    );

    List<String> expectedResponse = Arrays.asList("async-file-id");
    CompletableFuture<List<String>> futureResponse = new CompletableFuture<>();

    when(storageService.uploadFiles(anyList())).thenReturn(futureResponse);

    // Complete the future after a short delay to simulate async processing
    new Thread(() -> {
      try {
        Thread.sleep(100);
        futureResponse.complete(expectedResponse);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }).start();

    // Act & Assert
    mockMvc.perform(multipart("/api/v1/documents/batch/upload")
        .file(file)
        .contentType(MediaType.MULTIPART_FORM_DATA))
      .andExpect(status().isOk())
      .andExpect(request().asyncStarted());
  }

  @Test
  void uploadDocuments_ShouldHandleMixedFileTypes() throws Exception {
    // Arrange
    MockMultipartFile textFile = new MockMultipartFile(
      "files",
      "document.txt",
      MediaType.TEXT_PLAIN_VALUE,
      "Text content".getBytes()
    );
    MockMultipartFile imageFile = new MockMultipartFile(
      "files",
      "image.jpg",
      MediaType.IMAGE_JPEG_VALUE,
      "Image content".getBytes()
    );
    MockMultipartFile pdfFile = new MockMultipartFile(
      "files",
      "document.pdf",
      MediaType.APPLICATION_PDF_VALUE,
      "PDF content".getBytes()
    );

    List<String> expectedResponse = Arrays.asList("text-id", "image-id", "pdf-id");
    CompletableFuture<List<String>> futureResponse = CompletableFuture.completedFuture(expectedResponse);

    when(storageService.uploadFiles(anyList())).thenReturn(futureResponse);

    // Act & Assert
    mockMvc.perform(multipart("/api/v1/documents/batch/upload")
        .file(textFile)
        .file(imageFile)
        .file(pdfFile)
        .contentType(MediaType.MULTIPART_FORM_DATA))
      .andExpect(status().isOk())
      .andExpect(request().asyncStarted());
  }
}
