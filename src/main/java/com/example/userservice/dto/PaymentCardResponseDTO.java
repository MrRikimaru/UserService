package com.example.userservice.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentCardResponseDTO {
  private Long id;
  private Long userId;
  private String number;
  private String holder;
  private LocalDate expirationDate;
  private Boolean active;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
