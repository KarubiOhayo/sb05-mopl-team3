package io.mopl.api.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aws.s3")
public class S3Properties {

  private String accessKey;
  private String secretKey;
  private String region;
  private String bucket;
  private String profileImagePath = "profiles/";
  private String thumbnailMoviePath = "thumbnails/movie/";
  private String thumbnailTvSeriesPath = "thumbnails/tv_series/";
  private String thumbnailSportPath = "thumbnails/sport/";
  private long presignedUrlExpirationSeconds = 3600;
}
