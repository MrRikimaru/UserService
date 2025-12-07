package com.example.userservice.controller;

import com.example.userservice.dto.PaymentCardRequestDTO;
import com.example.userservice.dto.PaymentCardResponseDTO;
import com.example.userservice.service.PaymentCardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/payment-cards")
@RequiredArgsConstructor
@Validated
public class PaymentCardController {
  private final PaymentCardService paymentCardService;

  @PostMapping("/user/{userId}")
  public ResponseEntity<PaymentCardResponseDTO> createCard(
      @PathVariable @Positive(message = "User ID must be positive") Long userId,
      @Valid @RequestBody PaymentCardRequestDTO cardRequestDTO) {
    PaymentCardResponseDTO createdCard = paymentCardService.createCard(cardRequestDTO, userId);
    return new ResponseEntity<>(createdCard, HttpStatus.CREATED);
  }

  @GetMapping("/{id}")
  public ResponseEntity<PaymentCardResponseDTO> getCardById(
      @PathVariable @Positive(message = "Card ID must be positive") Long id) {
    PaymentCardResponseDTO card = paymentCardService.getCardById(id);
    return ResponseEntity.ok(card);
  }

  @GetMapping
  public ResponseEntity<Page<PaymentCardResponseDTO>> getAllCards(
      @RequestParam(required = false) String holder,
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) Long userId,
      Pageable pageable) {
    Page<PaymentCardResponseDTO> cards =
        paymentCardService.getAllCards(holder, active, userId, pageable);
    return ResponseEntity.ok(cards);
  }

  @GetMapping("/active")
  public ResponseEntity<Page<PaymentCardResponseDTO>> getActiveCards(Pageable pageable) {
    Page<PaymentCardResponseDTO> cards = paymentCardService.getActiveCards(pageable);
    return ResponseEntity.ok(cards);
  }

  @GetMapping("/user/{userId}")
  public ResponseEntity<Page<PaymentCardResponseDTO>> getAllCardsByUserId(
      @PathVariable @Positive(message = "User ID must be positive") Long userId,
      Pageable pageable) {
    Page<PaymentCardResponseDTO> cards = paymentCardService.getAllCardsByUserId(userId, pageable);
    return ResponseEntity.ok(cards);
  }

  @GetMapping("/user/{userId}/active")
  public ResponseEntity<Page<PaymentCardResponseDTO>> getActiveCardsByUserId(
      @PathVariable @Positive(message = "User ID must be positive") Long userId,
      Pageable pageable) {
    Page<PaymentCardResponseDTO> cards =
        paymentCardService.getActiveCardsByUserId(userId, pageable);
    return ResponseEntity.ok(cards);
  }

  @GetMapping("/user/{userId}/card/{cardId}")
  public ResponseEntity<PaymentCardResponseDTO> getCardByUserAndId(
      @PathVariable @Positive(message = "User ID must be positive") Long userId,
      @PathVariable @Positive(message = "Card ID must be positive") Long cardId) {
    PaymentCardResponseDTO card = paymentCardService.getCardByUserAndId(userId, cardId);
    return ResponseEntity.ok(card);
  }

  @GetMapping("/number/{number}")
  public ResponseEntity<PaymentCardResponseDTO> getCardByNumber(@PathVariable String number) {
    PaymentCardResponseDTO card = paymentCardService.getCardByNumber(number);
    return ResponseEntity.ok(card);
  }

  @PutMapping("/{id}")
  public ResponseEntity<PaymentCardResponseDTO> updateCard(
      @PathVariable @Positive(message = "Card ID must be positive") Long id,
      @Valid @RequestBody PaymentCardRequestDTO cardRequestDTO) {
    PaymentCardResponseDTO updatedCard = paymentCardService.updateCard(id, cardRequestDTO);
    return ResponseEntity.ok(updatedCard);
  }

  @PatchMapping("/{id}/activate")
  public ResponseEntity<Void> activateCard(
      @PathVariable @Positive(message = "Card ID must be positive") Long id) {
    paymentCardService.activateCard(id);
    return ResponseEntity.ok().build();
  }

  @PatchMapping("/{id}/deactivate")
  public ResponseEntity<Void> deactivateCard(
      @PathVariable @Positive(message = "Card ID must be positive") Long id) {
    paymentCardService.deactivateCard(id);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCard(
      @PathVariable @Positive(message = "Card ID must be positive") Long id) {
    paymentCardService.deleteCard(id);
    return ResponseEntity.noContent().build();
  }
}
