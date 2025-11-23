package com.example.userservice.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentCardRequestDTO {
  @NotBlank(message = "Card number is mandatory")
  @Size(min = 13, max = 19, message = "Card number must be between 13 and 19 digits")
  @Pattern(regexp = "^[0-9]+$", message = "Card number must contain only digits")
  private String number;

  @NotBlank(message = "Card holder is mandatory")
  @Size(min = 1, max = 255, message = "Card holder name must be between 1 and 255 characters")
  private String holder;

  @NotNull(message = "Expiration date is mandatory")
  @Future(message = "Expiration date must be in the future")
  private LocalDate expirationDate;

  private Boolean active = true;
}
