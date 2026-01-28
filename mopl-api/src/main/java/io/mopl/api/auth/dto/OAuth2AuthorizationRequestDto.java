package io.mopl.api.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuth2AuthorizationRequestDto implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("authorizationUri")
  private String authorizationUri;

  @JsonProperty("authorizationGrantType")
  private String authorizationGrantType;

  @JsonProperty("responseType")
  private String responseType;

  @JsonProperty("clientId")
  private String clientId;

  @JsonProperty("redirectUri")
  private String redirectUri;

  @JsonProperty("scopes")
  private Set<String> scopes;

  @JsonProperty("state")
  private String state;

  @JsonProperty("additionalParameters")
  private Map<String, Object> additionalParameters;

  @JsonProperty("authorizationRequestUri")
  private String authorizationRequestUri;

  @JsonProperty("attributes")
  private Map<String, Object> attributes;

  /** OAuth2AuthorizationRequest를 DTO로 변환 */
  public static OAuth2AuthorizationRequestDto from(OAuth2AuthorizationRequest request) {
    if (request == null) {
      return null;
    }

    return OAuth2AuthorizationRequestDto.builder()
        .authorizationUri(request.getAuthorizationUri())
        .authorizationGrantType(
            request.getGrantType() != null ? request.getGrantType().getValue() : null)
        .responseType(
            request.getResponseType() != null ? request.getResponseType().getValue() : null)
        .clientId(request.getClientId())
        .redirectUri(request.getRedirectUri())
        .scopes(request.getScopes())
        .state(request.getState())
        .additionalParameters(request.getAdditionalParameters())
        .authorizationRequestUri(request.getAuthorizationRequestUri())
        .attributes(request.getAttributes())
        .build();
  }

  /** DTO를 OAuth2AuthorizationRequest로 변환 */
  public OAuth2AuthorizationRequest toOAuth2AuthorizationRequest() {
    OAuth2AuthorizationRequest.Builder builder =
        OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri(this.authorizationUri)
            .clientId(this.clientId)
            .redirectUri(this.redirectUri)
            .state(this.state);

    // scopes 설정
    if (this.scopes != null && !this.scopes.isEmpty()) {
      builder.scopes(this.scopes);
    }

    // additionalParameters 설정
    if (this.additionalParameters != null && !this.additionalParameters.isEmpty()) {
      builder.additionalParameters(this.additionalParameters);
    }

    // attributes 설정
    if (this.attributes != null && !this.attributes.isEmpty()) {
      builder.attributes(this.attributes);
    }

    // authorizationRequestUri 설정
    if (this.authorizationRequestUri != null) {
      builder.authorizationRequestUri(this.authorizationRequestUri);
    }

    return builder.build();
  }
}
