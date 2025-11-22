package com.example.userservice.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserWithCardsResponseDTO {
  private Long id;
  private String name;
  private String surname;
  private LocalDate birthDate;
  private String email;
  private Boolean active;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<PaymentCardResponseDTO> paymentCards;
}
