package io.mopl.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignInRequest {

  @NotBlank(message = "{validation.email.required}")
  @Email(message = "{validation.email.invalid}")
  private String username;

  @NotBlank(message = "{validation.password.required}")
  private String password;
}
