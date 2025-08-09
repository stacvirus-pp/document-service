package com.stac.document.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@ConfigurationProperties(prefix = "minio")
@Configuration
public class MinioProperties {
  private String url;
  private Access access;
  private Bucket bucket;

  @Data
  public static class Access {
    private String name;
    private String secret;
  }

  @Data
  public static class Bucket {
    private String name;
  }
}
