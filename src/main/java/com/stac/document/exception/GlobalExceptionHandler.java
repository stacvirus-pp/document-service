package com.stac.document.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(DocumentUploadException.class)
  public ResponseEntity<ErrorResponse> handleDocumentUploadException(DocumentUploadException ex) {
    log.error("document upload exception error occurred: {}", ex.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(ex.getStatusCode().value(), ex.getMessage());
    return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
    log.error("illegal args error occurred: {}", ex.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    return ResponseEntity.badRequest().body(errorResponse);
  }

  @ExceptionHandler(MissingServletRequestPartException.class)
  public ResponseEntity<ErrorResponse> handleIMissingRequestPartException(MissingServletRequestPartException ex) {
    log.error("missing request args error occurred: {}", ex.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    return ResponseEntity.badRequest().body(errorResponse);
  }
}
