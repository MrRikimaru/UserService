package com.example.userservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.example.userservice.dto.PaymentCardRequestDTO;
import com.example.userservice.dto.PaymentCardResponseDTO;
import com.example.userservice.dto.UserRequestDTO;
import com.example.userservice.dto.UserResponseDTO;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PaymentCardControllerIntegrationTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  private Long userId;

  @BeforeEach
  void setUp() {
    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    String uniqueEmail = "card.owner." + uniqueId + "@example.com";

    UserRequestDTO userRequest = new UserRequestDTO();
    userRequest.setName("Card");
    userRequest.setSurname("Owner");
    userRequest.setEmail(uniqueEmail);

    ResponseEntity<UserResponseDTO> userResponse =
        restTemplate.postForEntity("/api/users", userRequest, UserResponseDTO.class);

    assertEquals(HttpStatus.CREATED, userResponse.getStatusCode());
    assertNotNull(userResponse.getBody());
    userId = userResponse.getBody().getId();
  }

  @Test
  void createCard_ShouldReturnCreatedCard_WhenValidInput() {
    // Arrange
    PaymentCardRequestDTO requestDTO = new PaymentCardRequestDTO();
    requestDTO.setNumber("4111111111111111");
    requestDTO.setHolder("CARD HOLDER");
    requestDTO.setExpirationDate(LocalDate.now().plusYears(2));

    // Act
    ResponseEntity<PaymentCardResponseDTO> response =
        restTemplate.postForEntity(
            "/api/payment-cards/user/" + userId, requestDTO, PaymentCardResponseDTO.class);

    // Assert
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("4111111111111111", response.getBody().getNumber());
    assertEquals("CARD HOLDER", response.getBody().getHolder());
    assertEquals(userId, response.getBody().getUserId());
  }

  @Test
  void createCard_ShouldReturnBadRequest_WhenCardLimitExceeded() {
    // Arrange
    for (int i = 0; i < 5; i++) {
      PaymentCardRequestDTO requestDTO = new PaymentCardRequestDTO();
      requestDTO.setNumber(generateUniqueCardNumber(i));
      requestDTO.setHolder("CARD HOLDER " + i);
      requestDTO.setExpirationDate(LocalDate.now().plusYears(2));

      ResponseEntity<PaymentCardResponseDTO> cardResponse =
          restTemplate.postForEntity(
              "/api/payment-cards/user/" + userId, requestDTO, PaymentCardResponseDTO.class);
      assertEquals(HttpStatus.CREATED, cardResponse.getStatusCode());
    }

    PaymentCardRequestDTO sixthCard = new PaymentCardRequestDTO();
    sixthCard.setNumber(generateUniqueCardNumber(6));
    sixthCard.setHolder("SIXTH CARD");
    sixthCard.setExpirationDate(LocalDate.now().plusYears(2));

    // Act
    ResponseEntity<String> response =
        restTemplate.postForEntity("/api/payment-cards/user/" + userId, sixthCard, String.class);

    // Assert
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  private String generateUniqueCardNumber(int index) {
    String baseNumber = "411111111111";
    String suffix = String.format("%04d", index);
    return baseNumber + suffix;
  }

  @Test
  void getCardById_ShouldReturnCard_WhenCardExists() {
    // Arrange
    PaymentCardRequestDTO createRequest = new PaymentCardRequestDTO();
    createRequest.setNumber("5111111111111111");
    createRequest.setHolder("TEST CARD");
    createRequest.setExpirationDate(LocalDate.now().plusYears(2));

    ResponseEntity<PaymentCardResponseDTO> createResponse =
        restTemplate.postForEntity(
            "/api/payment-cards/user/" + userId, createRequest, PaymentCardResponseDTO.class);
    assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
    Long cardId = createResponse.getBody().getId();

    // Act
    ResponseEntity<PaymentCardResponseDTO> response =
        restTemplate.getForEntity("/api/payment-cards/" + cardId, PaymentCardResponseDTO.class);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(cardId, response.getBody().getId());
    assertEquals("5111111111111111", response.getBody().getNumber());
  }

  @Test
  void activateCard_ShouldActivateCard() {
    // Arrange
    PaymentCardRequestDTO createRequest = new PaymentCardRequestDTO();
    createRequest.setNumber("6111111111111111");
    createRequest.setHolder("ACTIVATE TEST");
    createRequest.setExpirationDate(LocalDate.now().plusYears(2));
    createRequest.setActive(false);
    ResponseEntity<PaymentCardResponseDTO> createResponse =
        restTemplate.postForEntity(
            "/api/payment-cards/user/" + userId, createRequest, PaymentCardResponseDTO.class);
    assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
    Long cardId = createResponse.getBody().getId();

    // Act
    ResponseEntity<Void> response =
        restTemplate.exchange(
            "/api/payment-cards/" + cardId + "/activate", HttpMethod.PATCH, null, Void.class);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());

    ResponseEntity<PaymentCardResponseDTO> getResponse =
        restTemplate.getForEntity("/api/payment-cards/" + cardId, PaymentCardResponseDTO.class);
    assertEquals(HttpStatus.OK, getResponse.getStatusCode());
    assertTrue(getResponse.getBody().getActive());
  }

  @Test
  void getCardsByUserId_ShouldReturnUserCards() {
    // Arrange
    for (int i = 0; i < 3; i++) {
      PaymentCardRequestDTO requestDTO = new PaymentCardRequestDTO();
      requestDTO.setNumber("711111111111111" + i);
      requestDTO.setHolder("USER CARD " + i);
      requestDTO.setExpirationDate(LocalDate.now().plusYears(2));

      ResponseEntity<PaymentCardResponseDTO> cardResponse =
          restTemplate.postForEntity(
              "/api/payment-cards/user/" + userId, requestDTO, PaymentCardResponseDTO.class);
      assertEquals(HttpStatus.CREATED, cardResponse.getStatusCode());
    }

    // Act
    ResponseEntity<String> response =
        restTemplate.getForEntity(
            "/api/payment-cards/user/" + userId + "?page=0&size=10", String.class);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  @Test
  void deactivateCard_ShouldDeactivateCard() {
    // Arrange
    PaymentCardRequestDTO createRequest = new PaymentCardRequestDTO();
    createRequest.setNumber("8111111111111111");
    createRequest.setHolder("DEACTIVATE TEST");
    createRequest.setExpirationDate(LocalDate.now().plusYears(2));
    createRequest.setActive(true);

    ResponseEntity<PaymentCardResponseDTO> createResponse =
        restTemplate.postForEntity(
            "/api/payment-cards/user/" + userId, createRequest, PaymentCardResponseDTO.class);
    assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
    Long cardId = createResponse.getBody().getId();

    // Act
    ResponseEntity<Void> response =
        restTemplate.exchange(
            "/api/payment-cards/" + cardId + "/deactivate", HttpMethod.PATCH, null, Void.class);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());

    ResponseEntity<PaymentCardResponseDTO> getResponse =
        restTemplate.getForEntity("/api/payment-cards/" + cardId, PaymentCardResponseDTO.class);
    assertEquals(HttpStatus.OK, getResponse.getStatusCode());
    assertFalse(getResponse.getBody().getActive());
  }

  @Test
  void deleteCard_ShouldDeleteCard() {
    // Arrange
    PaymentCardRequestDTO createRequest = new PaymentCardRequestDTO();
    createRequest.setNumber("9111111111111111");
    createRequest.setHolder("DELETE TEST");
    createRequest.setExpirationDate(LocalDate.now().plusYears(2));

    ResponseEntity<PaymentCardResponseDTO> createResponse =
        restTemplate.postForEntity(
            "/api/payment-cards/user/" + userId, createRequest, PaymentCardResponseDTO.class);
    assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
    Long cardId = createResponse.getBody().getId();

    // Act
    ResponseEntity<Void> response =
        restTemplate.exchange("/api/payment-cards/" + cardId, HttpMethod.DELETE, null, Void.class);

    // Assert
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    ResponseEntity<String> getResponse =
        restTemplate.getForEntity("/api/payment-cards/" + cardId, String.class);
    assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
  }

  @Test
  void updateCard_ShouldReturnUpdatedCard() {
    // Arrange
    PaymentCardRequestDTO createRequest = new PaymentCardRequestDTO();
    createRequest.setNumber("1011111111111111");
    createRequest.setHolder("ORIGINAL HOLDER");
    createRequest.setExpirationDate(LocalDate.now().plusYears(2));

    ResponseEntity<PaymentCardResponseDTO> createResponse =
        restTemplate.postForEntity(
            "/api/payment-cards/user/" + userId, createRequest, PaymentCardResponseDTO.class);
    assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
    Long cardId = createResponse.getBody().getId();

    PaymentCardRequestDTO updateRequest = new PaymentCardRequestDTO();
    updateRequest.setNumber("1011111111111111");
    updateRequest.setHolder("UPDATED HOLDER");
    updateRequest.setExpirationDate(LocalDate.now().plusYears(3));

    // Act
    ResponseEntity<PaymentCardResponseDTO> response =
        restTemplate.exchange(
            "/api/payment-cards/" + cardId,
            HttpMethod.PUT,
            new HttpEntity<>(updateRequest),
            PaymentCardResponseDTO.class);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("UPDATED HOLDER", response.getBody().getHolder());
  }

  @Test
  void getCardByNumber_ShouldReturnCard() {
    // Arrange
    String cardNumber = "1211111111111111";
    PaymentCardRequestDTO createRequest = new PaymentCardRequestDTO();
    createRequest.setNumber(cardNumber);
    createRequest.setHolder("NUMBER TEST");
    createRequest.setExpirationDate(LocalDate.now().plusYears(2));

    ResponseEntity<PaymentCardResponseDTO> createResponse =
        restTemplate.postForEntity(
            "/api/payment-cards/user/" + userId, createRequest, PaymentCardResponseDTO.class);
    assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

    // Act
    ResponseEntity<PaymentCardResponseDTO> response =
        restTemplate.getForEntity(
            "/api/payment-cards/number/" + cardNumber, PaymentCardResponseDTO.class);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(cardNumber, response.getBody().getNumber());
  }

  @Test
  void getCardByUserAndId_ShouldReturnCard() {
    // Arrange
    PaymentCardRequestDTO createRequest = new PaymentCardRequestDTO();
    createRequest.setNumber("1311111111111111");
    createRequest.setHolder("USER CARD TEST");
    createRequest.setExpirationDate(LocalDate.now().plusYears(2));

    ResponseEntity<PaymentCardResponseDTO> createResponse =
        restTemplate.postForEntity(
            "/api/payment-cards/user/" + userId, createRequest, PaymentCardResponseDTO.class);
    assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
    Long cardId = createResponse.getBody().getId();

    // Act
    ResponseEntity<PaymentCardResponseDTO> response =
        restTemplate.getForEntity(
            "/api/payment-cards/user/" + userId + "/card/" + cardId, PaymentCardResponseDTO.class);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(cardId, response.getBody().getId());
  }

  @Test
  void createCard_ShouldReturnBadRequest_WhenExpirationDateInPast() {
    // Arrange
    PaymentCardRequestDTO requestDTO = new PaymentCardRequestDTO();
    requestDTO.setNumber("1411111111111111");
    requestDTO.setHolder("EXPIRED CARD");
    requestDTO.setExpirationDate(LocalDate.now().minusDays(1));

    // Act
    ResponseEntity<String> response =
        restTemplate.postForEntity("/api/payment-cards/user/" + userId, requestDTO, String.class);

    // Assert
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void createCard_ShouldReturnBadRequest_WhenCardNumberInvalid() {
    // Arrange
    PaymentCardRequestDTO requestDTO = new PaymentCardRequestDTO();
    requestDTO.setNumber("123"); // Too short
    requestDTO.setHolder("INVALID CARD");
    requestDTO.setExpirationDate(LocalDate.now().plusYears(2));

    // Act
    ResponseEntity<String> response =
        restTemplate.postForEntity("/api/payment-cards/user/" + userId, requestDTO, String.class);

    // Assert
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void getActiveCardsByUserId_ShouldReturnOnlyActiveCards() {
    // Arrange
    PaymentCardRequestDTO activeCard = new PaymentCardRequestDTO();
    activeCard.setNumber("1511111111111111");
    activeCard.setHolder("ACTIVE CARD");
    activeCard.setExpirationDate(LocalDate.now().plusYears(2));
    activeCard.setActive(true);

    PaymentCardRequestDTO inactiveCard = new PaymentCardRequestDTO();
    inactiveCard.setNumber("1611111111111111");
    inactiveCard.setHolder("INACTIVE CARD");
    inactiveCard.setExpirationDate(LocalDate.now().plusYears(2));
    inactiveCard.setActive(false);

    restTemplate.postForEntity(
        "/api/payment-cards/user/" + userId, activeCard, PaymentCardResponseDTO.class);
    restTemplate.postForEntity(
        "/api/payment-cards/user/" + userId, inactiveCard, PaymentCardResponseDTO.class);

    // Act
    ResponseEntity<String> response =
        restTemplate.getForEntity(
            "/api/payment-cards/user/" + userId + "/active?page=0&size=10", String.class);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }
}
