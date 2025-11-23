package com.example.userservice.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponseDTO {
  private Long id;
  private String name;
  private String surname;
  private LocalDate birthDate;
  private String email;
  private Boolean active;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
