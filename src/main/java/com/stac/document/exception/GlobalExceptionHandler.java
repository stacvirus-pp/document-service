package com.stac.document.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(DocumentUploadException.class)
  public @ResponseBody ErrorResponse handleDocumentUploadException(DocumentUploadException ex) {
    return  new ErrorResponse(ex.getStatusCode().value(), ex.getMessage());
  }
}
