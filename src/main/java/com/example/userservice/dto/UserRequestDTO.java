package com.example.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequestDTO {
  @NotBlank(message = "Name is mandatory")
  @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
  private String name;

  @NotBlank(message = "Surname is mandatory")
  @Size(min = 1, max = 255, message = "Surname must be between 1 and 255 characters")
  private String surname;

  @Past(message = "Birth date must be in the past")
  private LocalDate birthDate;

  @NotBlank(message = "Email is mandatory")
  @Email(message = "Email should be valid")
  @Size(max = 255, message = "Email must not exceed 255 characters")
  private String email;

  private Boolean active = true;
}
