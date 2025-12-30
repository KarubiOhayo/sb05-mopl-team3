package io.mopl.api.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {

  @NotBlank(message = "{validation.name.required}")
  @Size(min = 2, max = 30, message = "{validation.name.size}")
  private String name;

  @NotBlank(message = "{validation.email.required}")
  @Email(message = "{validation.email.invalid}")
  private String email;

  @NotBlank(message = "{validation.password.required}")
  @Size(min = 8, message = "{validation.password.size}")
  private String password;
}
