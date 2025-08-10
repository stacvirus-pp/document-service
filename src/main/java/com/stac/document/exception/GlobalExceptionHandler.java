package com.stac.document.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(DocumentUploadException.class)
  public ResponseEntity<ErrorResponse> handleDocumentUploadException(DocumentUploadException ex) {
    ErrorResponse errorResponse = new ErrorResponse(ex.getStatusCode().value(), ex.getMessage());
    return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
  }
}
