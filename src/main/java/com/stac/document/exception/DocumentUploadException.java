package com.stac.document.exception;

import org.springframework.http.HttpStatusCode;

public class DocumentUploadException extends RuntimeException {
  private final HttpStatusCode statusCode;

  public DocumentUploadException(HttpStatusCode statusCode, String message) {
    super(message);
    this.statusCode = statusCode;
  }

  public HttpStatusCode getStatusCode() {
    return statusCode;
  }
}
