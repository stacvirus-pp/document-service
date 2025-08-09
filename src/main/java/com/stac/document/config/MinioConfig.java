package com.stac.document.config;

import io.minio.MinioClient;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class MinioConfig {
  private final MinioProperties properties;

  @Bean
  public MinioClient minioClient() {
    return MinioClient.builder()
      .endpoint(properties.getUrl())
      .credentials(properties.getAccess().getName(), properties.getAccess().getSecret())
      .build();
  }
}
