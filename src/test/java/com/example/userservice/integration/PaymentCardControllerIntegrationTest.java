package com.example.userservice.integration;

import com.example.userservice.dto.PaymentCardRequestDTO;
import com.example.userservice.dto.PaymentCardResponseDTO;
import com.example.userservice.dto.UserRequestDTO;
import com.example.userservice.dto.UserResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentCardControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private Long userId;
    private String uniqueEmail;

    @BeforeEach
    void setUp() {
        // Генерируем уникальный email для каждого теста
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        uniqueEmail = "card.owner." + uniqueId + "@example.com";

        // Создаем пользователя для тестов карт
        UserRequestDTO userRequest = new UserRequestDTO();
        userRequest.setName("Card");
        userRequest.setSurname("Owner");
        userRequest.setEmail(uniqueEmail);

        ResponseEntity<UserResponseDTO> userResponse = restTemplate.postForEntity(
                "/api/users", userRequest, UserResponseDTO.class);

        // Проверяем успешное создание пользователя
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
        ResponseEntity<PaymentCardResponseDTO> response = restTemplate.postForEntity(
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
        // Arrange - создаем 5 карт с уникальными номерами
        for (int i = 0; i < 5; i++) {
            PaymentCardRequestDTO requestDTO = new PaymentCardRequestDTO();
            requestDTO.setNumber(generateUniqueCardNumber(i)); // Уникальный номер для каждой карты
            requestDTO.setHolder("CARD HOLDER " + i);
            requestDTO.setExpirationDate(LocalDate.now().plusYears(2));

            ResponseEntity<PaymentCardResponseDTO> cardResponse = restTemplate.postForEntity(
                    "/api/payment-cards/user/" + userId, requestDTO, PaymentCardResponseDTO.class);
            assertEquals(HttpStatus.CREATED, cardResponse.getStatusCode());
        }

        // Пытаемся создать 6-ю карту
        PaymentCardRequestDTO sixthCard = new PaymentCardRequestDTO();
        sixthCard.setNumber(generateUniqueCardNumber(6)); // Уникальный номер
        sixthCard.setHolder("SIXTH CARD");
        sixthCard.setExpirationDate(LocalDate.now().plusYears(2));

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/payment-cards/user/" + userId, sixthCard, String.class);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    private String generateUniqueCardNumber(int index) {
        // Генерируем уникальный номер карты для каждого индекса
        String baseNumber = "411111111111";
        String suffix = String.format("%04d", index);
        return baseNumber + suffix;
    }

    @Test
    void getCardById_ShouldReturnCard_WhenCardExists() {
        // Arrange - создаем карту
        PaymentCardRequestDTO createRequest = new PaymentCardRequestDTO();
        createRequest.setNumber("5111111111111111");
        createRequest.setHolder("TEST CARD");
        createRequest.setExpirationDate(LocalDate.now().plusYears(2));

        ResponseEntity<PaymentCardResponseDTO> createResponse = restTemplate.postForEntity(
                "/api/payment-cards/user/" + userId, createRequest, PaymentCardResponseDTO.class);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        Long cardId = createResponse.getBody().getId();

        // Act
        ResponseEntity<PaymentCardResponseDTO> response = restTemplate.getForEntity(
                "/api/payment-cards/" + cardId, PaymentCardResponseDTO.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(cardId, response.getBody().getId());
        assertEquals("5111111111111111", response.getBody().getNumber());
    }

    @Test
    void activateCard_ShouldActivateCard() {
        // Arrange - создаем карту
        PaymentCardRequestDTO createRequest = new PaymentCardRequestDTO();
        createRequest.setNumber("6111111111111111");
        createRequest.setHolder("ACTIVATE TEST");
        createRequest.setExpirationDate(LocalDate.now().plusYears(2));
        createRequest.setActive(false); // создаем неактивную карту

        ResponseEntity<PaymentCardResponseDTO> createResponse = restTemplate.postForEntity(
                "/api/payment-cards/user/" + userId, createRequest, PaymentCardResponseDTO.class);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        Long cardId = createResponse.getBody().getId();

        // Act
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/payment-cards/" + cardId + "/activate",
                HttpMethod.PATCH,
                null,
                Void.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Проверяем, что карта стала активной
        ResponseEntity<PaymentCardResponseDTO> getResponse = restTemplate.getForEntity(
                "/api/payment-cards/" + cardId, PaymentCardResponseDTO.class);
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertTrue(getResponse.getBody().getActive());
    }

    @Test
    void getCardsByUserId_ShouldReturnUserCards() {
        // Arrange - создаем несколько карт для пользователя
        for (int i = 0; i < 3; i++) {
            PaymentCardRequestDTO requestDTO = new PaymentCardRequestDTO();
            requestDTO.setNumber("711111111111111" + i);
            requestDTO.setHolder("USER CARD " + i);
            requestDTO.setExpirationDate(LocalDate.now().plusYears(2));

            ResponseEntity<PaymentCardResponseDTO> cardResponse = restTemplate.postForEntity(
                    "/api/payment-cards/user/" + userId, requestDTO, PaymentCardResponseDTO.class);
            assertEquals(HttpStatus.CREATED, cardResponse.getStatusCode());
        }

        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/payment-cards/user/" + userId + "?page=0&size=10", String.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}